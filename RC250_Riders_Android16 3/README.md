# RC250 Riders on the Storm — versión 0.3

**Proyecto Android nativo con tablero WebView local inspirado en la imagen proporcionada por el usuario.** Conserva el diseño de la RC250, con velocímetro, tormenta, mapa y reproductor representados en la ilustración.

## Mejoras respecto al proyecto anterior
- Hora local real y porcentaje de batería real superpuestos al arte.
- Velocidad real reportada por GPS en km/h. Sin señal o sin velocidad válida aparece `--`, no una cifra inventada.
- Canción y artista obtenidos de la sesión multimedia de Android cuando existe permiso. Actualización en el regreso a la aplicación y mediante callback al cambiar el reproductor seleccionado.
- Controles transparentes de anterior, play/pausa y siguiente, más accesos a Maps, YouTube Music, Apple Music y marcador de teléfono.
- Panel de ajustes integrado con enlaces a permisos de ubicación y acceso a notificaciones.
- Indicador legible del estado GPS y aviso de que el mapa y el clima mostrados en el fondo **son ilustrativos**.
- API objetivo 35, Android 16 incluido en el rango de ejecución, sujeto a seguridad y compatibilidad del teléfono.

## Límites claros
- **El mapa de la imagen no es navegación en vivo.** Tocar el mapa abre Google Maps de forma externa.
- **El clima, la ruta y el progreso musical del fondo no son datos reales.** El arte contiene estos elementos estáticos.
- El control de música depende de sesiones multimedia compatibles y permiso explícito de acceso a notificaciones.
- Es un proyecto fuente, **no una APK compilada o probada** en Android 16. Su instalación no está garantizada y un bloqueo de Play Protect requiere atender el motivo, no deshabilitar la protección.
- El APK `debug` se firma con la clave de depuración del entorno que lo genera. Si existe una instalación anterior con la misma aplicación y otra firma, Android puede rechazar la actualización. Desinstalar la anterior elimina sus datos.
- El GPS requiere permiso del usuario; no sustituye el instrumento original de velocidad de la motocicleta. No operes el teléfono mientras conduces.

## GitHub Actions
1. Descomprime el ZIP y sube **el contenido** de la carpeta a la raíz del repositorio, incluyendo la carpeta oculta `.github`.
2. Abre **Actions → Compilar RC250 Android 15 y 16 → Run workflow** (o ejecuta un push sobre `main`).
3. Descarga el artifact `RC250-Android16-APK` y extrae `app-debug.apk`.
4. Instálalo solo si confías en el código y la procedencia. Si falla, consulta el registro de instalación usando `adb install -r app-debug.apk` y comparte el mensaje **INSTALL_FAILED_...** para saber la causa exacta.

## Para avanzar hacia el diseño totalmente funcional
La integración de navegación giro a giro dentro del tablero requiere un SDK/API de mapas y navegación autorizado, configuración y en su caso claves. No viene integrada en este ZIP.
