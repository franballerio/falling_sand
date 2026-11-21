(ns ui.app
  "Une la vista JavaFX con el motor de simulación.

  La vista solo informa lo que hace el usuario. Cada aviso se guarda en una
  cola y lo aplica el hilo de simulación, que es el único que modifica el
  estado. Así se puede pintar mientras la simulación corre sin perder el
  dibujo ni atrasar los pasos."
  (:require [engine.config :as config]
            [engine.state :as state])
  (:import [java.util.concurrent LinkedBlockingQueue]
           [javafx.application Platform]
           [javafx.event EventHandler]
           [javafx.scene Scene]
           [javafx.stage Stage]
           [ui.vista VistaSimulador VistaListener OpcionMaterial]))

(def ^:private tamano-celda 5)
(def ^:private ms-por-paso 16)

;; Datos que usa la vista.

(defn- opciones-materiales
  "Arma las opciones del selector con el nombre y el color de cada material."
  []
  (mapv #(OpcionMaterial. (name %) (int (config/color-de %)))
        config/materiales))

(defn- a-pixeles
  "Convierte la grilla en un arreglo ARGB listo para dibujar."
  ^ints [grilla]
  (let [colores (into {}
                      (map (fn [[k v]] [k (int (:color v))]))
                      config/tabla-materiales)]
    (int-array (for [fila grilla, m fila] (colores m)))))

;; La vista avisa acá y nosotros lo convertimos en cambios de estado.
;; Un comando es una función que recibe un estado y devuelve otro.

(defn- crear-listener
  "Traduce los eventos de la vista en comandos para el hilo de simulación."
  [^LinkedBlockingQueue cola pausado?]
  (reify VistaListener
    (alPintar [_ x0 y0 x1 y1 radio material]
      (let [mat (keyword material)]
        (.put cola (fn [estado]
                     (state/pintar-segmento estado x0 y0 x1 y1 radio mat)))))
    (alAlternarPausa [_ pausado]
      (reset! pausado? pausado))
    (alPedirPaso [_]
      (.put cola state/aplicar-paso))
    (alLimpiar [_]
      (.put cola state/limpiar))))

;; Hilo de simulación.

(defn- drenar
  "Aplica en orden los comandos que haya en la cola."
  [^LinkedBlockingQueue cola estado]
  (loop [e estado]
    (if-let [f (.poll cola)]
      (recur (f e))
      e)))

(defn- bucle
  "Avanza la simulación y manda cada estado nuevo a la vista."
  [^VistaSimulador vista cola pausado? corriendo?]
  (loop [estado (state/estado-inicial)]
    (when @corriendo?
      (let [t0     (System/nanoTime)
            estado (drenar cola estado)
            estado (if @pausado? estado (state/aplicar-paso estado))]
        (.mostrar vista
                  (a-pixeles (:grilla estado))
                  (long (:paso estado))
                  (int (state/contar-particulas estado)))
        (let [ms (quot (- (System/nanoTime) t0) 1000000)]
          (when (< ms ms-por-paso)
            (Thread/sleep (long (- ms-por-paso ms)))))
        (recur estado)))))

;; Arranque.

(defn- iniciar
  "Prepara la ventana y arranca el hilo de simulación. Se ejecuta en el hilo de JavaFX."
  []
  (let [cola       (LinkedBlockingQueue.)
        pausado?   (atom false)
        corriendo? (atom true)
        vista      (VistaSimulador. config/ancho config/alto tamano-celda
                                    (opciones-materiales))
        stage      (Stage.)]
    (.agregarListener vista (crear-listener cola pausado?))
    (.setTitle stage "Simulador de materiales")
    (.setScene stage (Scene. vista))
    (.setResizable stage false)
    (.setOnCloseRequest stage
                        (reify EventHandler
                          (handle [_ _]
                            (reset! corriendo? false)
                            (shutdown-agents)
                            (System/exit 0))))
    (.show stage)
    (doto (Thread. ^Runnable #(bucle vista cola pausado? corriendo?) "simulacion")
      (.setDaemon true)
      (.start))))

(defn -main
  "Punto de entrada: inicia JavaFX y abre la ventana."
  [& _args]
  (Platform/startup iniciar))
