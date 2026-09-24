# Proyecto_1_Dep_Galeria_Kotlin - Galería y Editor Multimedia Android (Kotlin Nativo)

Aplicación móvil nativa desarrollada para el sistema operativo Android en **Kotlin**, que permite la visualización, organización y edición multimedia avanzada de imágenes digitales mediante un visor interactivo, gestión de favoritos y un motor de edición integrado.

---

## 1. Nombre del Proyecto
**Proyecto_1_Dep_Galeria_Kotlin** (Galería Interactiva y Editor Multimedia en Android Nativo - Kotlin)

---

## 2. Objetivo General
Diseñar e implementar una aplicación móvil nativa en Android utilizando el lenguaje **Kotlin**, que ofrezca al usuario una solución integral para la gestión y manipulación de fotografías digitales almacenadas en el dispositivo, incorporando funciones de navegación dinámica, filtrado por favoritos, herramientas de edición visual (dibujo a mano alzada con paleta cromática, recorte interactivo por cuadrante y rotación matricial) y almacenamiento persistente en el dispositivo a través de la API `MediaStore`.

---

## 3. Descripción Funcional del Sistema
El sistema proporciona una experiencia de usuario fluida y reactiva distribuida en dos modos principales de interacción:

### A. Modo Galería (Navegación y Administración)
- **Selección e Importación de Fotos:** Utiliza el contrato moderno `ActivityResultContracts.PickMultipleVisualMedia` (Android Photo Picker), permitiendo al usuario seleccionar una o varias imágenes de su dispositivo de forma segura y sin requerir permisos invasivos de almacenamiento global en versiones recientes.
- **Navegación Intuitiva:** Visor fotográfico central basado en `MaterialCardView` e `ImageView` (`fitCenter`), complementado con botones flotantes circulares para navegar hacia adelante y hacia atrás entre las fotos cargadas.
- **Sistema de Favoritos y Filtros:** Cada fotografía puede ser marcada o desmarcada como favorita en tiempo real. Un ícono flotante sobre la tarjeta y un botón en el panel inferior reflejan el estado del elemento. La barra de herramientas superior (`MaterialToolbar`) incluye un botón de filtro que conmuta la visualización entre el catálogo completo y exclusivamente las imágenes favoritas.
- **Eliminación Dinámica:** Permite remover la imagen actualmente en pantalla de la lista en memoria, recalculando y sincronizando el índice de visualización de manera automática.

### B. Modo Edición Multimedia
Al presionar el botón "Editar", la interfaz realiza una transición fluida al panel de herramientas de edición:
- **Pincel y Dibujo Libre (`DrawingView`):** Permite dibujar trazos continuos directamente sobre la imagen utilizando el dedo o puntero táctil, ofreciendo una paleta de 6 colores predefinidos (rojo, azul, verde, amarillo, negro y blanco).
- **Deshacer Trazos (`Undo`):** Permite revertir de forma secuencial los últimos trazos realizados en el lienzo sin perder el trabajo previo.
- **Herramienta de Recorte Interactivo (`Crop Tool`):** Despliega una capa translúcida oscurecida con marco delimitador y 4 anclas táctiles manipulables para ajustar dimensiones y proporciones de recorte, así como traslación completa del área seleccionada.
- **Rotación Matricial:** Permite rotar la imagen en incrementos de 90° en sentido horario (`postRotate(90f)`) adaptando el lienzo en tiempo real.
- **Guardado y Exportación en Alta Calidad:** Comprime y fusiona el bitmap original con los trazos vectoriales en un archivo JPEG con 95% de compresión y lo almacena en la ruta de almacenamiento público `Pictures/MiGaleria` mediante `MediaStore`.

---

## 4. Módulos Desarrollados

### 1. Módulo Principal y Controlador de Navegación (`MainActivity.kt`)
- Administra el ciclo de vida de la aplicación, inicialización de vistas y asignación de eventos (Listeners).
- Controla las colecciones en memoria (`fotos: MutableList<Foto>`), el puntero de posición (`indice`) y el estado del filtro (`soloFav`).
- Coordina la alternancia entre el modo visualización y el modo edición, controlando la visibilidad de componentes.

### 2. Módulo de Lienzo y Renderizado Táctil (`DrawingView`)
- Clase personalizada que extiende de `android.view.View`.
- Implementa `onTouchEvent` para la captura de coordenadas de movimiento, optimizada con interpolación cuadrática (`Path.quadTo`) para obtener trazos vectoriales suaves.
- Mantiene la pila de objetos `Trazo(val path: Path, val color: Int, val width: Float)`.
- Gestiona la visualización del visor de recorte (`cropRect`), los manejadores de esquina (`handles`) y el enmascarado oscuro exterior.

