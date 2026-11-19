(ns engine.paso-paralelo
  "Verifica que el paso paralelo sea indistinguible del secuencial.

  La referencia es `aplicar-paso-con` con el procesador estándar: secuencial,
  determinístico y ya cubierto por los otros tests. El paralelo tiene que dar
  exactamente el mismo estado."
  (:require [clojure.test :refer [deftest testing is]]
            [engine.state :as state]))

(defn- grilla-variada
  "Grilla determinística con variedad de materiales, para que las reglas tengan
  algo que hacer en cada fase."
  [ancho alto]
  (let [materiales [:aire :arena :agua :lava :vapor :fuego :madera :pared
                    :obsidiana]]
    (vec (for [y (range alto)]
           (vec (for [x (range ancho)]
                  (nth materiales
                       (mod (+ (* 7 x) (* 13 y)) (count materiales)))))))))

(defn- estado-de-prueba
  [ancho alto]
  (assoc (state/estado-inicial ancho alto)
         :grilla (grilla-variada ancho alto)))

(defn- paso-secuencial
  "Un paso por la vía secuencial, calculando el orden a partir del paso actual."
  [estado]
  (let [[ox oy] (state/desplazamiento (:paso estado))]
    (state/aplicar-paso-con
     estado
     (state/indices-bloques (:ancho estado) (:alto estado) ox oy))))

(deftest paralelo-equivale-al-secuencial
  (testing "misma grilla y mismo :paso, para varios tamaños y desplazamientos"
    (doseq [[ancho alto] [[12 10] [33 21] [160 120]
                          [160 120]]
            paso (range 4)]
      (let [estado (assoc (estado-de-prueba ancho alto) :paso paso)]
        (is (= (paso-secuencial estado)
               (state/aplicar-paso-paralelo estado))
            (str ancho "x" alto ", paso " paso
                 ", desplazamiento " (state/desplazamiento paso))))))

  (testing "encadenando pasos el desvío no se acumula"
    (let [inicial (estado-de-prueba 40 30)]
      (is (= (nth (iterate paso-secuencial inicial) 8)
             (nth (iterate state/aplicar-paso-paralelo inicial) 8))
          "8 pasos seguidos, comparando estado completo"))))

(deftest celdas-fuera-de-los-bloques-se-conservan
  (testing "con desplazamiento [1 1] no hay bloque sobre la fila 0, la última
            fila, la columna 0 ni la última columna"
    (let [ancho 32
          alto 24
          estado (assoc (estado-de-prueba ancho alto) :paso 1)
          antes (:grilla estado)
          despues (:grilla (state/aplicar-paso-paralelo estado))
          ultima-fila (dec alto)
          ultima-col (dec ancho)]
      (is (= [1 1] (state/desplazamiento 1)))

      (is (= (nth antes 0) (nth despues 0)) "fila 0 intacta")
      (is (= (nth antes ultima-fila) (nth despues ultima-fila))
          "última fila intacta")
      (is (every? true? (map = (map #(nth % 0) antes)
                             (map #(nth % 0) despues)))
          "columna 0 intacta")
      (is (every? true? (map = (map #(nth % ultima-col) antes)
                             (map #(nth % ultima-col) despues)))
          "última columna intacta")

      ;; Y que el test no pase por vacío: el paso tiene que haber hecho algo.
      (is (not= antes despues)
          "el paso tiene que haber modificado celdas dentro de los bloques"))))

(deftest grilla-vacia-no-rompe
  (testing "un paso sobre una grilla sin partículas no falla y no cambia nada"
    (let [estado (state/estado-inicial 32 24)
          nuevo (state/aplicar-paso-paralelo estado)]
      (is (= (:grilla estado) (:grilla nuevo)))
      (is (= 1 (:paso nuevo))))))

(deftest grilla-mas-chica-que-un-bloque
  (testing "sin bloques completos el paso solo avanza el contador"
    (let [estado (assoc (estado-de-prueba 1 1) :paso 0)
          nuevo (state/aplicar-paso-paralelo estado)]
      (is (= (:grilla estado) (:grilla nuevo)))
      (is (= 1 (:paso nuevo))))))
