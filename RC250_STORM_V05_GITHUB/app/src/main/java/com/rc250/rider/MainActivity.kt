package com.rc250.rider

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity(), LocationListener {
    private lateinit var web: WebView
    private lateinit var gps: LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { refreshBattery() }
    }
    private val ticker = object : Runnable {
        override fun run() {
            refreshBattery()
            if (lastLocationAt > 0L && System.currentTimeMillis() - lastLocationAt > 12000) js("setSpeed('--')")
            handler.postDelayed(this, 15000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        gps = getSystemService(LOCATION_SERVICE) as LocationManager
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.allowFileAccess = true // local packaged dashboard asset only
            settings.allowContentAccess = false
            settings.domStorageEnabled = false
            settings.setSupportMultipleWindows(false)
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    ready = true
                    refreshBattery()
                    if (!gpsRunning) js("setStatus('Ubicación pendiente · Mapa y clima ilustrativos')")
                }
            }
            addJavascriptInterface(Bridge(), "RC250")
        }
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        handler.post(ticker)
        requestGpsOrStart()
    }
    inner class Bridge {
        @JavascriptInterface fun action(command: String) { runOnUiThread { perform(command) } }
    }
    private fun perform(command: String) {
        when (command) {
            "maps" -> launch("com.google.android.apps.maps")
            "youtube" -> launch("com.google.android.apps.youtube.music")
            "apple" -> launch("com.apple.android.music")
            "phone" -> startActivity(Intent(Intent.ACTION_DIAL))
            "location" -> requestGpsOrStart()
            "previous", "play", "next" -> {
                toast("Controles disponibles en tu aplicación de música")
                launch("com.google.android.apps.youtube.music")
            }
        }
    }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
    private fun launch(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) try { startActivity(intent) } catch (_: Exception) { toast("No se pudo abrir la aplicación") }
        else toast("Instala la aplicación primero")
    }
    private fun requestGpsOrStart() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 22)
        } else startGps()
    }
    private fun startGps() {
        if (gpsRunning) return
        try {
            val provider = when {
                gps.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                gps.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> { js("setStatus('Activa la ubicación · Mapa y clima ilustrativos')"); return }
            }
            gps.requestLocationUpdates(provider, 1000L, 0f, this)
            gpsRunning = true
            js("setStatus('Buscando señal GPS · Mapa y clima ilustrativos')")
        } catch (_: SecurityException) { js("setStatus('Permiso GPS requerido')") }
        catch (_: Exception) { js("setStatus('GPS no disponible')") }
    }
    override fun onLocationChanged(location: Location) {
        lastLocationAt = System.currentTimeMillis()
        val speed = if (location.hasSpeed()) String.format(Locale.US, "%.0f", (location.speed * 3.6f).coerceAtLeast(0f)) else "--"
        js("setSpeed(${JSONObject.quote(speed)})")
        js("setStatus(${JSONObject.quote(if (location.hasSpeed()) "Velocidad GPS real · Mapa y clima ilustrativos" else "Señal recibida · Esperando velocidad GPS")})")
    }
    private fun js(code: String) { if (ready && !isFinishing) web.evaluateJavascript(code, null) }
    private fun refreshBattery() {
        val battery = getSystemService(BATTERY_SERVICE) as BatteryManager
        val pct = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (pct in 0..100) js("setBattery(${pct})")
    }
    override fun onResume() { super.onResume() }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 22 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) startGps()
        else if (requestCode == 22) js("setStatus('Permite ubicación para usar el velocímetro GPS')")
    }
    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        try { unregisterReceiver(batteryReceiver) } catch (_: Exception) { }
        try { if (gpsRunning) gps.removeUpdates(this) } catch (_: Exception) { }
        if (::web.isInitialized) { web.removeJavascriptInterface("RC250"); web.destroy() }
        super.onDestroy()
    }
}