### 3. Módulo de Transformación de Imágenes y Algoritmos Gráficos
- **Mapeo de Escala de Pantalla a Bitmap:** Algoritmo que calcula el factor de escala uniforme `s = minOf(viewWidth / bitmapWidth, viewHeight / bitmapHeight)` y los márgenes de centrado (`dx`, `dy`) generados por el modo de ajuste `fitCenter`.
- **Motor de Recorte Matricial:** Conversión bidireccional de coordenadas de pantalla a coordenadas reales de píxeles del mapa de bits para extraer sub-bitmaps (`Bitmap.createBitmap(b, x, y, bw, bh)`).
- **Rotación Matricial:** Aplicación de transformaciones geométricas afines mediante `android.graphics.Matrix`.

### 4. Módulo de Almacenamiento y Persistencia (`MediaStore Engine`)
- Manejo de metadatos de medios (`ContentValues`, `MIME_TYPE`, `DISPLAY_NAME`, `RELATIVE_PATH`).
- Integración con `ContentResolver` para escribir flujos binarios de salida (`openOutputStream`) sin violar las directivas de Scoped Storage.

### 5. Módulo de Interfaz de Usuario y Recursos (`XML Layouts & Drawables`)
- **Diseño Responsivo:** Implementado en `activity_main.xml` con `ConstraintLayout` y componentes de Material Design 3 (`MaterialToolbar`, `MaterialCardView`, `MaterialButton`).
- **Recursos Gráficos Vectoriales:** Íconos de favoritos (`ic_heart_filled`, `ic_heart_outline`), fondos circulares con estados y paleta de colores en `colors.xml`.

---

## 5. Tecnologías Empleadas

| Categoría | Tecnología / Herramienta | Descripción / Versión |
| :--- | :--- | :--- |
| **Lenguaje de Programación** | Kotlin | Lenguaje moderno para desarrollo nativo Android |
| **Plataforma Objetivo** | Android SDK | Min SDK: 23 (Android 6.0 Marshmallow)<br>Target & Compile SDK: 37 |
| **Entorno de Desarrollo (IDE)**| Android Studio | IDE oficial con emuladores y analizador de APK |
| **Sistema de Compilación** | Gradle (Kotlin DSL) | Configuración moderna con `build.gradle.kts` y catálogo de versiones |
| **Librerías UI / UX** | Material Components (M3) | `com.google.android.material:material` |
| **Componentes de Arquitectura** | AndroidX Core KTX / AppCompat | Extensiones idiomáticas de Kotlin para APIs de Android |
| **Contratos de Actividad** | ActivityResultContracts | `PickMultipleVisualMedia`, `RequestMultiplePermissions` |
| **Motor Gráfico 2D** | Android Graphics Library | `android.graphics.Canvas`, `Paint`, `Path`, `Bitmap`, `Matrix`, `RectF` |
| **Gestor de Almacenamiento** | Android MediaStore API | Acceso conforme a Scoped Storage (`MediaStore.Images.Media`) |
| **Control de Versiones** | Git & GitHub | Control de versiones distribuido |

---

## 6. Explicación de la Arquitectura General
El proyecto sigue una arquitectura **Model-View-Controller (MVC) / Single Activity Architecture** adaptada a las directrices de Android:

```mermaid
flowchart TD
    subgraph UI_Layer ["Capa de Presentación (UI)"]
        A[MainActivity - Controlador UI]
        B[activity_main.xml - Layout Material Design 3]
        C[DrawingView - Custom View para Dibujo y Recorte]
    end

    subgraph Data_Layer ["Capa de Datos y Estado"]
        D["Foto(uri, esFav)"]
        E["DrawingView.Trazo(path, color, width)"]
        F["Lista en memoria: fotos (MutableList)"]
    end

    subgraph System_Layer ["Capa de Servicios de Android"]
        G[Photo Picker API]
        H[ContentResolver & MediaStore]
        I[Android Graphics Engine Canvas / Bitmap]
    end

    A --> B
    A --> C
    A --> D
    A --> F
    C --> E
    A --> G
    A --> H
    A --> I
```

- **Separación de Responsabilidades:**
  - **Estado y Modelo:** Representado por la clase de datos `Foto`, que encapsula la URI del recurso multimedia y su bandera booleana `esFav`.
  - **Vista Personalizada (`DrawingView`):** Aísla por completo la complejidad del procesamiento de eventos táctiles, la geometría de recorte y la acumulación de trazos vectoriales, evitando saturar la actividad principal.
  - **Controlador (`MainActivity`):** Actúa como orquestador, escuchando eventos de usuario, alternando paneles según el estado de la aplicación y coordinando la persistencia.
  - **Transformación Aislada:** La edición no altera la imagen original en el almacenamiento hasta que el usuario confirma la acción mediante el botón "Guardar", momento en el cual se genera un nuevo archivo derivado sin pérdida de la fuente original.

