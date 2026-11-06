(ns engine.rules-test
    (:require [clojure.test :refer [deftest testing is]]
      [engine.rules :as rules]))

(deftest regla-del-bloque-test

         (testing "La arena cae directo o en diagonal"
                  (is (= [:aire :aire :arena :aire]
                         (rules/procesar-bloque [:arena :aire :aire :aire] 0)))
                  (is (= [:aire :aire :aire :arena]
                         (rules/procesar-bloque [:aire :arena :aire :aire] 0)))
                  (is (= [:aire :aire :pared :arena]
                         (rules/procesar-bloque [:arena :aire :pared :aire] 0))))

         (testing "Intercambio por densidad (hundimiento)"
                  (is (= [:aire :aire :arena :agua]
                         (rules/procesar-bloque [:arena :aire :agua :aire] 0))))

         (testing "Deslizamiento de líquidos"
                  (is (= [:aire :aire :aire :agua]
                         (rules/procesar-bloque [:aire :aire :agua :aire] 0))
                      "Fase 0: El agua apoyada se desliza")
                  (is (= [:aire :aire :agua :aire]
                         (rules/procesar-bloque [:aire :aire :agua :aire] 1))
                      "Fase 1: No se desliza (1 de cada 2 decisiones)"))

         (testing "Movimiento de gases"
                  (is (= [:vapor :aire :aire :aire]
                         (rules/procesar-bloque [:aire :aire :vapor :aire] 0))))

         (testing "Reacciones"
                  (is (= [:vapor :aire :piedra :aire]
                         (rules/procesar-bloque [:agua :aire :lava :aire] 0))
                      "Fase 0: Reacción agua-lava (N=2)")
                  (is (= [:aire :aire :lava :agua]
                         (rules/procesar-bloque [:agua :aire :lava :aire] 1))
                      "Fase 1: Sin reacción, el agua baja en diagonal"))

         (testing "Lava viscosa (período de movimiento N=3)"
                  (is (= [:lava :aire :aire :aire]
                         (rules/procesar-bloque [:lava :aire :aire :aire] 0))
                      "Fase 0: Lava viscosa, no se mueve")
                  (is (= [:lava :aire :aire :aire]
                         (rules/procesar-bloque [:lava :aire :aire :aire] 1))
                      "Fase 1: Ídem, no se mueve")
                  (is (= [:aire :aire :aire :lava]
                         (rules/procesar-bloque [:lava :aire :aire :aire] 2))
                      "Fase 2: Se mueve (cae y luego se desliza)"))

         (testing "Decaimiento"
                  (is (= [:aire :aire :aire :aire]
                         (rules/procesar-bloque [:fuego :aire :aire :aire] 0))
                      "Fase 0: El fuego se apaga (N=10)")))