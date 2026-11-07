(ns engine.deslizamiento-gases
  (:require [clojure.test :refer :all]
            [engine.rules :refer [procesar-bloque]]))

(deftest deslizamiento-horizontal-test
  (testing "Líquidos en la fila superior no se deslizan"
    (is (= [:agua :aire :pared :pared] (procesar-bloque [:agua :aire :pared :pared] 0)))
    (is (= [:agua :aire :pared :pared] (procesar-bloque [:agua :aire :pared :pared] 1))))

  (testing "Líquidos apoyados en la fila inferior"
    (is (= [:pared :pared :aire :agua] (procesar-bloque [:pared :pared :agua :aire] 0)))
    (is (= [:pared :pared :agua :aire] (procesar-bloque [:pared :pared :agua :aire] 1))))

  (testing "Falta de espacio vacío o presencia de pared lateral"
    (is (= [:pared :pared :agua :agua] (procesar-bloque [:pared :pared :agua :agua] 0)))
    (is (= [:aire :aire :pared :agua] (procesar-bloque [:aire :aire :pared :agua] 0))))

  (testing "Lava con y sin permiso de movimiento"
    (is (= [:aire :aire :lava :aire] (procesar-bloque [:aire :aire :lava :aire] 1)))
    (is (= [:aire :aire :aire :lava] (procesar-bloque [:aire :aire :lava :aire] 0)))))

(deftest gases-test
  (testing "Gases que suben"
    (is (= [:vapor :vapor :aire :aire] (procesar-bloque [:aire :aire :vapor :vapor] 0)))
    (is (= [:fuego :aire :aire :aire] (procesar-bloque [:aire :aire :fuego :aire] 0)))
    (is (= [:aire :fuego :aire :aire] (procesar-bloque [:aire :aire :fuego :aire] 1)))
    (is (= [:vapor :fuego :aire :aire] (procesar-bloque [:aire :aire :vapor :fuego] 1)))
    (is (= [:fuego :vapor :aire :aire] (procesar-bloque [:aire :aire :fuego :vapor] 0))))

  (testing "Gases que se deslizan"
    (is (= [:aire :vapor :pared :pared] (procesar-bloque [:vapor :aire :pared :pared] 1)))
    (is (= [:vapor :fuego :pared :pared] (procesar-bloque [:vapor :fuego :pared :pared] 1))))

  (testing "Interacciones múltiples de gases (decaimiento + deslizamiento)"
    (is (= [:aire :vapor :aire :agua] (procesar-bloque [:vapor :vapor :aire :aire] 0)))
    (is (= [:aire :fuego :pared :pared] (procesar-bloque [:fuego :fuego :pared :pared] 3)))))