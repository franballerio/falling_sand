(ns engine.independencia-orden
  "Verifica la propiedad que habilita paralelizar `aplicar-paso`: el orden en que
  se procesan los bloques de un paso no altera el resultado.

  El invariante que se testea (de forma observable) es: cada bloque LEE solo las
  celdas que ESCRIBE, y los bloques son disjuntos. Si alguien agrega una regla
  que mire fuera de su bloque, este test falla."
  (:require [clojure.test :refer [deftest testing is]]
            [engine.grid :as grid]
            [engine.rules :as rules]
            [engine.state :as state]))

(def ^:private ancho-de-prueba 12)
(def ^:private alto-de-prueba 10)

(defn- grilla-variada
  "Grilla determinística con variedad de materiales, para que las reglas tengan
  algo que hacer en cada fase."
  [ancho alto]
  (let [materiales [:aire :arena :agua :lava :vapor :fuego :madera :pared]]
    (vec (for [y (range alto)]
           (vec (for [x (range ancho)]
                  (nth materiales
                       (mod (+ (* 7 x) (* 13 y)) (count materiales)))))))))

(defn- estado-de-prueba
  []
  (assoc (state/estado-inicial ancho-de-prueba alto-de-prueba)
         :grilla (grilla-variada ancho-de-prueba alto-de-prueba)))

(defn- orden-de-bloques
  [paso]
  (let [[ox oy] (state/desplazamiento paso)]
    (state/indices-bloques ancho-de-prueba alto-de-prueba ox oy)))

(defn- procesar-estandar
  "Reimplementación independiente del procesamiento de un bloque, usando solo la
  API pública del motor. Sirve de contraprueba de `aplicar-paso`."
  [g grilla paso ox oy bx by]
  (let [bloque (state/leer-bloque grilla ox oy bx by)
        fase (state/fase-bloque paso bx by)]
    (state/escribir-bloque (rules/procesar-bloque bloque fase) ox oy bx by g)))

(defn- procesar-trampa
  "Regla DELIBERADAMENTE rota: además de procesar su bloque, pinta la celda (0 0).
  Escribe FUERA de su bloque, así que el resultado pasa a depender del orden.
  Solo existe para demostrar que el test de más abajo tiene dientes."
  [g grilla paso ox oy bx by]
  (-> (procesar-estandar g grilla paso ox oy bx by)
      (grid/pintar-celda 0 0 (if (zero? bx) :agua :arena))))

(deftest orden-no-altera-el-resultado
  (testing "invertir el orden de los bloques no cambia la grilla resultante"
    (dotimes [paso 4]
      (let [estado (assoc (estado-de-prueba) :paso paso)
            orden (orden-de-bloques paso)]
        (is (= (:grilla (state/aplicar-paso estado))
               (:grilla (state/aplicar-paso-con estado
                                                (reverse orden)
                                                procesar-estandar)))
            (str "paso " paso ", desplazamiento " (state/desplazamiento paso)))))))

(deftest el-test-tiene-dientes
  (testing "con una regla que escribe fuera de su bloque el orden SÍ importa"
    (dotimes [paso 4]
      (let [estado (assoc (estado-de-prueba) :paso paso)
            orden (orden-de-bloques paso)]
        (is (not= (:grilla (state/aplicar-paso-con estado orden procesar-trampa))
                  (:grilla (state/aplicar-paso-con estado
                                                   (reverse orden)
                                                   procesar-trampa)))
            (str "paso " paso ", desplazamiento " (state/desplazamiento paso)))))))
