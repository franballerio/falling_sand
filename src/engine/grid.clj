(ns engine.grid)

(defn crear-grilla-vacia
      "Crea una matriz bidimensional (vector de vectores) de ancho x alto.
      Todas las celdas se inicializan con la keyword :aire."
      [width height]
      ;; Lógica para crear un vector de 'alto' filas,
      ;; donde cada fila es un vector de 'ancho' elementos :aire
      (vec (repeat height (vec (repeat width :aire)))))


(defn pintar-celda
      "Devuelve una NUEVA grilla con el 'material' (keyword) colocado en la posición [x y].
      Nota: x es columna, y es fila. En Clojure el acceso es [y x]."
      [grilla x y material]
      (assoc-in grilla [y x] material))


(defn obtener-material
      "Devuelve la keyword del material en la posición [x y].
      Retorna nil si está fuera de los límites."
      [grilla x y]
      (get-in grilla [y x]))