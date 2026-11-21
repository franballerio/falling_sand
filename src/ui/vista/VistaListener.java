package ui.vista;

/**
 * Observador de la vista. La vista notifica acá lo que hace el usuario y no
 * sabe nada del motor de simulación. Todos los métodos son opcionales
 * (default vacío), así desde Clojure con reify se implementan solo los que
 * hagan falta.
 *
 * Los eventos llegan en el hilo de JavaFX: no hay que hacer cálculo pesado
 * acá, solo encolar/actualizar estado y volver.
 */
public interface VistaListener {

    /**
     * El usuario pintó sobre la grilla. Se debe pintar a lo largo del
     * segmento (x0,y0)-(x1,y1) para no dejar huecos al arrastrar rápido.
     * En el primer click, el segmento es un solo punto (x0=x1, y0=y1).
     * Las coordenadas son de celda y pueden caer fuera de la grilla.
     *
     * @param material nombre del material seleccionado (ej. "arena")
     */
    default void alPintar(int x0, int y0, int x1, int y1, int radio, String material) {}

    /** Se eligió otro material en el selector. */
    default void alCambiarMaterial(String material) {}

    /** Se movió el slider de radio. */
    default void alCambiarRadio(int radio) {}

    /** Se apretó Pausa/Reanudar. {@code pausado} es el estado nuevo. */
    default void alAlternarPausa(boolean pausado) {}

    /** Se apretó Paso (solo habilitado en pausa). */
    default void alPedirPaso() {}

    /** Se apretó Limpiar. */
    default void alLimpiar() {}
}
