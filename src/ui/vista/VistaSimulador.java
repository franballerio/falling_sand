package ui.vista;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Vista del simulador: grilla a la izquierda y panel de controles a la
 * derecha (selector de material, radio, Pausa, Paso, Limpiar, indicadores).
 *
 * Patrón Observer: la vista emite eventos a los {@link VistaListener}
 * registrados. Para mostrar el estado se llama a {@link #mostrar}, que puede
 * invocarse desde cualquier hilo (se pasa al hilo de JavaFX internamente).
 */
public class VistaSimulador extends HBox {

    private static final int RADIO_MIN = 1;
    private static final int RADIO_MAX = 12;
    private static final int RADIO_INICIAL = 3;

    private final int ancho;
    private final int alto;
    private final int tamanoCelda;

    private final List<VistaListener> listeners = new CopyOnWriteArrayList<>();

    private final WritableImage imagen;
    private final ImageView vistaGrilla;

    private final ToggleGroup grupoMateriales = new ToggleGroup();
    private final Slider sliderRadio = new Slider(RADIO_MIN, RADIO_MAX, RADIO_INICIAL);
    private final Button botonPausa = new Button("Pausa");
    private final Button botonPaso = new Button("Paso");
    private final Button botonLimpiar = new Button("Limpiar");
    private final Label etiquetaPaso = new Label("paso: 0");
    private final Label etiquetaParticulas = new Label("partículas: 0");

    private boolean pausado = false;
    private int ultimoX;
    private int ultimoY;

    /**
     * @param ancho       ancho de la grilla en celdas (ej. 160)
     * @param alto        alto de la grilla en celdas (ej. 120)
     * @param tamanoCelda lado de cada celda en píxeles de pantalla (ej. 5)
     * @param materiales  materiales en el orden en que se muestran; el
     *                    primero queda seleccionado
     */
    public VistaSimulador(int ancho, int alto, int tamanoCelda, List<OpcionMaterial> materiales) {
        this.ancho = ancho;
        this.alto = alto;
        this.tamanoCelda = tamanoCelda;

        this.imagen = new WritableImage(ancho, alto);
        this.vistaGrilla = crearVistaGrilla();

        setSpacing(16);
        setPadding(new Insets(16));
        getChildren().addAll(crearContenedorGrilla(), crearPanelControles(materiales));

        conectarEventos();
    }

    // ------------------------------------------------------------------
    // Observadores
    // ------------------------------------------------------------------

    public void agregarListener(VistaListener listener) {
        listeners.add(listener);
    }

    public void quitarListener(VistaListener listener) {
        listeners.remove(listener);
    }

    // ------------------------------------------------------------------
    // API para mostrar estado (thread-safe)
    // ------------------------------------------------------------------

    /**
     * Muestra la grilla y los indicadores. Se puede llamar desde cualquier
     * hilo.
     *
     * @param pixeles    colores ARGB en orden de filas (largo ancho*alto).
     *                   Se copia, así que el llamador puede reutilizar el
     *                   arreglo.
     * @param paso       número de paso actual
     * @param particulas cantidad de celdas que no son aire
     */
    public void mostrar(int[] pixeles, long paso, int particulas) {
        if (pixeles.length != ancho * alto) {
            throw new IllegalArgumentException(
                "Se esperaban " + (ancho * alto) + " píxeles y llegaron " + pixeles.length);
        }
        final int[] copia = pixeles.clone();
        ejecutarEnFx(() -> {
            imagen.getPixelWriter().setPixels(
                0, 0, ancho, alto, PixelFormat.getIntArgbInstance(), copia, 0, ancho);
            etiquetaPaso.setText("paso: " + paso);
            etiquetaParticulas.setText("partículas: " + particulas);
        });
    }

    /** Cambia el estado de pausa desde afuera (ej. al iniciar). */
    public void setPausado(boolean pausado) {
        ejecutarEnFx(() -> aplicarPausa(pausado));
    }

    // ------------------------------------------------------------------
    // Construcción de la interfaz
    // ------------------------------------------------------------------

    private ImageView crearVistaGrilla() {
        ImageView iv = new ImageView(imagen);
        iv.setFitWidth((double) ancho * tamanoCelda);
        iv.setFitHeight((double) alto * tamanoCelda);
        iv.setPreserveRatio(false);
        iv.setSmooth(false); // vecino más cercano: celdas nítidas
        return iv;
    }

    private StackPane crearContenedorGrilla() {
        StackPane contenedor = new StackPane(vistaGrilla);
        contenedor.setStyle("-fx-border-color: #888888; -fx-border-width: 1;");
        contenedor.setMaxSize(
            (double) ancho * tamanoCelda + 2, (double) alto * tamanoCelda + 2);
        contenedor.setMinSize(
            (double) ancho * tamanoCelda + 2, (double) alto * tamanoCelda + 2);
        return contenedor;
    }

    private VBox crearPanelControles(List<OpcionMaterial> materiales) {
        VBox panel = new VBox(6);
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setMinWidth(150);

        panel.getChildren().add(new Label("Material"));
        boolean primero = true;
        for (OpcionMaterial m : materiales) {
            panel.getChildren().add(crearRadioMaterial(m, primero));
            primero = false;
        }

        sliderRadio.setBlockIncrement(1);
        sliderRadio.setMajorTickUnit(1);
        sliderRadio.setMinorTickCount(0);
        sliderRadio.setSnapToTicks(true);
        sliderRadio.setPrefWidth(90);
        HBox filaRadio = new HBox(8, new Label("Radio:"), sliderRadio);
        filaRadio.setAlignment(Pos.CENTER_LEFT);
        panel.getChildren().add(filaRadio);

        botonPaso.setDisable(true); // solo se usa en pausa
        panel.getChildren().addAll(botonPausa, botonPaso, botonLimpiar,
            etiquetaPaso, etiquetaParticulas);
        return panel;
    }

    private RadioButton crearRadioMaterial(OpcionMaterial m, boolean seleccionado) {
        RadioButton rb = new RadioButton(m.nombre());
        rb.setToggleGroup(grupoMateriales);
        rb.setUserData(m.nombre());
        rb.setSelected(seleccionado);
        rb.setGraphic(muestraColor(m.colorArgb()));
        return rb;
    }

    private Rectangle muestraColor(int argb) {
        double a = ((argb >>> 24) & 0xFF) / 255.0;
        double r = ((argb >>> 16) & 0xFF) / 255.0;
        double g = ((argb >>> 8) & 0xFF) / 255.0;
        double b = (argb & 0xFF) / 255.0;
        Rectangle r0 = new Rectangle(10, 10, new Color(r, g, b, a));
        r0.setStroke(Color.GRAY);
        return r0;
    }

    // ------------------------------------------------------------------
    // Eventos de la interfaz -> notificaciones a los observadores
    // ------------------------------------------------------------------

    private void conectarEventos() {
        vistaGrilla.setOnMousePressed(this::alPresionar);
        vistaGrilla.setOnMouseDragged(this::alArrastrar);

        grupoMateriales.selectedToggleProperty().addListener((obs, anterior, nuevo) -> {
            if (nuevo == null) {
                // No permitir "sin selección": se vuelve a marcar el anterior.
                if (anterior != null) {
                    anterior.setSelected(true);
                }
                return;
            }
            String material = (String) nuevo.getUserData();
            for (VistaListener l : listeners) {
                l.alCambiarMaterial(material);
            }
        });

        sliderRadio.valueProperty().addListener((obs, viejo, nuevo) -> {
            int radio = (int) Math.round(nuevo.doubleValue());
            if (radio != (int) Math.round(viejo.doubleValue())) {
                for (VistaListener l : listeners) {
                    l.alCambiarRadio(radio);
                }
            }
        });

        botonPausa.setOnAction(e -> {
            aplicarPausa(!pausado);
            for (VistaListener l : listeners) {
                l.alAlternarPausa(pausado);
            }
        });

        botonPaso.setOnAction(e -> {
            for (VistaListener l : listeners) {
                l.alPedirPaso();
            }
        });

        botonLimpiar.setOnAction(e -> {
            for (VistaListener l : listeners) {
                l.alLimpiar();
            }
        });
    }

    private void alPresionar(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        ultimoX = celdaX(e);
        ultimoY = celdaY(e);
        notificarPintar(ultimoX, ultimoY, ultimoX, ultimoY);
    }

    private void alArrastrar(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        int x = celdaX(e);
        int y = celdaY(e);
        notificarPintar(ultimoX, ultimoY, x, y);
        ultimoX = x;
        ultimoY = y;
    }

    private void notificarPintar(int x0, int y0, int x1, int y1) {
        int radio = (int) Math.round(sliderRadio.getValue());
        String material = materialSeleccionado();
        for (VistaListener l : listeners) {
            l.alPintar(x0, y0, x1, y1, radio, material);
        }
    }

    private String materialSeleccionado() {
        return (String) grupoMateriales.getSelectedToggle().getUserData();
    }

    private int celdaX(MouseEvent e) {
        return (int) Math.floor(e.getX() / tamanoCelda);
    }

    private int celdaY(MouseEvent e) {
        return (int) Math.floor(e.getY() / tamanoCelda);
    }

    private void aplicarPausa(boolean nuevoEstado) {
        pausado = nuevoEstado;
        botonPausa.setText(pausado ? "Reanudar" : "Pausa");
        botonPaso.setDisable(!pausado);
    }

    private static void ejecutarEnFx(Runnable accion) {
        if (Platform.isFxApplicationThread()) {
            accion.run();
        } else {
            Platform.runLater(accion);
        }
    }
}
