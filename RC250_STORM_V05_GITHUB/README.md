# RC250 STORM v0.5 — Tablero cinematográfico

Esta versión **no usa el panel básico gris**. Utiliza `app/src/main/assets/dashboard.jpg` como fondo y superpone datos de velocidad GPS, batería y hora. Google Maps y apps musicales se abren externamente. El mapa y clima dibujados son ilustrativos.

## Subir correctamente a GitHub

1. Descarga y descomprime el ZIP.
2. En el repositorio existente, sube **el contenido** de la carpeta RC250_STORM_V05 a la **raíz**, para que `app`, `.github`, `build.gradle.kts`, `settings.gradle.kts` estén al mismo nivel que el resto de archivos. No subas la carpeta RC250_STORM_V05 entera.
3. Comprueba que exista `app/src/main/assets/dashboard.jpg` en GitHub.
4. En Actions ejecuta **RC250 STORM V05 - NUEVO TABLERO**, NO el antiguo `Compilar APK RC250`.
5. Descarga únicamente el artefacto **RC250-STORM-V05-NUEVA-APK**.
6. Instala la APK. Se llama **RC250 STORM V5** y se instala separada de la aplicación anterior, para comprobar cuál estás abriendo.

**Nota:** No hemos compilado ni verificado su instalación real. No desactives Play Protect.
