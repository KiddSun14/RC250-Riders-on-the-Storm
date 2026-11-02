# RC250 Rider — prototipo Android nativo

App de tablero horizontal para Redmi Note 13 Pro / Italika RC250.

## Incluye
- Velocímetro mediante GPS del teléfono (km/h), con permiso de ubicación.
- Accesos directos a Google Maps, YouTube Music y Apple Music (deben estar instaladas).
- Controles multimedia estándar Android (anterior, play/pausa, siguiente) y título de canción, sujetos a que la app de música publique una sesión multimedia compatible.
- Tema negro/ámbar y pantalla horizontal permanente.

## Compilar
1. Instala Android Studio con Android SDK 35 y JDK 17.
2. Abre la carpeta `RC250_Rider` como proyecto.
3. Espera la sincronización de Gradle y ejecuta `Build > Build APK(s)`.
4. Instala `app/build/outputs/apk/debug/app-debug.apk` en el Redmi.
5. Autoriza ubicación y, para los controles multimedia, toca «ACTIVAR CONTROL DE MÚSICA» y habilita acceso a notificaciones para RC250 Rider.

## Límites de esta primera versión
- No es Apple CarPlay ni Android Auto oficial.
- Google Maps abre en su app, no está embebido ni muestra instrucciones dentro del tablero.
- Los metadatos de la canción se actualizan al volver a la app o al usar un control; no hay escucha continua de cambios.
- El GPS puede tardar en fijar señal y la velocidad es estimada; no sustituye el velocímetro de la motocicleta.
- No se ha compilado ni probado en un dispositivo real en este entorno.
- La app no reemplaza el tablero original; no interactúes con ella mientras conduces.
