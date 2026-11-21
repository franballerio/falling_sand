(ns engine.paso-paralelo-propiedades
  "Propiedades del paso paralelo derivadas del pedido, independientes de
  `aplicar-paso-paralelo` (la referencia se arma con la API pública del motor).

  Cubre: partición disjunta por fila de bloque, determinismo, entrada intacta,
  filas/columnas sin bloques copiadas, umbral y fallback secuencial, y que el
  camino paralelo realmente se ejercite en los tests."
  (:require [clojure.test :refer [deftest testing is]]
            [engine.rules :as rules]
            [engine.state :as state]))

(def ^:private materiales
  [:aire :arena :agua :lava :vapor :fuego :madera :pared :obsidiana])

(defn- grilla-variada [ancho alto]
  (vec (for [y (range alto)]
         (vec (for [x (range ancho)]
                (nth materiales (mod (+ (* 7 x) (* 13 y)) (count materiales))))))))

(defn- estado-de-prueba [ancho alto paso]
  (assoc (state/estado-inicial ancho alto)
         :grilla (grilla-variada ancho alto)
         :paso paso))

(defn- referencia
  "Paso secuencial armado solo con API pública, sin pasar por el paralelo."
  [estado]
  (let [{:keys [grilla paso ancho alto]} estado
        [ox oy] (state/desplazamiento paso)
        orden (state/indices-bloques ancho alto ox oy)]
    (assoc estado
           :paso (inc paso)
           :grilla (reduce (fn [g [bx by]]
                             (state/escribir-bloque
                              (rules/procesar-bloque
                               (state/leer-bloque grilla ox oy bx by)
                               (state/fase-bloque paso bx by))
                              ox oy bx by g))
                           grilla orden))))

(def ^:private hilos-var (ns-resolve 'engine.state 'hilos-de-trabajo))
(def ^:private chunk-var (ns-resolve 'engine.state 'procesar-chunk))
(def ^:private chunks-var (ns-resolve 'engine.state 'chunks-por-fila-de-bloque))

(defn- con-var-redefinida [v f cuerpo]
  (let [original @v]
    (try
      (alter-var-root v (constantly f))
      (cuerpo)
      (finally (alter-var-root v (constantly original))))))

(deftest equivalente-a-la-referencia-secuencial
  (doseq [[ancho alto] [[12 10] [33 21] [34 22] [160 120] [161 121]
                        [65 40] [200 3] [3 200]]
          paso (range 4)]
    (testing (str ancho "x" alto ", paso " paso)
      (is (= (referencia (estado-de-prueba ancho alto paso))
             (state/aplicar-paso-paralelo (estado-de-prueba ancho alto paso)))))))

