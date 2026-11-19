(ns engine.state
  (:require [engine.config :as config]
            [engine.grid :as grid]
            [engine.rules :as rules]))

(defn estado-inicial
  "Crea un estado vacío con una grilla de ancho x alto y paso 0. Sin argumentos
  usa config/ancho y config/alto."
  ([] (estado-inicial config/ancho config/alto))
  ([ancho alto]
   {:grilla (grid/crear-grilla-vacia ancho alto)
    :paso 0
    :ancho ancho
    :alto alto}))

(defn desplazamiento
  "Devuelve el desplazamiento (ox, oy) de la partición de bloques para el paso
  indicado. El ciclo se repite cada 4 pasos."
  [paso]
  (case (int (mod paso 4))
    0 [0 0]
    1 [1 1]
    2 [1 0]
    3 [0 1]))

(defn leer-bloque
  "Lee los cuatro materiales del bloque (bx, by) usando el desplazamiento
  (ox, oy). Devuelve [posición0 posición1 posición2 posición3]."
  [grilla ox oy bx by]
  (let [x (+ ox (* 2 bx))
        y (+ oy (* 2 by))]
    [(grid/obtener-material grilla x y)
     (grid/obtener-material grilla (inc x) y)
     (grid/obtener-material grilla x (inc y))
     (grid/obtener-material grilla (inc x) (inc y))]))

(defn escribir-bloque
  "Escribe los cuatro materiales del bloque (bx, by) en la grilla usando el
  desplazamiento (ox, oy). Devuelve la grilla resultante."
  [bloque ox oy bx by grilla]
  (let [pos0 (get bloque 0)
        pos1 (get bloque 1)
        pos2 (get bloque 2)
        pos3 (get bloque 3)
        x (+ ox (* 2 bx))
        y (+ oy (* 2 by))]
    (-> grilla
        (grid/pintar-celda x y pos0)
        (grid/pintar-celda (+ x 1) y pos1)
        (grid/pintar-celda x (+ y 1) pos2)
        (grid/pintar-celda (+ x 1) (+ y 1) pos3))))

(defn fase-bloque
  "Calcula la fase de un bloque a partir del paso y de sus índices (bx, by)."
  [paso bx by]
  (+ (quot paso 4) (* 3 bx) (* 5 by)))

(defn indices-bloques
  "Devuelve los índices [bx by] de todos los bloques completos que entran en la
  grilla con el desplazamiento (ox, oy)."
  [ancho alto ox oy]
  (let [cant-bx (quot (- ancho ox) 2)
        cant-by (quot (- alto oy) 2)]
    (for [bx (range cant-bx)
          by (range cant-by)]
      [bx by])))

(defn- ancho-de
  "Devuelve el ancho del estado, usando :ancho o el largo de la primera fila."
  [{:keys [grilla ancho]}]
  (or ancho (count (first grilla))))

(defn- alto-de
  "Devuelve el alto del estado, usando :alto o la cantidad de filas."
  [{:keys [grilla alto]}]
  (or alto (count grilla)))

(defn- bloque-en-grilla
  "Procesa un bloque con las reglas normales y lo escribe en la grilla acumulada."
  [g grilla paso ox oy bx by]
  (let [bloque (leer-bloque grilla ox oy bx by)
        fase (fase-bloque paso bx by)]
    (escribir-bloque (rules/procesar-bloque bloque fase) ox oy bx by g)))

