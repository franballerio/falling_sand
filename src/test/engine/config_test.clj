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

(deftest dimensiones-test
  (is (= 160 config/ancho))
  (is (= 120 config/alto)))
