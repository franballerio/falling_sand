(ns engine.state-test
  (:require [clojure.test :refer [deftest testing is]]
            [engine.state :as state]))

(def a :aire)
(def p :pared)
(def s :arena)
(def g :agua)
(def v :vapor)
(def f :fuego)
(def l :lava)

(defn matriz [filas]
  (mapv vec filas))

(defn estado [filas paso]
  {:grilla (matriz filas)
   :paso paso
   :ancho (count (first filas))
   :alto (count filas)})

(defn grilla-tras [estado n]
  (:grilla (nth (iterate state/aplicar-paso estado) n)))


(deftest estado-inicial-test
  (let [estado (state/estado-inicial)]
    (is (= 160 (count (first (:grilla estado)))))
    (is (= 120 (count (:grilla estado))))
    (is (= 0 (:paso estado))))
  (let [estado (state/estado-inicial 7 3)]
    (is (= 7 (count (first (:grilla estado)))))
    (is (= 3 (count (:grilla estado))))
    (is (= 7 (:ancho estado)))
    (is (= 3 (:alto estado)))))


(deftest arena-cae-en-grilla-8x5
  (let [inicial (estado [[a a a s a a a a]
                         [a a a a a a a a]
                         [a a a a a a a a]
                         [a a a a a a a a]
                         [p p p p p p p p]] 0)]
    (testing "n=1: la arena baja una fila"
      (is (= (matriz [[a a a a a a a a]
                      [a a a s a a a a]
                      [a a a a a a a a]
                      [a a a a a a a a]
                      [p p p p p p p p]])
             (grilla-tras inicial 1))))
    (testing "n=2: sigue cayendo"
      (is (= (matriz [[a a a a a a a a]
                      [a a a a a a a a]
                      [a a a s a a a a]
                      [a a a a a a a a]
                      [p p p p p p p p]])
             (grilla-tras inicial 2))))
    (testing "n=3: llega al piso"
      (is (= (matriz [[a a a a a a a a]
                      [a a a a a a a a]
                      [a a a a a a a a]
                      [a a a s a a a a]
                      [p p p p p p p p]])
             (grilla-tras inicial 3))))
    (testing "n=4 y n=5: queda apoyada"
      (let [apoyada (matriz [[a a a a a a a a]
                             [a a a a a a a a]
                             [a a a a a a a a]
                             [a a a s a a a a]
                             [p p p p p p p p]])]
        (is (= apoyada (grilla-tras inicial 4)))
        (is (= apoyada (grilla-tras inicial 5)))))))


(deftest agua-se-nivela-en-grilla-4x4
  (let [inicial (estado [[g g g g]
                         [a a a a]
                         [a a a a]
                         [p p p p]] 0)]
    (testing "n=1: la fila de agua cae"
      (is (= (matriz [[a a a a]
                      [g g g g]
                      [a a a a]
                      [p p p p]])
             (grilla-tras inicial 1))))
    (testing "n=2: se abre y baja por los costados"
      (is (= (matriz [[a a a a]
                      [g a a g]
                      [a g g a]
                      [p p p p]])
             (grilla-tras inicial 2))))
    (testing "n=3: mismo estado"
      (is (= (matriz [[a a a a]
                      [g a a g]
                      [a g g a]
                      [p p p p]])
             (grilla-tras inicial 3))))
    (testing "n=4 y n=5: se junta abajo"
      (let [abajo (matriz [[a a a a]
                           [a a a a]
                           [g g g g]
                           [p p p p]])]
        (is (= abajo (grilla-tras inicial 4)))
        (is (= abajo (grilla-tras inicial 5)))))))


(deftest ancho-impar-deja-celdas-fuera-de-los-bloques
  (let [inicial (estado [[a a a a s]
                         [a a a a a]
                         [a a a a a]
                         [p p p p p]] 0)]
    (testing "n=1 y n=2: la columna 4 queda afuera y no cambia"
      (is (= (matriz [[a a a a s]
                      [a a a a a]
                      [a a a a a]
                      [p p p p p]])
             (grilla-tras inicial 1)))
      (is (= (matriz [[a a a a s]
                      [a a a a a]
                      [a a a a a]
                      [p p p p p]])
             (grilla-tras inicial 2))))
    (testing "n=3: con otro desplazamiento la arena baja"
      (is (= (matriz [[a a a a a]
                      [a a a a s]
                      [a a a a a]
                      [p p p p p]])
             (grilla-tras inicial 3))))
    (testing "n=4 y n=5: queda apoyada"
      (let [apoyada (matriz [[a a a a a]
                             [a a a a s]
                             [a a a a a]
                             [p p p p p]])]
        (is (= apoyada (grilla-tras inicial 4)))
        (is (= apoyada (grilla-tras inicial 5)))))))