(defn aplicar-paso-con
  "Variante parametrizable de `aplicar-paso`: procesa los bloques en el `orden`
  dado, aplicando `procesar-bloque-en-grilla` a cada uno.

  `procesar-bloque-en-grilla` recibe
  [grilla-acumulada grilla-original paso ox oy bx by] y devuelve la acumulada.

  La aritiedad de 2 usa el procesador estándar (`bloque-en-grilla`), así que
  sirve como referencia secuencial sin tocar funciones privadas.

  Existe para poder verificar la independencia de orden del paso (ver
  `src/test/engine/independencia_orden.clj`) y para comparar contra
  `aplicar-paso-paralelo` (ver `src/test/engine/paso_paralelo.clj`): el contrato
  de la partición es que cada bloque LEE solo las celdas que ESCRIBE, así que
  cualquier permutación del orden tiene que producir la misma grilla."
  ([estado orden]
   (aplicar-paso-con estado orden bloque-en-grilla))
  ([estado orden procesar-bloque-en-grilla]
   (let [{:keys [grilla paso]} estado
         [ox oy] (desplazamiento paso)]
     (assoc estado
            :grilla (reduce
                     (fn [g [bx by]]
                       (procesar-bloque-en-grilla g grilla paso ox oy bx by))
                     grilla
                     orden)
            :paso (inc paso)))))

(def ^:private bloques-minimos-paralelo
  "Cantidad mínima de bloques completos para repartir el paso entre varios
  hilos. Por debajo de este umbral el costo de coordinar los futuros supera al
  trabajo útil, así que se usa la vía secuencial."
  64)

(defn- hilos-de-trabajo
  "Cantidad de hilos a usar en un paso paralelo: todos los procesadores menos
  uno, dejando libre el del hilo principal."
  []
  (max 1 (dec (.availableProcessors (Runtime/getRuntime)))))

(defn- chunks-por-fila-de-bloque
  "Parte `orden` en a lo sumo `cant-hilos` chunks, cada uno formado por filas de
  bloque (`by`) distintas de las de los otros chunks.

  Agrupar por `by` es lo que hace disjuntas a las escrituras: el bloque (bx, by)
  escribe solo las filas oy+2by y oy+2by+1 y las columnas ox+2bx y ox+2bx+1.
  Devuelve una secuencia de pares [by [[bx by] ...]] ordenados por `by`, para
  que el reparto sea determinístico (los chunks dependen solo de `orden`)."
  [orden cant-hilos]
  (let [por-fila (sort-by key (group-by second orden))
        tamano (max 1 (long (Math/ceil (/ (count por-fila) (double cant-hilos)))))]
    (partition-all tamano por-fila)))

(defn- procesar-chunk
  "Procesa los bloques de un chunk y devuelve {fila vector-fila} con las filas
  que ese chunk posee.

  Todas las lecturas van a `grilla-original`: un bloque nunca ve el resultado
  parcial de otro. Cada fila se arma con un transient propio (uno por fila y por
  chunk, porque los transients son de uso exclusivo de un hilo) copiado de la
  fila original, y se vuelve a persistente antes de devolverlo."
  [grilla-original paso ox oy chunk]
  (reduce
   (fn [filas [by bloques]]
     (let [y (+ oy (* 2 by))
           fila-arriba (transient (vec (nth grilla-original y)))
           fila-abajo (transient (vec (nth grilla-original (inc y))))
           [ta tb] (reduce
                    (fn [[ta tb] [bx _]]
                      (let [x (+ ox (* 2 bx))
                            bloque (rules/procesar-bloque
                                    (leer-bloque grilla-original ox oy bx by)
                                    (fase-bloque paso bx by))]
                        [(-> ta
                             (assoc! x (nth bloque 0))
                             (assoc! (inc x) (nth bloque 1)))
                         (-> tb
                             (assoc! x (nth bloque 2))
                             (assoc! (inc x) (nth bloque 3)))]))
                    [fila-arriba fila-abajo]
                    bloques)]
       (assoc filas y (persistent! ta) (inc y) (persistent! tb))))
   {}
   chunk))

