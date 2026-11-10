(ns engine.rules
  (:require [engine.config :as config]))

(defn aplicar-decaimiento
  "Etapa 1 de la regla del bloque. Recorre las posiciones 0 a 3 y transforma
  cada material que tenga decaimiento si se cumple su decisión de período N
  con k igual al número de posición. Devuelve un bloque nuevo."
  [bloque fase]
  (vec
   (map-indexed
    (fn [k material]
      (if-let [regla-decaimiento (get-in config/tabla-materiales [material :decaimiento])]
        (let [destino (:destino regla-decaimiento)
              N       (:N regla-decaimiento)]
          (if (zero? (mod (+ fase (* 7 k)) N))
            destino
            material))
        material))
    bloque)))

(defn aplicar-reacciones
  "Etapa 2 de la regla del bloque. Evalúa los seis pares de posiciones en orden
  y aplica las reacciones configuradas usando los materiales actuales del bloque.
  La decisión del par q usa k = 4 + q."
  [bloque fase]
  (let [pares-a-evaluar [{:q 0 :indices [0 1]}
                         {:q 1 :indices [2 3]}
                         {:q 2 :indices [0 2]}
                         {:q 3 :indices [1 3]}
                         {:q 4 :indices [0 3]}
                         {:q 5 :indices [1 2]}]]
    (reduce
     (fn [acc-bloque {:keys [q indices]}]
       (let [[i j] indices
             mat1 (get acc-bloque i)
             mat2 (get acc-bloque j)
             par-set (hash-set mat1 mat2)]
         (if-let [reaccion (get config/tabla-reacciones par-set)]
           (let [N (:N reaccion)
                 k (+ 4 q)]
             (if (zero? (mod (+ fase (* 7 k)) N))
               (-> acc-bloque
                   (assoc i (get reaccion mat1))
                   (assoc j (get reaccion mat2)))
               acc-bloque))
           acc-bloque)))
     bloque
     pares-a-evaluar)))

(defn- obtener-material
  "Devuelve la configuración del material, o nil si no existe."
  [mat]
  (get config/tabla-materiales mat))

(defn- obtener-densidad
  "Devuelve la densidad del material; 0 si no está definida."
  [mat]
  (let [m (obtener-material mat)]
    (or (:densidad m) 0)))

(defn- se-desplaza?
  "Indica si el material puede deslizarse horizontalmente en la fila indicada.
  Los gases se deslizan en cualquier fila y los líquidos solo en la inferior."
  [mat fila]
  (let [m (obtener-material mat)
        tipo (:tipo m)]
    (cond
      (= tipo :gas) true
      (and (= tipo :liquido) (= fila 1)) true
      :else false)))

(defn- es-vacio?
  "Indica si el material es vacío (:aire o de tipo :vacio)."
  [mat]
  (let [m (obtener-material mat)]
    (or (= mat :aire) (= (:tipo m) :vacio))))

(defn- intentar-swap
  "Intercambia las posiciones i y j si ambas tienen permiso de movimiento y la
  densidad del material de i es estrictamente mayor que la del material de j.
  Devuelve el bloque resultante."
  [bloque permisos i j]
  (if (and (get permisos i) (get permisos j))
    (let [mat-i (get bloque i)
          mat-j (get bloque j)
          dens-i (obtener-densidad mat-i)
          dens-j (obtener-densidad mat-j)]
      (if (> dens-i dens-j)
        (-> bloque
            (assoc i mat-j)
            (assoc j mat-i))
        bloque))
    bloque))

(defn- intentar-deslizamiento
  "Intercambia las posiciones i y j de la misma fila si ambas tienen permiso,
  se cumple la decisión de período 2 y una contiene un material desplazable
  mientras la otra está vacía. Devuelve el bloque resultante."
  [bloque permisos fase i j fila k]
  (let [periodo-ok? (zero? (mod (+ fase (* 7 k)) 2))]
    (if (and (get permisos i) (get permisos j) periodo-ok?)
      (let [mat-i (get bloque i)
            mat-j (get bloque j)]
        (cond
          (and (se-desplaza? mat-i fila) (es-vacio? mat-j))
          (-> bloque (assoc i mat-j) (assoc j mat-i))

          (and (se-desplaza? mat-j fila) (es-vacio? mat-i))
          (-> bloque (assoc i mat-j) (assoc j mat-i))

          :else bloque))
      bloque)))

(defn aplicar-movimiento
  "Etapa 3 de la regla del bloque. Calcula los permisos de movimiento por
  posición y aplica intercambios verticales, diagonales y deslizamientos
  horizontales en el orden indicado por el enunciado."
  [bloque fase]
  (let [permisos (vec (map (fn [i]
                             (let [mat (get bloque i)
                                   m (obtener-material mat)
                                   tipo (:tipo m)
                                   no-fijo? (not= tipo :fijo)
                                   N (or (:periodo-movimiento m) 1)
                                   k (+ 10 i)]
                               (and no-fijo?
                                    (zero? (mod (+ fase (* 7 k)) N)))))
                           [0 1 2 3]))

        b1 (intentar-swap bloque permisos 0 2)
        b2 (intentar-swap b1 permisos 1 3)

        evaluar-diagonal? (zero? (mod (+ fase (* 7 14)) 2))
        b3 (if evaluar-diagonal?
             (-> b2
                 (intentar-swap permisos 0 3)
                 (intentar-swap permisos 1 2))
             (-> b2
                 (intentar-swap permisos 1 2)
                 (intentar-swap permisos 0 3)))

        b4 (intentar-deslizamiento b3 permisos fase 0 1 0 15)
        b5 (intentar-deslizamiento b4 permisos fase 2 3 1 16)]
    b5))

(defn procesar-bloque
  "Aplica las tres etapas de la regla del bloque (decaimiento, reacciones y
  movimiento) en orden y devuelve el bloque resultante."
  [bloque fase]
  (-> bloque
      (aplicar-decaimiento fase)
      (aplicar-reacciones fase)
      (aplicar-movimiento fase)))
