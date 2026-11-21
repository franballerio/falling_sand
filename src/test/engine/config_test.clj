(ns engine.config-test
  (:require [clojure.test :refer [deftest is testing]]
            [engine.config :as config]))

(deftest materiales-test
  (is (= (set config/materiales) (set (keys config/tabla-materiales))))
  (doseq [material config/materiales]
    (testing (name material)
      (let [datos (get config/tabla-materiales material)]
        (is (some? datos))
        (is (integer? (:color datos)))
        (is (string? (:simbolo datos)))))))

(deftest obsidiana-test
  (testing "La tabla define obsidiana como material fijo sin densidad"
    (let [datos (get config/tabla-materiales :obsidiana)]
      (is (= :fijo (:tipo datos)))
      (is (= 0 (:densidad datos)))))

  (testing "Color opaco y símbolo propio"
    (is (= (unchecked-int 0xFF1E002E) (config/color-de :obsidiana)))
    (is (= "o" (config/simbolo-de :obsidiana)))
    (is (= 1 (count (filter #(= "o" (config/simbolo-de %))
                            (keys config/tabla-materiales))))
        "El símbolo 'o' identifica solo a la obsidiana"))

  (testing "Está disponible en el selector de materiales"
    (is (some #{:obsidiana} config/materiales))))

(deftest dimensiones-test
  (is (= 160 config/ancho))
  (is (= 120 config/alto)))
