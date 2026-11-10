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

(defn aplicar-paso
  "Avanza la simulación un paso. Todos los bloques leen la grilla del paso
  anterior y escriben sobre una grilla nueva, de modo que el resultado no
  depende del orden de procesamiento. Devuelve el estado con el paso incrementado."
  [{:keys [grilla paso] :as estado}]
  (let [[ox oy] (desplazamiento paso)
        ancho (ancho-de estado)
        alto (alto-de estado)
        nueva (reduce
               (fn [g [bx by]]
                 (let [bloque (leer-bloque grilla ox oy bx by)
                       fase (fase-bloque paso bx by)
                       res (rules/procesar-bloque bloque fase)]
                   (escribir-bloque res ox oy bx by g)))
               grilla
               (indices-bloques ancho alto ox oy))]
    (assoc estado :grilla nueva :paso (inc paso))))

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
