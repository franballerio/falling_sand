(ns engine.state
  (:require [engine.config :as config]
            [engine.grid :as grid]
            [engine.rules :as rules]))


(defn estado-inicial
  ([] (estado-inicial config/filas config/columnas))
  ([ancho alto]
   {:grilla (grid/crear-grilla-vacia ancho alto)
    :paso 0
    :ancho ancho
    :alto alto}))


(defn desplazamiento [paso]
  "define como se desplaza la grilla segun el numero de paso"
  (case (int (mod paso 4))
    0 [0 0]
    1 [1 1]
    2 [1 0]
    3 [0 1]))


(defn leer-bloque [grilla ox oy bx by]
  (let [x (+ ox (* 2 bx))
        y (+ oy (* 2 by))]
    [(grid/obtener-material grilla x y)
     (grid/obtener-material grilla (inc x) y)
     (grid/obtener-material grilla x (inc y))
     (grid/obtener-material grilla (inc x) (inc y))]))


(defn escribir-bloque [bloque ox oy bx by grilla]
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


(defn fase-bloque [paso bx by]
  "ecuacion dada por la catedra para fase"
  (+ (quot paso 4) (* 3 bx) (* 5 by)))


(defn indices-bloques [ancho alto ox oy]
  "devuelve todos los indices de bloques que no exceden la matriz teniendo en cuenta el desplazamiento (por el paso)"
  (let [cant-bx (quot (- ancho ox) 2)
        cant-by (quot (- alto oy) 2)]
    (for [bx (range cant-bx)
          by (range cant-by)]
      [bx by])))


(defn- ancho-de [{:keys [grilla ancho]}]
  (or ancho (count (first grilla))))


(defn- alto-de [{:keys [grilla alto]}]
  (or alto (count grilla)))


(defn aplicar-paso [{:keys [grilla paso] :as estado}]
  "aplica un paso en toda la grilla
  actualiza toda la grilla para el mismo instante segun el paso"
  (let [[ox oy] (desplazamiento paso)
        ancho (ancho-de estado)
        alto (alto-de estado)
        nueva (reduce                                       ;;va modificando y acumulando en g por cada combinacion de coordenadas de bloque
                (fn [g [bx by]]
                  (let [bloque (leer-bloque grilla ox oy bx by) ;; siempre la misma grilla para la ilusion de que todos se mueven al mismo tiempo
                        fase (fase-bloque paso bx by)
                        res (rules/procesar-bloque bloque fase)]
                    (escribir-bloque res ox oy bx by g)))
                grilla
                (indices-bloques ancho alto ox oy))]        ;;a reduce le pasamos la funcion anonima,  la grilla como estado inicial y todos los bloques que debe modificar
    (assoc estado :grilla nueva :paso (inc paso))))


(defn contar-particulas [{:keys [grilla]}]
  "cuenta la cantidad de particulas de la grilla (diferentes a aire)"
  (count (remove #{:aire} (mapcat identity grilla))))


(defn limpiar [estado]
  "limpia toda la grilla y asigna el paso 0"
  (assoc estado
         :grilla (grid/crear-grilla-vacia (ancho-de estado) (alto-de estado))
         :paso 0))


(defn pintar-disco [{:keys [grilla] :as estado} cx cy radio material]
  "asigna un material al circulo "
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


(defn pintar-segmento [estado x0 y0 x1 y1 radio material]
  "asigna a un segmento determinado material"
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
