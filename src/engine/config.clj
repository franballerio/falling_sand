(ns engine.config)

;;densidad de los materiales para saber como se desplazan
(def tabla-materiales
  {:aire   {:tipo :vacio,   :densidad 0}
   :pared  {:tipo :fijo,    :densidad 0}
   :piedra {:tipo :fijo,    :densidad 0}
   :vidrio {:tipo :fijo,    :densidad 0}
   :madera {:tipo :fijo,    :densidad 0}
   :arena  {:tipo :polvo,   :densidad 4}
   :agua   {:tipo :liquido, :densidad 2}
   :lava   {:tipo :liquido, :densidad 3,  :periodo-movimiento 3}
   :vapor  {:tipo :gas,     :densidad -1, :decaimiento {:destino :agua, :N 100}}
   :fuego  {:tipo :gas,     :densidad -2, :decaimiento {:destino :aire, :N 10}}})


(def tabla-reacciones
  {
   #{:fuego :agua}    {:fuego :aire,   :agua :vapor,   :N 2}
   #{:agua :lava}     {:agua :vapor,   :lava :piedra,  :N 2}
   #{:arena :lava}    {:arena :vidrio, :lava :lava,    :N 20}
   #{:fuego :madera}  {:fuego :fuego,  :madera :fuego, :N 20}
   #{:lava :madera}   {:lava :lava,    :madera :fuego, :N 5}})

(def filas 160)
(def columnas 120)