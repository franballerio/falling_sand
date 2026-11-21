# Falling Sand

TP2 Paradigmas de Programación - Facultad de Ingeniería de la Universidad de Buenos Aires

Docentes: Diego Essaya y Santiago Maraggi
Corrector: Macarena Vita Sanchez
Integrantes del grupo: Lautaro Tomas Soria, padron: 114698
Francisco Oscar Ballerio, padron: 114777
Nombre del proyecto: Falling Sand

## Videos
https://drive.google.com/drive/u/0/folders/1K60_XMP0txX_CKFouoMwiw-9Va3ADsh9
https://drive.google.com/file/d/1Xa6H-EDzuvGjsQaoEwwiS7UWd8rAEicE/view?usp=sharing

## Descripción

Un simulador de materiales tipo *falling sand*. El mundo es una grilla de 160 × 120
celdas y cada celda contiene un material: la arena cae, el agua fluye, el vapor sube,
la lava es viscosa y los materiales reaccionan al tocarse. El usuario dibuja con el
mouse y observa la evolución en tiempo real.

El motor (`src/engine`) es Clojure puro e inmutable: no conoce JavaFX y avanza el
mundo con funciones `estado -> estado`. La interfaz (`src/ui`) es JavaFX y solo
intercambia un `int[]` de píxeles y dos contadores con el motor.

## Requisitos

- JDK 21 o superior.
- Leiningen 2.x.

JavaFX 21 llega como dependencia declarada en `project.clj`, así que no hace falta
instalarlo aparte.

## Ejecución

```
lein run
```

Se abre una ventana con la grilla a la izquierda y el panel de control a la derecha.

## Controles

| Control | Qué hace |
| --- | --- |
| Selector **Material** | Elige el material que se pinta. Cada opción muestra su color. |
| **Radio** (1 a 12) | Radio del disco que se estampa al pintar. Arranca en 3. |
| **Pausa** / **Reanudar** | Congela o reanuda la simulación. El botón cambia de nombre. |
| **Paso** | Avanza exactamente un paso. Está deshabilitado mientras la simulación corre. |
| **Limpiar** | Vacía la grilla. Conserva el número de paso y las dimensiones. |
| Clic | Pinta un disco del material y radio elegidos. |
| Arrastrar | Pinta un trazo continuo: los puntos se unen con discos para que un movimiento rápido no deje huecos. |

En el panel derecho se muestran dos indicadores: el número de **paso** y la cantidad
de **partículas** (celdas con un material distinto de aire).

## Reglas de la simulación

### Partición en bloques

En cada paso la grilla se parte en bloques de **2 × 2** celdas. La partición no está
fija: el origen se desplaza en un ciclo de cuatro pasos,

```
paso mod 4 = 0 -> [0 0]   1 -> [1 1]   2 -> [1 0]   3 -> [0 1]
```

de modo que cada celda termina emparejada con vecinos distintos y el movimiento puede
propagarse en cualquier dirección. Las celdas que quedan fuera de todo bloque no
cambian en ese paso.

Dentro de un bloque, las cuatro posiciones son:

```
0 = (x, y)      1 = (x+1, y)
2 = (x, y+1)    3 = (x+1, y+1)
```

### Fase

Cada bloque tiene un número de **fase** que combina el paso y su posición en la grilla:

```
fase = (paso div 4) + 3·bx + 5·by
```

Todas las decisiones temporales se derivan de ahí, sin guardar contadores: una
decisión de período `N` con constante `k` se cumple si y solo si

```
(fase + 7·k) mod N = 0
```

Cada regla tiene su propio `k`, elegido para que las decisiones no coincidan entre sí.

### Las tres etapas de un bloque

Cada bloque se procesa en tres etapas encadenadas, y cada una ve el resultado de la
anterior:

1. **Decaimiento** — cada posición transforma su material si está en su fase. Usa
   `k` igual al número de posición (0 a 3). Vapor → agua (N = 100), fuego → aire (N = 10).
2. **Reacciones** — se evalúan los seis pares de posiciones en orden
   `(0,1) (2,3) (0,2) (1,3) (0,3) (1,2)`, con `k = 4 + q` donde `q` es el orden del par.
   Se usan los materiales **actuales** del bloque, así que una reacción en cadena
   funciona dentro del mismo bloque.
3. **Movimiento** — primero se calcula el permiso de cada posición (`k = 10 + i`,
   período igual al del material, o 1 si no define uno; nunca para materiales fijos).
   El permiso pertenece a la **posición**, no al material, y no se recalcula tras un
   intercambio. Después: intercambios verticales, luego diagonales (decisión con
   `k = 14`, período 2, en un orden que también depende de la fase) y por último
   deslizamientos horizontales (`k = 15` en la fila superior, `k = 16` en la inferior,
   período 2).

Un intercambio exige que **ambas** posiciones tengan permiso y que la densidad del
material de origen sea estrictamente mayor. Por eso un material fijo nunca se mueve y
nadie lo puede atravesar: es lo que frena a la lava contra una sola celda de piedra.

Los gases se deslizan en cualquiera de las dos filas del bloque; los líquidos solo en
la inferior. Polvos y fijos no se deslizan.

### Reacciones