---

## 7. Dificultades Encontradas y Soluciones Implementadas

### Dificultad 1: Mapeo de Coordenadas entre la Pantalla y la Resolución Real del Bitmap
- **Problema:** Debido a que el `ImageView` utiliza `scaleType="fitCenter"`, la imagen se escala proporcionalmente dentro del contenedor, dejando márgenes vacíos (letterboxing o pillarboxing). Si se dibujaba o recortaba utilizando las coordenadas `(x, y)` brutas del evento táctil, las modificaciones quedaban desplazadas o deformadas al guardarse en el bitmap original.
- **Solución Implementada:** Se formuló un algoritmo que calcula el factor de escala `s = minOf(viewWidth / bitmapWidth, viewHeight / bitmapHeight)` y los desfases horizontales y verticales `dx = (viewWidth - bitmapWidth * s) / 2f`, `dy = (viewHeight - bitmapHeight * s) / 2f`. Al aplicar el recorte o dibujar en el guardado, se aplica la matriz inversa `(x - dx) / s` para proyectar con exactitud los puntos de la pantalla sobre el mapa de bits a su resolución nativa.

### Dificultad 2: Manejo de Memoria y Prevención de Excepciones `OutOfMemoryError` (OOM)
- **Problema:** La manipulación simultánea de imágenes de alta resolución (como fotos de cámara tomadas a 12 MP o superiores) decodificadas como mapas de bits mutables (`ARGB_8888`) satura rápidamente la memoria RAM asignada al proceso de la aplicación.
- **Solución Implementada:** Se incorporó el reciclado explícito del bitmap anterior (`b.recycle()`) al aplicar operaciones destructivas (como rotación o recorte), se liberaron recursos en el callback `onDestroy()` de la actividad, y se utilizaron bloques `use { ... }` para garantizar el cierre seguro de descriptores de archivos y flujos de entrada/salida.

### Dificultad 3: Fragmentación de Permisos y Compatibilidad con Scoped Storage
- **Problema:** En versiones modernas de Android (Android 10+, Android 13+ con API 33), las aplicaciones ya no pueden utilizar rutas de archivo directas tipo `java.io.File("/sdcard/...")` ni solicitar `WRITE_EXTERNAL_STORAGE` de forma tradicional.
- **Solución Implementada:** Se adoptó el nuevo **Photo Picker** (`PickMultipleVisualMedia`) que funciona sin requerir permisos especiales de lectura para la selección del usuario. Para el guardado, se implementó la inserción mediante `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` definiendo el campo `RELATIVE_PATH` como `Pictures/MiGaleria`, cumpliendo al 100% con los estándares de seguridad de Android.

### Dificultad 4: Fluidez y Trazado Natural en el Dibujo Táctil
- **Problema:** Conectar los puntos táctiles con líneas rectas simples (`lineTo`) provocaba que los trazos rápidos tuvieran un aspecto poligonal y dentado.
- **Solución Implementada:** Se utilizó interpolación por curvas de Bézier cuadráticas (`Path.quadTo`), calculando el punto intermedio entre la coordenada previa y la actual: `((x + lastX) / 2f, (y + lastY) / 2f)`. Esto garantizó un trazo continuo, redondeado y con sensación visual orgánica.

---

## 8. Conclusiones
1. **Desarrollo Nativo Robusto:** La utilización de Kotlin junto con AndroidX y Material 3 permitió construir una aplicación ágil, responsiva y con una interfaz moderna y coherente con las guías de diseño de Android.
2. **Modularidad y Separación de Lógica Gráfica:** La encapsulación de la lógica de dibujo y recorte interactivo en un componente personalizado (`DrawingView`) facilitó el mantenimiento del código y evitó el sobrecargamiento de la actividad principal.
3. **Compatibilidad y Buenas Prácticas del Sistema:** La adopción del Photo Picker y la API `MediaStore` garantiza que la aplicación sea totalmente compatible tanto con versiones anteriores de Android como con las versiones más recientes (Android 13, 14 y superiores), protegiendo la privacidad del usuario y cumpliendo con Scoped Storage.
4. **Experiencia de Usuario Integral:** Se logró un flujo de trabajo intuitivo donde el usuario puede explorar su colección, filtrar sus elementos favoritos, transformar y personalizar sus fotos y guardar los resultados sin fricción ni pérdida de calidad.
