(ns engine.casos-bordes
   (:require [clojure.test :refer :all]
            [engine.rules :refer [procesar-bloque]]))

(deftest bloques-estaticos-test
  (testing "Bloques completamente inactivos"
    (is (= [:aire :aire :aire :aire] (procesar-bloque [:aire :aire :aire :aire] 0)))
    (is (= [:pared :pared :pared :pared] (procesar-bloque [:pared :pared :pared :pared] 0))))

  (testing "Materiales estáticos (paredes no se desplazan ni caen)"
    (is (= [:pared :aire :aire :aire] (procesar-bloque [:pared :aire :aire :aire] 0))))

  (testing "Polvos apoyados sin espacio lateral no se deslizan"
    (is (= [:arena :arena :pared :pared] (procesar-bloque [:arena :arena :pared :pared] 0)))
    (is (= [:arena :aire :pared :pared] (procesar-bloque [:arena :aire :pared :pared] 0))))

  (testing "Líquidos apoyados o sin diferencia de densidad"
    (is (= [:lava :lava :piedra :piedra] (procesar-bloque [:lava :lava :piedra :piedra] 0)))
    (is (= [:agua :agua :agua :agua] (procesar-bloque [:agua :agua :agua :agua] 0)))))