(defn aplicar-paso-paralelo
  "Avanza la simulación un paso repartiendo los bloques entre varios hilos.

  El resultado es indistinguible de `aplicar-paso-con` secuencial: la partición
  por `by` hace que cada bloque lea y escriba celdas que ningún otro toca, y el
  armado final es por índice de fila (no por orden de terminación), así que la
  grilla devuelta es siempre la misma.

  Las filas que ningún bloque cubre (con desplazamiento [1 1]: la primera y la
  última, además de la primera y la última columna) se copian intactas de la
  grilla original."
  [estado]
  (let [{:keys [grilla paso]} estado
        [ox oy] (desplazamiento paso)
        orden (vec (indices-bloques (ancho-de estado) (alto-de estado) ox oy))
        cant-hilos (hilos-de-trabajo)]
    (if (or (< cant-hilos 2)
            (< (count orden) bloques-minimos-paralelo))
      (aplicar-paso-con estado orden)
      (let [chunks (chunks-por-fila-de-bloque orden cant-hilos)
            filas-nuevas (apply merge
                                (map deref
                                     (mapv (fn [chunk]
                                             (future (procesar-chunk grilla paso
                                                                     ox oy chunk)))
                                           chunks)))]
        (assoc estado
               :grilla (vec (map-indexed
                             (fn [y fila]
                               (if-let [nueva (get filas-nuevas y)]
                                 nueva
                                 fila))
                             grilla))
               :paso (inc paso))))))

(defn aplicar-paso
  "Avanza la simulación un paso. Todos los bloques leen la grilla del paso
  anterior y escriben sobre una grilla nueva, de modo que el resultado no
  depende del orden de procesamiento. Devuelve el estado con el paso incrementado."
  [{:keys [paso] :as estado}]
  (let [[ox oy] (desplazamiento paso)]
    (aplicar-paso-con estado
                      (indices-bloques (ancho-de estado) (alto-de estado) ox oy)
                      bloque-en-grilla)))

(defn contar-particulas
  "Cuenta las celdas ocupadas por un material distinto de :aire."
  [{:keys [grilla]}]
  (count (remove #{:aire} (mapcat identity grilla))))

(defn limpiar
  "Devuelve un estado con la grilla vacía, conservando el número de paso y las
  dimensiones."
  [estado]
  (assoc estado
         :grilla (grid/crear-grilla-vacia (ancho-de estado) (alto-de estado))))

(defn pintar-disco
  "Pinta un disco de radio radio centrado en (cx, cy) con el material indicado.
  Solo modifica celdas dentro de la grilla. Devuelve el estado resultante."
  [{:keys [grilla] :as estado} cx cy radio material]
  (let [radio (max 0 radio)
        r2 (* radio radio)
        ancho (ancho-de estado)
        alto (alto-de estado)
        x0 (max 0 (- cx radio))
        x1 (min (dec ancho) (+ cx radio))
        y0 (max 0 (- cy radio))
        y1 (min (dec alto) (+ cy radio))
        nueva (reduce
               (fn [g y]
                 (reduce
                  (fn [g x]
                    (let [dx (- x cx)
                          dy (- y cy)]
                      (if (<= (+ (* dx dx) (* dy dy)) r2)
                        (grid/pintar-celda g x y material)
                        g)))
                  g
                  (range x0 (inc x1))))
               grilla
               (range y0 (inc y1)))]
    (assoc estado :grilla nueva)))

(defn pintar-segmento
  "Pinta un segmento entre (x0, y0) y (x1, y1) usando un disco de radio radio
  en cada punto. Devuelve el estado resultante."
  [estado x0 y0 x1 y1 radio material]
  (let [dx (- x1 x0)
        dy (- y1 y0)
        pasos (max (Math/abs (long dx)) (Math/abs (long dy)))]
    (if (zero? pasos)
      (pintar-disco estado x1 y1 radio material)
      (reduce
       (fn [est t]
         (let [x (long (Math/round (double (+ x0 (/ (* dx t) pasos)))))
               y (long (Math/round (double (+ y0 (/ (* dy t) pasos)))))]
           (pintar-disco est x y radio material)))
       estado
       (range (inc pasos))))))
