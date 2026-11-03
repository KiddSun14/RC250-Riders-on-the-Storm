package com.autostrom.launcher

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.max

/** AutoStrom — pantalla HOME + panel nativo, mapa de prueba y controles del sistema. */
class MainActivity : Activity(), LocationListener, TextToSpeech.OnInitListener {
    private lateinit var web: WebView
    private lateinit var locations: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private var ready = false
    private var gpsOn = false
    private var currentLocation: Location? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var lastArtworkId: String? = null
    private var lastArtwork: String = ""
    private var lastAppList: JSONArray = JSONArray()
    private val ticker = object : Runnable {
        override fun run() {
            updateStatus()
            handler.postDelayed(this, 1800L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        locations = getSystemService(LOCATION_SERVICE) as LocationManager
        tts = TextToSpeech(this, this)
        web = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(7, 8, 10))
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.domStorageEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.loadsImagesAutomatically = true
            settings.userAgentString = settings.userAgentString + " AutoStrom/0.2"
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    if (url == "file:///android_asset/index.html") {
                        ready = true
                        updateStatus()
                        refreshApps()
                        currentLocation?.let { onLocationChanged(it) }
                    }
                }
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url ?: return true
                    if (url.scheme == "http" || url.scheme == "https") {
                        try { startActivity(Intent(Intent.ACTION_VIEW, url)) } catch (_: Exception) { }
                    }
                    return true
                }
            }
            addJavascriptInterface(Bridge(), "Native") // Solo se carga el HTML empaquetado.
        }
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
        handler.post(ticker)
        requestGps()
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale("es", "MX")
    }

    inner class Bridge {
        @JavascriptInterface fun action(name: String) { runOnUiThread { doAction(name) } }
        @JavascriptInterface fun openApp(pkg: String) { runOnUiThread { openPackage(pkg) } }
        @JavascriptInterface fun routeTo(search: String) { runOnUiThread { buildRoute(search) } }
        @JavascriptInterface fun navigate(search: String) { runOnUiThread { openNavigation(search) } }
        @JavascriptInterface fun seekTo(millis: Long) { runOnUiThread {
            try { mediaController()?.transportControls?.seekTo(max(0L, millis)) } catch (_: Exception) { }
        } }
        @JavascriptInterface fun speak(phrase: String) { runOnUiThread {
            if (ttsReady && phrase.length <= 240) tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "autostrom-turn")
        } }
    }

    private fun emit(event: String, json: JSONObject) {
        if (ready) web.evaluateJavascript("window.AutoStrom && window.AutoStrom.$event(${json})", null)
    }
    private fun emitString(event: String, message: String) {
        emit(event, JSONObject().put("text", message))
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun doAction(name: String) {
        when (name) {
            "maps" -> openPackage("com.google.android.apps.maps")
            "phone" -> try { startActivity(Intent(Intent.ACTION_DIAL)) } catch (_: Exception) { toast("Marcador no disponible") }
            "apps" -> refreshApps()
            "mediaPermission" -> startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            "homeSettings" -> try { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) } catch (_: Exception) { toast("Abre Ajustes > Aplicaciones predeterminadas") }
            "settings" -> startActivity(Intent(Settings.ACTION_SETTINGS))
            "gps" -> requestGps()
            "previous", "toggle", "next" -> mediaAction(name)
        }
    }

    private fun openPackage(pkg: String) {
        try {
            val launch = packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) { startActivity(launch); return }
        } catch (_: Exception) { }
        toast("Aplicación no disponible")
    }

    private fun refreshApps() {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(query, 0)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
        val arr = JSONArray()
        apps.forEach { info ->
            val row = JSONObject()
            row.put("name", info.loadLabel(packageManager).toString())
            row.put("pkg", info.activityInfo.packageName)
            try { row.put("icon", drawableData(info.loadIcon(packageManager))) } catch (_: Exception) { row.put("icon", "") }
            arr.put(row)
        }
        lastAppList = arr
        if (ready) web.evaluateJavascript("window.AutoStrom && window.AutoStrom.onApps($arr)", null)
    }

    private fun drawableData(drawable: Drawable): String {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, 64, 64)
        drawable.draw(canvas)
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, output)
        bitmap.recycle()
        return "data:image/png;base64," + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun mediaController(): MediaController? = try {
        val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
        val sessions = manager.getActiveSessions(ComponentName(this, MediaListener::class.java))
        sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull { it.metadata != null }
            ?: sessions.firstOrNull()
    } catch (_: Exception) { null }

    private fun mediaAction(name: String) {
        val media = mediaController()
        if (media == null) { toast("Habilita acceso a notificaciones para controlar música"); return }
        try {
            when(name) {
                "previous" -> media.transportControls.skipToPrevious()
                "next" -> media.transportControls.skipToNext()
                "toggle" -> if (media.playbackState?.state == PlaybackState.STATE_PLAYING) media.transportControls.pause() else media.transportControls.play()
            }
        } catch (_: Exception) { toast("El reproductor no admite ese control") }
        handler.postDelayed({ updateStatus() }, 220L)
    }

    private fun coverData(media: MediaController?, id: String): String {
        if (lastArtworkId == id) return lastArtwork
        lastArtworkId = id
        lastArtwork = ""
        try {
            val metadata = media?.metadata ?: return ""
            val artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            if (artwork != null) {
                val target = Bitmap.createScaledBitmap(artwork, 260, 260, true)
                val bytes = ByteArrayOutputStream()
                target.compress(Bitmap.CompressFormat.JPEG, 78, bytes)
                if (target !== artwork) target.recycle()
                lastArtwork = "data:image/jpeg;base64," + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)
            }
        } catch (_: Exception) { }
        return lastArtwork
    }

    private fun updateStatus() {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        var online = false
        try {
            val network = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            online = network.getNetworkCapabilities(network.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (_: Exception) { }
        emit("onSystem", JSONObject().put("battery", battery).put("online", online))
        val media = mediaController()
        val metadata = media?.metadata
        val playback = media?.playbackState
        var position = max(0L, playback?.position ?: 0L)
        if (playback?.state == PlaybackState.STATE_PLAYING && playback.lastPositionUpdateTime > 0) {
            position += ((SystemClock.elapsedRealtime() - playback.lastPositionUpdateTime) * playback.playbackSpeed).toLong()
        }
        val duration = max(0L, metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L)
        if (duration > 0) position = position.coerceAtMost(duration)
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: "Sin reproducción"
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: "Abre tu reproductor"
        val id = "${media?.packageName}|$title|$artist|${metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)}|$duration"
        emit("onMedia", JSONObject()
            .put("title", title).put("artist", artist)
            .put("playing", playback?.state == PlaybackState.STATE_PLAYING)
            .put("position", position).put("duration", duration)
            .put("art", coverData(media, id)).put("app", media?.packageName ?: "")
            .put("permissionRequired", media == null))
    }

    private fun requestGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 42)
        } else startGps()
    }
    private fun startGps() {
        if (gpsOn) return
        try {
            var subscribed = false
            for (provider in arrayOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
                if (locations.isProviderEnabled(provider)) {
                    locations.requestLocationUpdates(provider, 1000L, 1f, this)
                    subscribed = true
                    locations.getLastKnownLocation(provider)?.let { onLocationChanged(it) }
                }
            }
            gpsOn = subscribed
            if (!subscribed) emitString("onError", "Activa los servicios de ubicación")
        } catch (_: SecurityException) { emitString("onError", "Concede permiso de ubicación") }
        catch (_: Exception) { emitString("onError", "No se pudo iniciar el GPS") }
    }
    override fun onLocationChanged(location: Location) {
        val old = currentLocation
        if (old != null && location.elapsedRealtimeNanos < old.elapsedRealtimeNanos) return
        currentLocation = location
        emit("onLocation", JSONObject().put("lat", location.latitude).put("lon", location.longitude)
            .put("speed", if (location.hasSpeed()) max(0f, location.speed * 3.6f).toInt() else -1)
            .put("bearing", if (location.hasBearing()) location.bearing.toDouble() else 0.0)
            .put("accuracy", if (location.hasAccuracy()) location.accuracy.toDouble() else -1.0))
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 42 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) startGps()
    }

    /** Navegación interna de prueba con datos OSRM; no es proveedor para publicación. */
    private fun buildRoute(rawSearch: String) {
        val search = rawSearch.trim().take(160)
        if (search.length < 3) { emitString("onError", "Escribe un destino"); return }
        val origin = currentLocation
        if (origin == null) { emitString("onError", "Espera a que el GPS obtenga tu posición"); requestGps(); return }
        emitString("onRouteStatus", "Calculando ruta real…")
        io.execute {
            try {
                @Suppress("DEPRECATION")
                val results = Geocoder(this, Locale("es", "MX")).getFromLocationName(search, 1)
                val place = results?.firstOrNull() ?: throw IllegalStateException("No se encontró el destino. Prueba una dirección más completa.")
                val url = String.format(Locale.US,
                    "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson&steps=true",
                    origin.longitude, origin.latitude, place.longitude, place.latitude)
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 9000
                conn.readTimeout = 12000
                conn.setRequestProperty("User-Agent", "AutoStrom/0.2")
                conn.connect()
                if (conn.responseCode != 200) throw IllegalStateException("Servicio de rutas no disponible (${conn.responseCode})")
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val objectRoot = JSONObject(response)
                val result = objectRoot.optJSONArray("routes")?.optJSONObject(0)
                    ?: throw IllegalStateException("No se encontró una ruta para ese destino")
                val coordinates = result.getJSONObject("geometry").getJSONArray("coordinates")
                val routePoints = JSONArray()
                for (i in 0 until coordinates.length()) {
                    val point = coordinates.getJSONArray(i)
                    routePoints.put(JSONArray().put(point.getDouble(1)).put(point.getDouble(0)))
                }
                val steps = JSONArray()
                val legs = result.optJSONArray("legs")
                if (legs != null) for (li in 0 until legs.length()) {
                    val maneuvers = legs.getJSONObject(li).getJSONArray("steps")
                    for (si in 0 until maneuvers.length()) {
                        val step = maneuvers.getJSONObject(si)
                        val move = step.getJSONObject("maneuver")
                        val coord = move.getJSONArray("location")
                        steps.put(JSONObject().put("type", move.optString("type"))
                            .put("modifier", move.optString("modifier"))
                            .put("road", step.optString("name"))
                            .put("distance", step.optDouble("distance"))
                            .put("lat", coord.getDouble(1)).put("lon", coord.getDouble(0)))
                    }
                }
                val payload = JSONObject().put("destination", search)
                    .put("end", JSONArray().put(place.latitude).put(place.longitude))
                    .put("distance", result.getDouble("distance"))
                    .put("duration", result.getDouble("duration"))
                    .put("points", routePoints).put("steps", steps)
                runOnUiThread { emit("onRoute", payload) }
            } catch (e: Exception) {
                runOnUiThread { emitString("onError", "Ruta interna: ${e.message ?: "servicio no disponible"}. Puedes usar Google Maps.") }
            }
        }
    }

    /** Navegación por voz y recálculo con Google Maps; funciona sin servidor propio. */
    private fun openNavigation(rawSearch: String) {
        val destination = rawSearch.trim().ifBlank { "Cadereyta de Montes, Querétaro" }
        val uri = Uri.parse("google.navigation:q=" + Uri.encode(destination) + "&mode=d")
        try {
            val intent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")
            startActivity(intent)
        } catch (_: Exception) {
            try {
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination) + "&travelmode=driving")
                startActivity(Intent(Intent.ACTION_VIEW, webUri))
            } catch (_: Exception) { toast("No se pudo abrir la navegación") }
        }
    }

    override fun onResume() { super.onResume(); handler.postDelayed({ updateStatus(); refreshApps() }, 400L) }
    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        try { if (gpsOn) locations.removeUpdates(this) } catch (_: Exception) { }
        tts?.stop(); tts?.shutdown()
        io.shutdownNow()
        web.removeJavascriptInterface("Native")
        web.destroy()
        super.onDestroy()
    }
}
