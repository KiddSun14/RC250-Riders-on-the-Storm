# AutoStrom — Beta 2 (Android)

**AutoStrom** es un launcher para conducción en teléfonos Android, no Android Auto oficial. Es código propio: no contiene recursos ni código de AutoZen. El archivo APK de referencia facilitado se identificó internamente como `com.uptodown`, y **no** forma parte de este proyecto.

## Qué incluye

- Puede seleccionarse como pantalla de inicio (HOME) en Android.
- Interfaz horizontal oscura con naranja, compatible con el diseño objetivo.
- Mapa **real** mediante teselas OpenStreetMap (necesita Internet); ubicación, velocidad GPS y orientación.
- Buscar destino y previsualizar una ruta real sobre el mapa, con distancias y maniobras obtenidas de un servidor de prueba OSRM; al pulsar «Iniciar navegación» se abre Google Maps con indicaciones, recálculo y voz reales.
- Controles de música por MediaSession de Android: título, artista, **carátula** si el reproductor la publica, duración, posición y seek cuando lo admite, anterior/reproducir-pausar/siguiente.
- Panel para abrir apps instaladas con iconos, marcador telefónico, batería, conectividad y ajustes.
- Paquete instalable propio `com.kiddsun14.autostrom.beta`; no sobrescribe la RC250 anterior.

## Importante sobre el mapa y Play Store

La ruta interna utiliza el servidor **demostrativo** `router.project-osrm.org`, que NO ofrece SLA y NO es apropiado como backend comercial. Para una versión pública es obligatorio configurar un proveedor autorizado de mapas y cálculo de rutas (p. ej. una infraestructura OSRM propia o un servicio comercial) y respetar las políticas de uso de las teselas OpenStreetMap. No se garantiza geocodificación ni cobertura en todas las zonas.

Las **indicaciones de navegación completas y recálculo** se realizan al abrir Google Maps. La vista interna muestra previsualización de ruta y avisos básicos; NO sustituye todavía un motor completo de navegación guiada. No interactúes con el dispositivo mientras conduces. Una versión de Play Store requiere, entre otras cosas, pruebas en dispositivos, política de privacidad, cumplimiento de permisos, contenido legal, una clave de firma de publicación y un AAB firmado.

## Subir a GitHub (sin Android Studio)

Repositorio actual: `KiddSun14/RC250-Riders-on-the-Storm`, rama `launcher-beta1`.

1. Descarga y descomprime el ZIP.
2. Abre la rama `launcher-beta1` en GitHub → **Code → Add file → Upload files**.
3. Desde Finder, abre la carpeta `AutoStrom_Beta2` descomprimida y arrastra **el contenido de dentro** (`app/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `README.md`) directamente al área de subida. GitHub reemplaza archivos del mismo nombre en esa rama. No incluyas una carpeta `AutoStrom_Beta2` adicional.
4. Haz **Commit directly to launcher-beta1**. El flujo de trabajo que ya compiló antes puede compilar el nuevo proyecto desde la raíz; no necesitas cambiar `.github` si la versión existente usa `android-actions/setup-android@v4` y `gradle :app:assembleDebug`.
5. Actions → `RC250 STORM V05 - APK` (puede mantener el nombre antiguo, pues el workflow anterior está en `main`) → **Run workflow** → `launcher-beta1`.
6. Si aparece verde, descarga el Artifact APK de esa ejecución. Instala en tu Android y da permiso de ubicación y de escucha de notificaciones (para música).

### Opcional: workflow AutoStrom

El ZIP incluye `.github/workflows/android.yml` listo para compilar con SDK 36, Gradle 8.11.1 y `setup-android@v4`. En macOS la carpeta `.github` está oculta por defecto y **no es necesario** subirla si el workflow de la rama ya funciona. Para crear un repositorio nuevo, sí inclúyela.

## Identidad Android

- Etiqueta visible: **AutoStrom**.
- Kotlin namespace: `com.autostrom.launcher`.
- Application ID de beta: `com.kiddsun14.autostrom.beta`.
- Android: mínimo API 26; objetivo API 36.

## Estado de validación

Código HTML/JS revisado estáticamente. No hay Android SDK ni acceso al ejecutor de GitHub Actions desde este entorno, por lo que **esta Beta 2 aún no está compilada ni probada en el Redmi**. La compilación verde anterior era de Beta 1; no implica que Beta 2 esté validada.
