package com.rc250.rider

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity(), LocationListener {
    private lateinit var web: WebView
    private lateinit var locationManager: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private var loaded = false
    private var gpsOn = false
    private val tick = object : Runnable { override fun run() { updateStatus(); handler.postDelayed(this, 2000) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) { loaded = true; updateStatus(); showApps() }
            }
            addJavascriptInterface(Bridge(), "RC250")
        }
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
        handler.post(tick)
        requestGps()
    }

    inner class Bridge {
        @JavascriptInterface fun action(action: String) { runOnUiThread { handle(action) } }
        @JavascriptInterface fun launch(pkg: String) { runOnUiThread { launchPackage(pkg) } }
    }
    private fun js(method: String, value: String) {
        if (loaded) web.evaluateJavascript("window.$method(${JSONObject.quote(value)})", null)
    }
    private fun handle(action: String) {
        when(action) {
            "maps" -> {
                val i = packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
                if (i != null) startActivity(i) else toast("Instala Google Maps")
            }
            "navigation" -> {
                val i = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("google.navigation:q=Cadereyta+de+Montes"))
                i.setPackage("com.google.android.apps.maps")
                try { startActivity(i) } catch (_: Exception) { toast("Google Maps no disponible") }
            }
            "phone" -> try { startActivity(Intent(Intent.ACTION_DIAL)) } catch (_: Exception) { toast("Marcador no disponible") }
            "apps" -> showApps()
            "musicPermission" -> startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            "systemSettings" -> startActivity(Intent(Settings.ACTION_SETTINGS))
            "homeSettings" -> try { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) } catch (_: Exception) { toast("Configura RC250 como inicio en Ajustes") }
            "gps" -> requestGps()
            "prev", "play", "next" -> mediaAction(action)
        }
    }
    private fun launchPackage(pkg: String) {
        try { packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it); return } } catch (_: Exception) { }
        toast("No se puede abrir esta aplicación")
    }
    private fun showApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(intent, 0)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val arr = JSONArray()
        for (app in apps) {
            val item = JSONObject()
            item.put("name", app.loadLabel(packageManager).toString())
            item.put("pkg", app.activityInfo.packageName)
            arr.put(item)
        }
        if (loaded) web.evaluateJavascript("window.setApps($arr)", null)
    }
    private fun mediaController(): MediaController? {
        return try {
            val mgr = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = mgr.getActiveSessions(ComponentName(this, MediaListener::class.java))
            sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
        } catch (_: SecurityException) { null }
    }
    private fun mediaAction(action: String) {
        val c = mediaController() ?: run { toast("Activa el acceso a música en Ajustes"); return }
        when(action) {
            "prev" -> c.transportControls.skipToPrevious()
            "next" -> c.transportControls.skipToNext()
            "play" -> if(c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
        }
        updateStatus()
    }
    private fun updateStatus() {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        js("setBattery", "$battery%")
        val c = mediaController()
        js("setTrack", c?.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Sin reproducción")
        js("setArtist", c?.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Abre tu app de música")
        js("setPlaying", if(c?.playbackState?.state == PlaybackState.STATE_PLAYING) "true" else "false")
    }
    private fun requestGps() {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 42)
        } else startGps()
    }
    private fun startGps() {
        if (gpsOn) return
        try {
            val provider = when {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> { js("setGps", "Activa ubicación"); return }
            }
            locationManager.requestLocationUpdates(provider, 1000L, 1f, this)
            gpsOn = true
            js("setGps", "Buscando GPS")
        } catch (_: Exception) { js("setGps", "GPS no disponible") }
    }
    override fun onLocationChanged(location: Location) {
        js("setSpeed", if(location.hasSpeed()) String.format(Locale.US, "%.0f", (location.speed * 3.6f).coerceAtLeast(0f)) else "--")
        js("setGps", "GPS conectado")
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 42 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) startGps()
    }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    override fun onResume() { super.onResume(); updateStatus() }
    override fun onDestroy() {
        handler.removeCallbacks(tick)
        if(gpsOn) try { locationManager.removeUpdates(this) } catch (_: Exception) { }
        web.removeJavascriptInterface("RC250")
        web.destroy()
        super.onDestroy()
    }
}
