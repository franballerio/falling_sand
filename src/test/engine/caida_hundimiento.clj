(ns engine.caida-hundimiento
  (:require [clojure.test :refer :all]
            [engine.rules :refer [procesar-bloque]]))

(deftest caida-test
  (testing "Caída recta"
    (is (= [:aire :aire :arena :pared] (procesar-bloque [:arena :aire :aire :pared] 0))))

  (testing "Caída en diagonal sobre pared"
    (is (= [:aire :aire :pared :arena] (procesar-bloque [:arena :aire :pared :aire] 0)))
    (is (= [:aire :aire :pared :arena] (procesar-bloque [:arena :aire :pared :aire] 1)))
    (is (= [:aire :aire :pared :arena] (procesar-bloque [:aire :arena :pared :aire] 1))))

  (testing "Caída en diagonal sobre piedra u obstáculos"
    (is (= [:aire :aire :piedra :arena] (procesar-bloque [:arena :aire :piedra :aire] 0)))
    (is (= [:vidrio :aire :piedra :arena] (procesar-bloque [:vidrio :arena :piedra :aire] 0))))

  (testing "Múltiples materiales cayendo"
    (is (= [:arena :aire :pared :arena] (procesar-bloque [:arena :arena :pared :aire] 0)))
    (is (= [:aire :aire :arena :arena] (procesar-bloque [:arena :arena :aire :aire] 0)))
    (is (= [:aire :aire :agua :agua] (procesar-bloque [:agua :agua :aire :aire] 0))))

  (testing "Agua cayendo y deslizándose (evaluando fases)"
    (is (= [:aire :aire :agua :aire] (procesar-bloque [:agua :aire :aire :aire] 1)))
    (is (= [:aire :aire :aire :agua] (procesar-bloque [:agua :aire :aire :aire] 0)))))

(deftest hundimiento-densidad-test
  (testing "Arena hundiéndose en líquidos"
    (is (= [:aire :aire :arena :agua] (procesar-bloque [:arena :aire :agua :aire] 1)))
    (is (= [:agua :agua :arena :arena] (procesar-bloque [:arena :arena :agua :agua] 1)))
    (is (= [:aire :aire :arena :lava] (procesar-bloque [:arena :aire :lava :aire] 0))))

  (testing "Lava en agua"
    (is (= [:lava :aire :agua :aire] (procesar-bloque [:lava :aire :agua :aire] 1)))
    (is (= [:aire :aire :lava :agua] (procesar-bloque [:lava :aire :agua :aire] 5)))))