| Par | Resultado | N |
| --- | --- | --- |
| fuego + agua | fuego → aire, agua → vapor | 2 |
| agua + lava | agua → vapor, lava → piedra | 2 |
| arena + lava | arena → vidrio, lava → lava | 20 |
| fuego + madera | fuego → fuego, madera → fuego | 20 |
| lava + madera | lava → lava, madera → fuego | 5 |

## Materiales

| Material | Símbolo | Color | Tipo | Densidad | Extra |
| --- | --- | --- | --- | --- | --- |
| aire | `.` | #000000 | vacío | 0 | |
| pared | `#` | #808080 | fijo | 0 | |
| piedra | `p` | #555560 | fijo | 0 | |
| vidrio | `v` | #9EDCE8 | fijo | 0 | |
| madera | `m` | #8B5A2B | fijo | 0 | |
| obsidiana | `o` | #1E002E | fijo | 0 | extensión propia, ver notas |
| arena | `a` | #E0C068 | polvo | 4 | |
| agua | `g` | #2060E0 | líquido | 2 | |
| lava | `l` | #E85010 | líquido | 3 | período de movimiento 3 |
| vapor | `^` | #C8C8D8 | gas | −1 | decae a agua (N = 100) |
| fuego | `f` | #FFC020 | gas | −2 | decae a aire (N = 10) |

## Arquitectura

| Namespace / clase | Responsabilidad |
| --- | --- |
| `engine.config` | Única fuente de verdad: tabla de materiales, tabla de reacciones y dimensiones. |
| `engine.grid` | Grilla como vector de vectores y sus tres primitivas: crear, pintar, leer. |
| `engine.rules` | Las tres etapas de la regla de bloque. Funciones puras sobre un bloque de cuatro celdas. |
| `engine.state` | El mundo: partición, fase, `aplicar-paso`, conteo de partículas, pintado. |
| `ui.app` | Arranque de JavaFX, conversión a píxeles y el hilo de simulación. |
| `ui.vista.VistaSimulador` | Ventana, panel de control y dibujado de la grilla. |
| `ui.vista.VistaListener` | Interfaz de observador con métodos por defecto; `ui.app` la implementa con `reify`. |

### Concurrencia

Hay tres ejecuciones de un solo dueño conectadas por colas, nunca por memoria
compartida:

- El **hilo de eventos de JavaFX** es dueño de los controles y del panel.
- Un **hilo de simulación** propio es el único que toca el estado del mundo: consume
  una cola de comandos `estado -> estado` y publica el resultado.
- El **hilo de render de JavaFX** recibe los píxeles con `Platform.runLater`.

Adentro de un paso, los bloques se reparten entre hilos: se agrupan por fila de bloque
(`by`), de modo que cada tarea escribe únicamente las filas `oy + 2·by` y
`oy + 2·by + 1`, disjuntas entre tareas. Cada tarea lee siempre la grilla del paso
anterior, arma sus filas con transients propios y devuelve datos inmutables; el
ensamblado final es por índice de fila, no por orden de terminación, así que el
resultado es determinístico.

Con menos de 64 bloques, o con un solo hilo disponible, se usa la vía secuencial y no
se crea ninguna tarea.

## Tests

```
lein test
```

38 tests con 1382 aserciones. Cubren, entre otras cosas:

- La tabla de ejemplos de la regla de bloque del enunciado, y cada reacción en sus
  fases de corte.
- Caída, hundimiento por densidad, deslizamiento y reacción de cada material.
- Casos de borde: grilla vacía, grilla más chica que un bloque, ancho impar.
- **Independencia de orden**: invertir el orden de los bloques produce la misma
  grilla. Es la propiedad que habilita paralelizar el paso, y hay un test aparte que
  comprueba que ese test detecte una regla que escriba fuera de su bloque.
- **Equivalencia paralelo/secuencial** en ocho tamaños por cuatro desplazamientos,
  contra una referencia armada solo con la API pública del motor, más determinismo y
  conservación de las celdas sin bloque.

## Rendimiento

Con la grilla llena (160 × 120, unas 16.000 partículas), medido en una máquina de 12
núcleos:

| Implementación | ms por paso |
| --- | --- |
| Secuencial escribiendo la grilla con `assoc-in` | ~93 |
| Secuencial escribiendo filas con transients | ~80 |
| Paralela, 11 hilos | **~17** |

Las tres se midieron para separar las causas: los transients aportan 1,16× y los hilos
4,7×. El objetivo de ritmo es un paso cada 16 ms (`ms-por-paso` en `ui/app.clj`), así
que a densidad máxima la simulación queda en el límite y a densidad normal sobra
tiempo.

Costo por etapa del trabajo de reglas: reacciones ~49 %, movimiento ~31 %,
decaimiento ~16 %, lectura de bloques ~8 %. Los números absolutos son de la máquina de
desarrollo; lo que se traslada entre máquinas es el cociente.

## Notas

- **La obsidiana es una extensión propia**: el enunciado no la define. Es un material
  fijo e inerte, se puede pintar desde el selector y no decae ni reacciona. La reacción
  agua + lava produce **piedra**, como pide el enunciado.
- `piedra` y `obsidiana` son indistinguibles en comportamiento: ambas son fijas con
  densidad 0, así que cualquiera de las dos frena igual a la lava.
