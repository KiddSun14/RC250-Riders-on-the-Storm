# RC250 Riders on the Storm — Launcher Beta 1

Proyecto Android nativo Kotlin con interfaz local HTML/CSS. **No es una APK ya compilada**.

## Funciones implementadas
- Actividad HOME para seleccionarlo como launcher predeterminado.
- Lista de aplicaciones instaladas con acceso para abrirlas.
- Velocidad por GPS, batería y reloj reales.
- Controles de sesión multimedia (requiere habilitar acceso a notificaciones).
- Acceso a Maps, marcador, navegación y ajustes.
- Interfaz horizontal negro/naranja.

## Limitaciones conocidas
- La navegación abre Google Maps externamente; **no** existe mapa embebido ni instrucciones de giro dentro del launcher.
- El destino del botón "Ir a" está configurado a Cadereyta de Montes a modo de acceso rápido; debe hacerse configurable.
- No hay integración directa con Spotify ni con llamadas Bluetooth; controles dependen de sesiones multimedia Android.
- No se ha compilado ni probado aquí. Necesita pruebas reales y revisión de requisitos de Play Store.
- El AAB de GitHub Actions no está firmado para publicación. Para Play Store requiere firma de release, ficha, política de privacidad, pruebas y revisión.
- No uses la interfaz mientras conduces.

## Compilar en Mac
1. Instala Android Studio actualizado y abre la carpeta `RC250_Launcher`.
2. Instala Android SDK 36 desde SDK Manager y sincroniza Gradle.
3. Selecciona `Build > Build APK(s)` para generar el APK debug.
4. Alternativamente, sube todo el contenido a un repositorio GitHub y ejecuta Actions > Compilar RC250 Launcher > Run workflow. Descarga `RC250-Launcher-Beta-APK`.
5. Instala la APK en el Redmi y prueba los permisos de ubicación, acceso a notificaciones y launcher predeterminado.
