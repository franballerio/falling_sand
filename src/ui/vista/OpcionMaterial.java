package ui.vista;

/**
 * Dato inmutable para armar el selector de materiales: nombre a mostrar y
 * color ARGB (el mismo entero que usa engine.config).
 */
public final class OpcionMaterial {
    private final String nombre;
    private final int colorArgb;

    public OpcionMaterial(String nombre, int colorArgb) {
        this.nombre = nombre;
        this.colorArgb = colorArgb;
    }

    public String nombre() {
        return nombre;
    }

    public int colorArgb() {
        return colorArgb;
    }
}
