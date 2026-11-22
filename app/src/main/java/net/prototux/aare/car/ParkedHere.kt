package net.prototux.aare.car

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import androidx.core.content.edit
import net.prototux.aare.R
import net.prototux.aare.Settings
import java.util.Locale

/**
 * MyPlay (ideas 2026-11-22, #11): "Where I parked". Off by default. When a drive ends, the last
 * position (the car's GPS if it sent one, else the phone's last known fix) is kept on the phone,
 * and a quiet notification offers "Navigate back to car" (walking in Google Maps when installed,
 * else any maps app through a geo: link). Only the newest spot is kept; never backed up, never in
 * the log, never sent anywhere by MyPlay.
 */
const val KEY_PARKED_HERE = "myplay_parked_here"
var Settings.myplayParkedHere: Boolean
    get() = prefs.getBoolean(KEY_PARKED_HERE, false)
    set(v) = prefs.edit {
        putBoolean(KEY_PARKED_HERE, v)
        // Turned off: forget the spot too.
        if (!v) remove(KEY_PARKED_SPOT)
    }

/** The newest spot, "lat,lng,timeMs". Not backed up. */
const val KEY_PARKED_SPOT = "myplay_parked_spot"

data class ParkedSpot(val lat: Double, val lng: Double, val atMs: Long)

object ParkedHere {
    /** A fix older than this when the drive ends is not where the car stopped. */
    const val MAX_FIX_AGE_MS = 5L * 60 * 1000
    const val NOTIFICATION_ID = 11
    const val CHANNEL = "myplay_parked"
    const val GOOGLE_MAPS = "com.google.android.apps.maps"

    /** The spot from [car] (preferred) or [phone], if one is fresh at [nowMs]. */
    fun pick(car: Location?, phone: Location?, nowMs: Long): ParkedSpot? =
        listOfNotNull(car, phone).firstOrNull { nowMs - it.time in 0..MAX_FIX_AGE_MS }?.let { ParkedSpot(it.latitude, it.longitude, nowMs) }

    fun encode(s: ParkedSpot): String = String.format(Locale.ROOT, "%.6f,%.6f,%d", s.lat, s.lng, s.atMs)

    fun decode(v: String?): ParkedSpot? = runCatching {
        val (lat, lng, at) = v!!.split(',')
        ParkedSpot(lat.toDouble(), lng.toDouble(), at.toLong())
    }.getOrNull()

    fun latLng(s: ParkedSpot): String = String.format(Locale.ROOT, "%.6f,%.6f", s.lat, s.lng)

    /** Walking directions in Google Maps. */
    fun walkingUri(s: ParkedSpot): Uri = Uri.parse("google.navigation:q=${latLng(s)}&mode=w")

    /** Any maps app (Waze, Organic Maps…) at the spot. */
    fun geoUri(s: ParkedSpot): Uri = Uri.parse("geo:${latLng(s)}?q=${latLng(s)}")

    /** The intent for "Navigate back to car" on this phone. */
    fun navigateIntent(context: Context, s: ParkedSpot): Intent {
        val walk = Intent(Intent.ACTION_VIEW, walkingUri(s)).setPackage(GOOGLE_MAPS)
        val usable = runCatching { walk.resolveActivity(context.packageManager) != null }.getOrDefault(false)
        return (if (usable) walk else Intent(Intent.ACTION_VIEW, geoUri(s))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** The drive ended: keep the spot and offer the way back, if turned on. Main thread. */
    fun driveEnded(context: Context, settings: Settings, car: Location?, phone: Location?, nowMs: Long = System.currentTimeMillis()) {
        if (!settings.myplayParkedHere) return
        val spot = pick(car, phone, nowMs) ?: return
        settings.prefs.edit { putString(KEY_PARKED_SPOT, encode(spot)) }
        runCatching { notify(context, spot) }
    }

    private fun notify(context: Context, spot: ParkedSpot) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.myplay_parked_channel), NotificationManager.IMPORTANCE_LOW))
        val pi = PendingIntent.getActivity(context, NOTIFICATION_ID, navigateIntent(context, spot), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val time = android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(spot.atMs))
        nm.notify(NOTIFICATION_ID, Notification.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.myplay_parked_title, time))
            .setContentText(context.getString(R.string.myplay_parked_text))
            .setContentIntent(pi).setAutoCancel(true)
            .addAction(Notification.Action.Builder(null, context.getString(R.string.myplay_parked_navigate), pi).build())
            .build())
    }
}
