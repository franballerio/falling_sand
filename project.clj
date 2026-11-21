(defproject sand-sim "0.1.0-SNAPSHOT"
  :description "Simulador de materiales falling sand - TP2"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.openjfx/javafx-base "21"]
                 [org.openjfx/javafx-graphics "21"]
                 [org.openjfx/javafx-controls "21"]
                 [org.openjfx/javafx-fxml "21"]]
  :main ui.app
  :java-source-paths ["src"]
  :javac-options ["-encoding" "UTF-8"]
  :test-paths ["src/test"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
