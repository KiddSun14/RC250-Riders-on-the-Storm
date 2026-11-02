package com.rc250.rider

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity(), LocationListener {
    private val amber = Color.rgb(255, 174, 40)
    private val bg = Color.rgb(14, 16, 20)
    private lateinit var speed: TextView
    private lateinit var track: TextView
    private lateinit var locationManager: LocationManager
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(28, 16, 28, 18) }
        val title = text("RC250  /  RIDERS ON THE STORM", 23, amber, true)
        root.addView(title)
        val content = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 18, 0, 10) }
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; background = panel() }
        left.addView(text("GPS  ·  KM/H", 16, amber, true))
        speed = text("--", 88, Color.WHITE, true).apply { gravity = Gravity.CENTER }
        left.addView(speed)
        left.addView(text("VELOCIDAD", 14, Color.LTGRAY, false))
        content.addView(left, LinearLayout.LayoutParams(0, -1, 1f).apply { rightMargin = 18 })
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = panel(); setPadding(20, 16, 20, 12) }
        right.addView(text("NAVEGACIÓN", 16, amber, true))
        right.addView(button("ABRIR GOOGLE MAPS") { openPackage("com.google.android.apps.maps") })
        right.addView(text("MÚSICA", 16, amber, true))
        val apps = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        apps.addView(button("YouTube Music") { openPackage("com.google.android.apps.youtube.music") }, LinearLayout.LayoutParams(0, 58, 1f))
        apps.addView(button("Apple Music") { openPackage("com.apple.android.music") }, LinearLayout.LayoutParams(0, 58, 1f))
        right.addView(apps)
        track = text("Reproduce música para ver el título", 14, Color.WHITE, false)
        right.addView(track)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(button("⏮") { withMedia { it.transportControls.skipToPrevious() } }, LinearLayout.LayoutParams(0, 58, 1f))
        controls.addView(button("⏯") { withMedia { c -> if (c.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play() } }, LinearLayout.LayoutParams(0, 58, 1f))
        controls.addView(button("⏭") { withMedia { it.transportControls.skipToNext() } }, LinearLayout.LayoutParams(0, 58, 1f))
        right.addView(controls)
        right.addView(button("ACTIVAR CONTROL DE MÚSICA") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) })
        content.addView(right, LinearLayout.LayoutParams(0, -1, 1.3f))
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(text("RC250  •  CONDUCE CON PRECAUCIÓN", 12, Color.GRAY, false))
        setContentView(root)
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 22) else startGps()
    }
    private fun panel() = GradientDrawable().apply { setColor(Color.rgb(29, 32, 38)); cornerRadius = 22f; setStroke(2, Color.rgb(70, 65, 50)) }
    private fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply { text = value; textSize = size.toFloat(); setTextColor(color); gravity = Gravity.CENTER_VERTICAL; if (bold) setTypeface(null, Typeface.BOLD) }
    private fun button(value: String, action: () -> Unit) = Button(this).apply { text = value; textSize = 12f; isAllCaps = false; setTextColor(amber); setOnClickListener { action() } }
    private fun openPackage(pkg: String) { val launch = packageManager.getLaunchIntentForPackage(pkg); if (launch != null) startActivity(launch) else Toast.makeText(this, "Instala la app primero", Toast.LENGTH_SHORT).show() }
    private fun startGps() { try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, this) } catch (_: SecurityException) { } }
    override fun onLocationChanged(location: Location) { speed.text = String.format(Locale.getDefault(), "%.0f", (location.speed * 3.6f).coerceAtLeast(0f)) }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); if (requestCode == 22 && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) startGps() }
    private fun withMedia(action: (MediaController) -> Unit) { refreshMedia(); val c = mediaController; if (c != null) action(c) else Toast.makeText(this, "Activa control de música y reproduce una canción", Toast.LENGTH_LONG).show() }
    private fun refreshMedia() {
        try {
            val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val sessions = manager.getActiveSessions(ComponentName(this, MediaListener::class.java))
            mediaController = sessions.firstOrNull { it.packageName == "com.google.android.apps.youtube.music" || it.packageName == "com.apple.android.music" } ?: sessions.firstOrNull()
            val metadata = mediaController?.metadata
            track.text = listOfNotNull(metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE), metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST)).joinToString(" — ").ifBlank { "Reproduce música para ver el título" }
        } catch (_: SecurityException) { track.text = "Activa el acceso a notificaciones" }
    }
    override fun onResume() { super.onResume(); if (::track.isInitialized) refreshMedia() }
    override fun onDestroy() { super.onDestroy(); if (::locationManager.isInitialized) locationManager.removeUpdates(this) }
}