(deftest particion-por-fila-de-bloque-es-disjunta
  (testing "cada bloque en un chunk, filas de grilla sin solapamiento, dentro de rango"
    (doseq [[ancho alto] [[160 120] [33 21] [161 121] [3 200]]
            paso (range 4)
            cant-hilos [1 2 3 11 47]]
      (let [[ox oy] (state/desplazamiento paso)
            orden (vec (state/indices-bloques ancho alto ox oy))]
        (when (seq orden)
          (let [chunks (chunks-var orden cant-hilos)
                bys (vec (mapcat (fn [c] (map first c)) chunks))
                filas (vec (mapcat (fn [c]
                                     (mapcat (fn [[by _]] [(+ oy (* 2 by))
                                                          (inc (+ oy (* 2 by)))])
                                             c))
                                   chunks))]
            (is (= (set (map second orden)) (set bys)) "todos los bloques repartidos")
            (is (= (count bys) (count (set bys))) "ningún by en dos chunks")
            (is (= (count filas) (count (set filas))) "filas escritas disjuntas")
            (is (every? #(<= 0 % (dec alto)) filas) "filas dentro de la grilla")
            (is (<= (count chunks) cant-hilos) "no más chunks que hilos")
            (doseq [c chunks]
              (let [b (map first c)]
                (is (= (vec b) (vec (sort (distinct b))))
                    "cada chunk toma filas de bloque contiguas y sin repetir")))))))))

(deftest determinista-y-sin-mutar-la-entrada
  (doseq [paso (range 4)]
    (let [estado (estado-de-prueba 160 120 paso)
          antes (:grilla estado)
          resultados (vec (repeatedly 5 #(state/aplicar-paso-paralelo estado)))]
      (is (= antes (:grilla estado)) (str "entrada intacta, paso " paso))
      (is (= 1 (count (distinct resultados))) (str "5 corridas iguales, paso " paso)))))

(deftest filas-y-columnas-sin-bloques-se-copian-de-la-original
  (doseq [[ancho alto] [[32 24] [33 21] [160 120]]
          paso (range 4)]
    (let [estado (estado-de-prueba ancho alto paso)
          [ox oy] (state/desplazamiento paso)
          orden (vec (state/indices-bloques ancho alto ox oy))
          filas-cub (set (mapcat (fn [[_ by]] [(+ oy (* 2 by))
                                               (inc (+ oy (* 2 by)))])
                                orden))
          cols-cub (set (mapcat (fn [[bx _]] [(+ ox (* 2 bx))
                                              (inc (+ ox (* 2 bx)))])
                                orden))
          antes (:grilla estado)
          despues (:grilla (state/aplicar-paso-paralelo estado))
          fuera (for [y (range alto), x (range ancho)
                      :when (or (not (filas-cub y)) (not (cols-cub x)))]
                  [y x])
          alteradas (remove (fn [[y x]] (= (get-in antes [y x]) (get-in despues [y x])))
                            fuera)]
      (when (pos? oy)
        (is (seq fuera) "con oy>0 la fila 0 queda fuera de todo bloque"))
      (is (= alto (count despues)) "ninguna fila ausente")
      (is (every? #(and (vector? %) (= ancho (count %)) (not-any? nil? %)) despues)
          "ninguna fila corta ni con celdas nil")
      (is (empty? alteradas)
          (str "celdas fuera de bloque alteradas: " (vec (take 5 alteradas)))))))

(deftest con-un-solo-hilo-usa-la-via-secuencial
  (con-var-redefinida hilos-var (fn [] 1)
                      (fn []
                        (doseq [paso (range 4)]
                          (is (= (referencia (estado-de-prueba 160 120 paso))
                                 (state/aplicar-paso-paralelo (estado-de-prueba 160 120 paso)))))
                        (is (= 1 (hilos-var))))))

(deftest bajo-el-umbral-cae-a-la-via-secuencial
  (con-var-redefinida chunk-var
                      (fn [& _] (throw (ex-info "procesar-chunk no debería llamarse" {})))
                      (fn []
                        (doseq [paso (range 4)]
                          (is (= (referencia (estado-de-prueba 12 10 paso))
                                 (state/aplicar-paso-paralelo (estado-de-prueba 12 10 paso))))))))

(deftest equivalente-con-hilos-forzados-a-4
  (testing "el camino paralelo se ejercita aunque la máquina tenga un solo núcleo"
    (con-var-redefinida hilos-var (fn [] 4)
                        (fn []
                          (doseq [[ancho alto] [[33 21] [160 120] [161 121]]
                                  paso (range 4)]
                            (is (= (referencia (estado-de-prueba ancho alto paso))
                                   (state/aplicar-paso-paralelo
                                    (estado-de-prueba ancho alto paso)))
                                (str ancho "x" alto ", paso " paso ", 4 hilos")))))))

(deftest el-camino-paralelo-se-ejercita
  (when (>= (hilos-var) 2)
    (let [llamados (atom 0)]
      (con-var-redefinida chunk-var
                          (fn [& _] (swap! llamados inc) {})
                          (fn []
                            (state/aplicar-paso-paralelo (estado-de-prueba 160 120 0))
                            (is (= (count (chunks-var
                                           (vec (state/indices-bloques 160 120 0 0))
                                           (hilos-var)))
                                   @llamados)
                                "160x120 tiene que pasar por procesar-chunk"))))))
