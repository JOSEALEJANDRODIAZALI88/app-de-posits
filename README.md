# Mis Pósits

**Recordatorios visibles sobre la pantalla de inicio original de tu Android.**

Mis Pósits es una aplicación nativa de notas adhesivas pensada para que los pendientes importantes estén a la vista cada vez que vuelves al inicio del teléfono. Puedes escribir tus recordatorios, elegir un color y colocarlos libremente sobre tu fondo y tus iconos habituales.

La aplicación conserva el launcher del dispositivo. Utiliza ventanas superpuestas y detecta cuándo el inicio está en primer plano para mostrar los pósits; al detectar otra aplicación, los oculta. No necesita widgets, cuenta de usuario ni conexión a Internet.

> **Versión actual: 2.0.** La aparición y ocultación dependen de los eventos de uso que entrega Android: puede haber un pequeño retraso. Consulta [Limitaciones](#limitaciones) para conocer el comportamiento en distintos launchers.

## Contenido

- [Qué puedes hacer](#qué-puedes-hacer)
- [Requisitos](#requisitos)
- [Abrir el proyecto en Android Studio](#abrir-el-proyecto-en-android-studio)
- [Compilar e instalar el APK](#compilar-e-instalar-el-apk)
- [Primer uso](#primer-uso)
- [Permisos](#permisos)
- [Cómo funciona](#cómo-funciona)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Datos y privacidad](#datos-y-privacidad)
- [Pruebas y comprobaciones](#pruebas-y-comprobaciones)
- [Limitaciones](#limitaciones)
- [Solución de problemas](#solución-de-problemas)
- [Desarrollo y contribuciones](#desarrollo-y-contribuciones)

## Qué puedes hacer

| Función | Descripción |
| --- | --- |
| Crear y editar | Escribe un título y el contenido de cada recordatorio. |
| Personalizar | Elige entre cinco colores, texto de 16 a 32 sp y papel normal o ancho. |
| Mover libremente | Arrastra la cabecera **MOVER** sin una cuadrícula fija. |
| Mantener la posición | La ubicación se guarda al soltar la nota y se adapta al tamaño de pantalla. |
| Superponer notas | Usa varias notas a la vez; la nota movida queda delante al guardarse. |
| Plegar y expandir | Usa **- / +** para reducir o desplegar el papel. |
| Retirar sin borrar | Quita un pósit del inicio y consérvalo en la lista de notas. |
| Eliminar | Borra una nota después de confirmar. |
| Controlar la actividad | Activa los pósits desde la app y detén el servicio desde la app o la notificación. |
| Conservar tus datos | Las notas se guardan localmente entre sesiones y actualizaciones compatibles. |

## Requisitos

### Para utilizar la aplicación

- Android **8.0 / API 26** o posterior.
- Una pantalla de inicio seleccionada en los ajustes de Android.
- Permisos de **mostrar sobre otras aplicaciones** y **acceso al uso de apps**.
- En Android 13 o superior, se recomienda permitir las notificaciones para acceder al control de detención.

### Para desarrollar y compilar

| Componente | Versión/configuración |
| --- | --- |
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose y Material 3 |
| Android Gradle Plugin | 9.3.2 |
| Gradle Wrapper | 9.5.0, incluido en el repositorio |
| JDK del daemon de Gradle | **25**, configurado en `gradle/gradle-daemon-jvm.properties` |
| Compatibilidad del código Java | 17 |
| SDK de compilación | 36 |
| SDK objetivo | 35 |
| SDK mínimo | 26 |
| Pruebas unitarias | JUnit 4 |

Utiliza una versión de Android Studio compatible con el Android Gradle Plugin indicado. La primera sincronización necesita conexión para descargar las herramientas y dependencias.

## Abrir el proyecto en Android Studio

1. Clona este repositorio:

   ```bash
   git clone https://github.com/JOSEALEJANDRODIAZALI88/app-de-posits.git
   ```

2. En Android Studio, selecciona **Open** y abre la carpeta `app-de-posits`, que contiene `settings.gradle.kts`.
3. Instala **Android SDK Platform 36** desde **SDK Manager** si no está disponible.
4. Espera a que termine la sincronización de Gradle. El daemon está fijado a Java 25; cambiar únicamente `JAVA_HOME` no sustituye esta configuración.
5. Selecciona un dispositivo físico con depuración USB o un emulador con API 26 o superior.
6. Ejecuta el módulo **app** con **Run**.

Android Studio debe crear `local.properties` con la ruta del SDK de tu equipo. Ese archivo es local y se excluye del repositorio.

## Compilar e instalar el APK

Ejecuta los comandos desde la raíz del proyecto.

### Windows / PowerShell

```powershell
.\gradlew.bat :app:assembleDebug
```

### Linux / macOS

```bash
./gradlew :app:assembleDebug
```

El APK de depuración se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Para instalarlo con un dispositivo conectado y `adb` disponible:

```bash
adb install -r "app/build/outputs/apk/debug/app-debug.apk"
```

También puedes copiar el APK al teléfono y abrirlo para instalarlo. Android puede solicitar permiso para instalar aplicaciones desde la fuente utilizada.

El repositorio contiene el código y el wrapper necesarios para compilar. Las carpetas de compilación y los APK generados se excluyen de Git. El APK de depuración sirve para pruebas e instalación directa; una distribución firmada requiere configurar tu propia firma fuera del repositorio.

## Primer uso

1. Abre **Mis Pósits** y pulsa **+ Nuevo pósit**.
2. Escribe el título o el contenido, elige el papel y guarda la nota para pegarla al inicio.
3. En la tarjeta de configuración, entra en **Mostrar sobre otras apps** y autoriza a **Mis Pósits**.
4. Abre **Acceso al uso de apps** y habilita el acceso para **Mis Pósits**.
5. Vuelve a la app y pulsa **Activar en mi inicio**.
6. Si Android solicita permiso de notificaciones, puedes permitirlo para tener el botón **Detener** a mano.
7. Pulsa el botón o gesto **Inicio**, o **Ir a mi pantalla de inicio** dentro de la app.

En el inicio, arrastra la cabecera para mover una nota. Toca el papel o la cabecera sin arrastrar para editarla. El botón de cierre la retira del inicio sin borrarla; podrás volver a pegarla desde la aplicación.

### Actualizar desde la versión anterior

Se conserva el identificador `com.notaflotante.app` y el almacenamiento original. Puedes actualizar sin desinstalar si las dos versiones están firmadas con la misma clave. Las compilaciones de depuración generadas en equipos distintos pueden usar claves distintas.

Si tenías la versión anterior de Mis Pósits como launcher, esta versión deja de ofrecerse como inicio. Si Android no restaura automáticamente el anterior, pulsa **Elegir inicio del teléfono** y selecciona el launcher del fabricante.

## Permisos

| Permiso | Para qué se utiliza | Cómo se habilita |
| --- | --- | --- |
| `SYSTEM_ALERT_WINDOW` | Crear las ventanas de los pósits sobre el inicio. | Ajustes de mostrar sobre otras apps. |
| `PACKAGE_USAGE_STATS` | Detectar qué aplicación está en primer plano y compararla con el inicio seleccionado. | Ajustes de acceso al uso de apps. No es un permiso de instalación automático. |
| `POST_NOTIFICATIONS` | Mostrar la notificación de control en Android 13 o superior. | Solicitud de notificaciones de Android. |
| `FOREGROUND_SERVICE` | Mantener la detección activa mientras el usuario la utiliza. | Declaración del servicio. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Identificar el caso de uso del servicio en versiones recientes de Android. | Declaración del servicio y de su subtipo. |

La app no utiliza un servicio de accesibilidad ni solicita permiso de Internet. Si se revoca el permiso de superposición o el acceso de uso, se quitan las ventanas y se detiene el servicio.

## Cómo funciona

```text
Usuario activa los pósits
        |
        v
HomeNotesService inicia la notificación de control
        |
        v
HomeDetection consulta eventos de uso y el inicio seleccionado
        |
        v
ForegroundTracker identifica la aplicación en primer plano
        |
        +-- Inicio confirmado + pantalla desbloqueada --> Mostrar notas ancladas
        |
        +-- Otra app / editor / bloqueo / estado desconocido --> Ocultar notas
```

### Detección del inicio

`HomeDetection` consulta `UsageStatsManager` fuera del hilo de interfaz. Compara el paquete de la actividad en primer plano con la aplicación que resuelve la acción `MAIN` y la categoría `HOME`. No sustituye esa aplicación.

`ForegroundTracker` procesa eventos de reanudación, pausa y detención. Los eventos antiguos no pueden sustituir un estado más reciente y el estado desconocido no habilita las notas.

La comprobación se realiza aproximadamente cada **250 ms** con la pantalla encendida y cada **1 segundo** cuando no está activa. La identificación del inicio seleccionado se actualiza periódicamente para detectar un cambio de launcher.

### Ventanas y gestos

`HomeNotesService` crea una ventana `TYPE_APPLICATION_OVERLAY` por nota visible, mediante `WindowManager`. Las ventanas no reciben foco de teclado y los toques fuera de ellas siguen llegando a la pantalla de inicio.

Las posiciones `boardX` y `boardY` se guardan como fracciones entre 0 y 1 del **recorrido disponible**: tamaño de pantalla utilizable menos tamaño del pósit. Así se mantienen dentro de los límites y se adaptan al cambiar la orientación o el tamaño del papel. La posición se persiste al terminar el arrastre.

El servicio se inicia por una acción del usuario y devuelve `START_NOT_STICKY`. Tras reiniciar el dispositivo, forzar la detención o perder un permiso, debes abrir la app y activar los pósits de nuevo.

## Estructura del proyecto

```text
app-de-posits/
├-- app/
|   ├-- build.gradle.kts
|   └-- src/
|       ├-- main/
|       |   ├-- AndroidManifest.xml
|       |   ├-- java/com/notaflotante/app/
|       |   |   ├-- MainActivity.kt
|       |   |   ├-- HomeDetection.kt
|       |   |   ├-- ForegroundTracker.kt
|       |   |   ├-- HomeNotesService.kt
|       |   |   ├-- NotesStore.kt
|       |   |   └-- PaperUi.kt
|       |   └-- res/
|       └-- test/java/com/notaflotante/app/
|           └-- ForegroundTrackerTest.kt
├-- gradle/
|   ├-- gradle-daemon-jvm.properties
|   └-- wrapper/
├-- build.gradle.kts
├-- settings.gradle.kts
├-- gradle.properties
├-- gradlew
├-- gradlew.bat
└-- README.md
```

| Archivo | Responsabilidad |
| --- | --- |
| `MainActivity.kt` | Lista de notas, flujo de permisos, editor y controles de activación/detención. |
| `HomeDetection.kt` | Validación del acceso de uso, consulta de eventos y resolución del inicio actual. |
| `ForegroundTracker.kt` | Estado de la actividad en primer plano y rechazo de eventos atrasados. |
| `HomeNotesService.kt` | Servicio, notificación, visibilidad, ventanas, movimientos y plegado. |
| `NotesStore.kt` | Modelo `Note`, aplicación `NotesApp`, estado observable y almacenamiento JSON. |
| `PaperUi.kt` | Apariencia del papel, componentes y editor Compose. |
| `ForegroundTrackerTest.kt` | Pruebas unitarias del estado de detección. |

## Datos y privacidad

Las notas se almacenan en preferencias privadas del dispositivo: archivo `sticky_notes`, clave `notes`, con una lista JSON. No se requiere cuenta, servidor ni base de datos remota.

Cada nota conserva su identificador, título, contenido, índice de color, tamaño de texto, ancho, estado visible/guardado, estado plegado y posición normalizada. Los campos antiguos `x` e `y` se mantienen por compatibilidad; la ubicación actual usa `boardX` y `boardY`.

Los eventos de uso se consultan en memoria para decidir la visibilidad. La app no guarda un historial de aplicaciones ni lo envía a un servidor.

Las notas sobreviven al cierre y a actualizaciones compatibles. Borrar los datos de la aplicación o desinstalarla puede eliminarlas. Esta versión no incluye exportación ni sincronización de notas.

## Pruebas y comprobaciones

Ejecutar compilación, análisis de Android Lint y pruebas unitarias:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

En Linux/macOS, sustituye `.\gradlew.bat` por `./gradlew`.

Las cinco pruebas unitarias verifican:

1. Ocultación en estado desconocido o al pausarse el inicio.
2. Cambio a otra aplicación y retorno al inicio.
3. Rechazo de eventos antiguos frente a un estado más reciente.
4. Pausa de una actividad anterior sin invalidar otra actividad más reciente del mismo paquete.
5. Rechazo de un inicio no resuelto o de la propia app como inicio.

### Comprobaciones realizadas en Android 15 / Pixel 5 emulado

- Instalación sobre la versión anterior y conservación de las notas.
- Uso del inicio original después de actualizar.
- Habilitación de los permisos desde las pantallas reales de Ajustes.
- Visibilidad de los pósits en el inicio, ocultación al abrir Reloj y reaparición al volver.
- Arrastre y persistencia de posición, expansión y apertura del editor desde un pósit.
- Ocultación con la pantalla apagada y recuperación al desbloquear y volver a Inicio.
- Retirada de ventanas y detención al revocar el acceso de uso.
- Detención desde la app y prueba de instalación del APK final.

La compilación y las cinco pruebas unitarias finalizaron correctamente. Android Lint informó **0 errores** y sugerencias de mantenimiento y accesibilidad. Estas comprobaciones no sustituyen las pruebas en dispositivos físicos de diferentes fabricantes.

## Limitaciones

- **No es una integración interna del sistema:** se utilizan las APIs públicas de superposición y acceso de uso.
- **Las transiciones no son instantáneas:** la entrega de eventos depende de Android y del fabricante, además del intervalo de consulta.
- **Escritorio, cajón y Recientes pueden compartir actividad:** algunos launchers no permiten distinguir esas superficies con el acceso de uso. Los pósits pueden verse también en ellas.
- **Pantallas protegidas:** Android y ciertas aplicaciones pueden bloquear las superposiciones.
- **Ahorro de batería:** el sistema puede cerrar el servicio. En ese caso, vuelve a activarlo desde la app.
- **Sin reactivación automática tras reiniciar:** el usuario vuelve a activar el servicio cuando lo necesite.

## Solución de problemas

| Problema | Qué revisar |
| --- | --- |
| No se habilita Activar | Crea o pega al inicio al menos una nota y comprueba los dos permisos especiales. |
| Las notas no aparecen en el inicio | Comprueba el inicio predeterminado, vuelve a activar el servicio y espera a que Android entregue los eventos. |
| Android abre una lista al solicitar superposición | Busca **Mis Pósits** en esa lista y habilita el permiso. |
| Las notas tardan en ocultarse | Existe una latencia del sistema; revisa también las restricciones de actividad en segundo plano del fabricante. |
| Se detuvo después de quitar un permiso | Restablece el permiso y pulsa **Activar en mi inicio**. |
| No aparece la notificación | Revisa el permiso de notificaciones en Android 13 o superior. Puedes detener los pósits desde la app. |
| Gradle no encuentra el SDK | Instala Platform 36 y deja que Android Studio configure `local.properties`. |
| Gradle utiliza otro Java | Revisa `gradle/gradle-daemon-jvm.properties`: el daemon requiere Java 25. |
| No se instala una actualización | Comprueba que se haya firmado con la misma clave que la instalación existente. |

## Desarrollo y contribuciones

Para reportar un problema, indica la versión de Android, fabricante, launcher utilizado, permisos habilitados y pasos para reproducirlo. Puedes abrir un [issue](https://github.com/JOSEALEJANDRODIAZALI88/app-de-posits/issues) con esa información.

Antes de proponer cambios, ejecuta las comprobaciones de Gradle y prueba en el dispositivo el cambio entre Inicio y otra aplicación. No incluyas rutas locales del SDK, claves de firma, credenciales ni archivos generados de compilación.
