(ns engine.decaimiento-reacciones
  (:require [clojure.test :refer :all]
            [engine.rules :refer [procesar-bloque]]))

(deftest decaimiento-test
  (testing "Decaimiento del fuego (fuego -> aire)"
    (is (= [:pared :aire :pared :pared] (procesar-bloque [:pared :fuego :pared :pared] 3)))
    (is (= [:pared :fuego :pared :pared] (procesar-bloque [:pared :fuego :pared :pared] 0))))

  (testing "Condensación del vapor (vapor -> agua)"
    (is (= [:pared :agua :pared :pared] (procesar-bloque [:pared :vapor :pared :pared] 93)))
    (is (= [:pared :vapor :pared :pared] (procesar-bloque [:pared :vapor :pared :pared] 92)))))

(deftest reacciones-test
  (testing "Fuego y Agua"
    (is (= [:aire :vapor :pared :pared] (procesar-bloque [:fuego :agua :pared :pared] 2)))
    (is (= [:fuego :agua :pared :pared] (procesar-bloque [:fuego :agua :pared :pared] 3))))

  (testing "Agua y Lava (el agua enfría la lava: queda piedra y vapor)"
    (is (= [:vapor :piedra :pared :pared] (procesar-bloque [:agua :lava :pared :pared] 2)))
    (is (= [:agua :lava :pared :pared] (procesar-bloque [:agua :lava :pared :pared] 3)))
    (is (= [:vapor :piedra :pared :pared] (procesar-bloque [:agua :lava :pared :pared] 4))))

  (testing "Arena y Lava"
    (is (= [:vidrio :lava :pared :pared] (procesar-bloque [:arena :lava :pared :pared] 12)))
    (is (= [:arena :lava :pared :pared] (procesar-bloque [:arena :lava :pared :pared] 11))))

  (testing "Lava y Madera"
    (is (= [:lava :fuego :pared :pared] (procesar-bloque [:lava :madera :pared :pared] 2)))
    (is (= [:lava :madera :pared :pared] (procesar-bloque [:lava :madera :pared :pared] 1))))

  (testing "Fuego y Madera"
    (is (= [:fuego :fuego :pared :pared] (procesar-bloque [:fuego :madera :pared :pared] 12)))
    (is (= [:fuego :madera :pared :pared] (procesar-bloque [:fuego :madera :pared :pared] 11))))

  (testing "Reacciones en fila inferior"
    (is (= [:pared :pared :lava :fuego] (procesar-bloque [:pared :pared :lava :madera] 0))))

  (testing "Reacciones secuenciales con materiales actualizados"
    (is (= [:aire :vapor :lava :pared] (procesar-bloque [:fuego :agua :lava :pared] 2)))))

(deftest obsidiana-inerte-test
  (testing "La obsidiana no reacciona con el agua"
    (is (= [:obsidiana :agua :pared :pared]
           (procesar-bloque [:obsidiana :agua :pared :pared] 0))))

  (testing "La obsidiana no reacciona con la lava"
    (is (= [:obsidiana :lava :pared :pared]
           (procesar-bloque [:obsidiana :lava :pared :pared] 0))))

  (testing "La obsidiana no decae: queda igual en todas las fases"
    (doseq [fase (range 6)]
      (is (= [:obsidiana :obsidiana :pared :pared]
             (procesar-bloque [:obsidiana :obsidiana :pared :pared] fase))
          (str "fase " fase)))))