(deftest grilla-minima-3x3
  (let [inicial (estado [[s a a]
                         [a a a]
                         [p p p]] 0)]
    (is (= (matriz [[a a a]
                    [s a a]
                    [p p p]])
           (grilla-tras inicial 1)))))


(deftest arena-se-hunde-en-agua-2x5
  (let [inicial (estado [[s a]
                         [a a]
                         [g g]
                         [g g]
                         [p p]] 0)]
    (testing "n=1 a n=3: la arena baja hasta el agua"
      (let [esperada (matriz [[a a]
                              [s a]
                              [g g]
                              [g g]
                              [p p]])]
        (is (= esperada (grilla-tras inicial 1)))
        (is (= esperada (grilla-tras inicial 2)))
        (is (= esperada (grilla-tras inicial 3)))))
    (testing "n=4: se hunde y el agua sube"
      (is (= (matriz [[a a]
                      [g a]
                      [s g]
                      [g g]
                      [p p]])
             (grilla-tras inicial 4))))
    (testing "n=5 y n=6: sigue hundiéndose"
      (let [esperada (matriz [[a a]
                              [g a]
                              [g g]
                              [s g]
                              [p p]])]
        (is (= esperada (grilla-tras inicial 5)))
        (is (= esperada (grilla-tras inicial 6)))))))


(deftest vapor-sube-4x4
  (let [inicial (estado [[a a a a]
                         [a a a a]
                         [a a a a]
                         [v a a a]] 0)]
    (testing "n=1"
      (is (= (matriz [[a a a a]
                      [a a a a]
                      [a v a a]
                      [a a a a]])
             (grilla-tras inicial 1))))
    (testing "n=2"
      (is (= (matriz [[a a a a]
                      [a v a a]
                      [a a a a]
                      [a a a a]])
             (grilla-tras inicial 2))))
    (testing "n=3 y n=4: llega arriba"
      (let [arriba (matriz [[a v a a]
                            [a a a a]
                            [a a a a]
                            [a a a a]])]
        (is (= arriba (grilla-tras inicial 3)))
        (is (= arriba (grilla-tras inicial 4)))))
    (testing "n=5 y n=6: se corre a la izquierda"
      (let [izquierda (matriz [[v a a a]
                               [a a a a]
                               [a a a a]
                               [a a a a]])]
        (is (= izquierda (grilla-tras inicial 5)))
        (is (= izquierda (grilla-tras inicial 6)))))))


(deftest contar-particulas-test
  (is (= 3 (state/contar-particulas
            (estado [[s a]
                     [g p]] 0))))
  (is (= 0 (state/contar-particulas
            (estado [[a a]
                     [a a]] 0)))))


(deftest limpiar-test
  (let [estado (state/limpiar (estado [[s a]
                                       [g p]] 7))]
    (is (= [[a a] [a a]] (:grilla estado)))
    (is (= 7 (:paso estado)))
    (is (= 2 (:ancho estado)))
    (is (= 2 (:alto estado)))))


(deftest pintar-disco-test
  (testing "radio 1 pinta una cruz"
    (is (= (matriz [[a s a]
                    [s s s]
                    [a s a]])
           (:grilla (state/pintar-disco
                     (estado [[a a a]
                              [a a a]
                              [a a a]] 0)
                     1 1 1 s)))))
  (testing "recorta las celdas fuera de la grilla"
    (is (= (matriz [[s s a]
                    [s a a]
                    [a a a]])
           (:grilla (state/pintar-disco
                     (estado [[a a a]
                              [a a a]
                              [a a a]] 0)
                     0 0 1 s)))))
  (testing "radio 0 pinta solo el centro"
    (is (= (matriz [[a a a]
                    [a p a]
                    [a a a]])
           (:grilla (state/pintar-disco
                     (estado [[a a a]
                              [a a a]
                              [a a a]] 0)
                     1 1 0 p))))))


(deftest pintar-segmento-test
  (testing "horizontal sin huecos"
    (is (= (matriz [[p p p p]
                    [a a a a]
                    [a a a a]])
           (:grilla (state/pintar-segmento
                     (estado [[a a a a]
                              [a a a a]
                              [a a a a]] 0)
                     0 0 3 0 0 p)))))
  (testing "diagonal sin huecos"
    (is (= (matriz [[g a a a]
                    [a g a a]
                    [a a g a]
                    [a a a g]])
           (:grilla (state/pintar-segmento
                     (estado [[a a a a]
                              [a a a a]
                              [a a a a]
                              [a a a a]] 0)
                     0 0 3 3 0 g)))))
  (testing "un solo punto delega en pintar-disco"
    (is (= (matriz [[a a a]
                    [a s a]
                    [a a a]])
           (:grilla (state/pintar-segmento
                     (estado [[a a a]
                              [a a a]
                              [a a a]] 0)
                     1 1 1 1 0 s))))))
