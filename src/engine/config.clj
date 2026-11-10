(ns engine.config)

(def tabla-materiales
  "Tabla de materiales. Cada entrada define :tipo, :densidad, :color ARGB y
  :simbolo. Opcionalmente incluye :periodo-movimiento y :decaimiento."
  {:aire   {:tipo :vacio,   :densidad 0,  :color (unchecked-int 0xFF000000) :simbolo "."}
   :pared  {:tipo :fijo,    :densidad 0,  :color (unchecked-int 0xFF808080) :simbolo "#"}
   :piedra {:tipo :fijo,    :densidad 0,  :color (unchecked-int 0xFF555560) :simbolo "p"}
   :vidrio {:tipo :fijo,    :densidad 0,  :color (unchecked-int 0xFF9EDCE8) :simbolo "v"}
   :madera {:tipo :fijo,    :densidad 0,  :color (unchecked-int 0xFF8B5A2B) :simbolo "m"}
   :arena  {:tipo :polvo,   :densidad 4,  :color (unchecked-int 0xFFE0C068) :simbolo "a"}
   :agua   {:tipo :liquido, :densidad 2,  :color (unchecked-int 0xFF2060E0) :simbolo "g"}
   :lava   {:tipo :liquido, :densidad 3,  :periodo-movimiento 3, :color (unchecked-int 0xFFE85010) :simbolo "l"}
   :vapor  {:tipo :gas,     :densidad -1, :decaimiento {:destino :agua, :N 100}, :color (unchecked-int 0xFFC8C8D8) :simbolo "^"}
   :fuego  {:tipo :gas,     :densidad -2, :decaimiento {:destino :aire, :N 10}, :color (unchecked-int 0xFFFFC020) :simbolo "f"}})

(def materiales
  "Materiales en el orden que usa la interfaz para el selector."
  [:aire :pared :arena :agua :vapor :lava :fuego :madera :piedra :vidrio])

(def tabla-reacciones
  "Reacciones entre pares de materiales. La clave es un conjunto con los dos
  materiales y el valor indica el resultado de cada uno y el período N."
  {#{:fuego :agua}   {:fuego :aire,   :agua :vapor,   :N 2}
   #{:agua :lava}    {:agua :vapor,   :lava :piedra,  :N 2}
   #{:arena :lava}   {:arena :vidrio, :lava :lava,    :N 20}
   #{:fuego :madera} {:fuego :fuego,  :madera :fuego, :N 20}
   #{:lava :madera}  {:lava :lava,    :madera :fuego, :N 5}})

(def ancho
  "Ancho de la grilla en celdas."
  160)

(def alto
  "Alto de la grilla en celdas."
  120)

(defn color-de
  "Devuelve el color ARGB del material, o nil si el material no existe."
  [material]
  (:color (get tabla-materiales material)))

(defn simbolo-de
  "Devuelve el símbolo de texto del material, o nil si el material no existe."
  [material]
  (:simbolo (get tabla-materiales material)))
