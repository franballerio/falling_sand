(defproject sand-sim "0.1.0-SNAPSHOT"
  :description "Simulador de materiales falling sand - TP2"
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :main sand-sim.ui.app
  :test-paths ["src/test"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})