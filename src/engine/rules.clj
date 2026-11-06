(ns engine.rules
  (:require [engine.config :as config]))

(defn aplicar-decaimiento [bloque fase]
      "recibe el bloque y la fase
      transforma lo que pasa en esta determinada celda
      ej: si era fuego se transforma en aire, si era vapor se transforma en agua"
      (vec
            (map-indexed
                  (fn [k material]
                      ;; buscamos si el material tiene regla de decaimiento en la config
                      (if-let [regla-decaimiento (get-in config/tabla-materiales [material :decaimiento])]
                              (let [destino (:destino regla-decaimiento)
                                    N       (:N regla-decaimiento)]
                                   ;; evaluamos la condición matemática: (fase + 7 * k) es múltiplo de N
                                   (if (zero? (mod (+ fase (* 7 k)) N))
                                         destino     ;; Si se cumple, se transforma en el nuevo material
                                         material))  ;; Si no, queda como estaba
                              ;; Si el material no tiene decaimiento, se queda igual
                              material))
                  bloque)))
(defn aplicar-reacciones [bloque fase]
      (let [pares-a-evaluar [{:q 0 :indices [0 1]} ; Horizontal superior
                             {:q 1 :indices [2 3]} ; Horizontal inferior
                             {:q 2 :indices [0 2]} ; Vertical izquierda
                             {:q 3 :indices [1 3]} ; Vertical derecha
                             {:q 4 :indices [0 3]} ; Diagonal principal
                             {:q 5 :indices [1 2]}]] ; Diagonal secundaria

           ;reduce recibe funcion valor incial y coleccioon. reduce es una funcion acumuladora
           (reduce
                 (fn [acc-bloque {:keys [q indices]}]
                     (let [[i j] indices
                           mat1 (get acc-bloque i)
                           mat2 (get acc-bloque j)
                           par-set (hash-set mat1 mat2)]
                          ;; si tiene reaccion ese par de elementos
                          (if-let [reaccion (get config/tabla-reacciones par-set)]
                                  (let [N (:N reaccion)
                                        k (+ 4 q)]
                                       ;; si la ecuacion se cumple aplicamos la reaccion
                                       (if (zero? (mod (+ fase (* 7 k)) N))
                                             (-> acc-bloque ;; toma el resultado y lo inyecta en la siguiente vuelta de reduce
                                                 (assoc i (get reaccion mat1))
                                                 (assoc j (get reaccion mat2)))
                                             acc-bloque))   ;; si no se cumple el periodo definido por la ecuacion queda acc bloque

                                  acc-bloque)))             ;; si no se cumple el if let queda el acc bloque
                 bloque                                     ;; este es el parametro valor inicial de reduce
                 pares-a-evaluar)))                         ;; parametro coleccion de reduce



(defn- obtener-material [mat]
       (get config/tabla-materiales mat))

(defn- obtener-densidad [mat]
       (let [m (obtener-material mat)]
            (or (:densidad m) 0)))

(defn- se-desplaza? [mat fila]
       (let [m (obtener-material mat)
             tipo (:tipo m)]
            (cond
                  (= tipo :gas) true
                  (and (= tipo :liquido) (= fila 1)) true
                  :else false)))

(defn- es-vacio? [mat]
       (let [m (obtener-material mat)]
            (or (= mat :aire) (= (:tipo m) :vacio))))

(defn- intentar-swap [bloque permisos i j]
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

(defn- intentar-deslizamiento [bloque permisos fase i j fila k]
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

(defn aplicar-movimiento [bloque fase]
      (let [;; 1. Permisos de movimiento calculados por posición (k = 10 + i)
            permisos (vec (map (fn [i]
                                   (let [mat (get bloque i)
                                         m (obtener-material mat)
                                         tipo (:tipo m)
                                         no-fijo? (not= tipo :fijo)
                                         N (or (:N m) (:periodo-movimiento m) 1)
                                         k (+ 10 i)]
                                        (and no-fijo?
                                             (zero? (mod (+ fase (* 7 k)) N)))))
                               [0 1 2 3]))

            ;; 2. Intercambios por densidad
            ;; - Verticales: 0 con 2, luego 1 con 3
            b1 (intentar-swap bloque permisos 0 2)
            b2 (intentar-swap b1 permisos 1 3)

            ;; - Diagonales: orden dependiente de la fase (k = 14, N = 2)
            evaluar-diagonal? (zero? (mod (+ fase (* 7 14)) 2))
            b3 (if evaluar-diagonal?
                     (-> b2
                         (intentar-swap permisos 0 3)
                         (intentar-swap permisos 1 2))
                     (-> b2
                         (intentar-swap permisos 1 2)
                         (intentar-swap permisos 0 3)))

            ;; 3. Deslizamiento horizontal
            ;; - Fila superior (0 y 1), k = 15
            b4 (intentar-deslizamiento b3 permisos fase 0 1 0 15)
            ;; - Fila inferior (2 y 3), k = 16
            b5 (intentar-deslizamiento b4 permisos fase 2 3 1 16)]

           b5))

(defn procesar-bloque
      "Recibe un bloque de 4 materiales y la fase. 
       Aplica las 3 etapas en orden y devuelve el bloque final."
      [bloque fase]
      (-> bloque
          (aplicar-decaimiento fase)  ;; Etapa 1
          (aplicar-reacciones fase)   ;; Etapa 2
          (aplicar-movimiento fase))) ;; Etapa 3