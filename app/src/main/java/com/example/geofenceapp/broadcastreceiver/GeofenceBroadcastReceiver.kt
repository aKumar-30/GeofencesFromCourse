package com.example.geofenceapp.broadcastreceiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.geofenceapp.R
import com.example.geofenceapp.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.geofenceapp.util.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.geofenceapp.util.Constants.NOTIFICATION_ID
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver: BroadcastReceiver() {
    companion object {
        private var _geofenceChanged = MutableLiveData(0.0.toLong())
         val geofenceChanges: LiveData<Long> get() = _geofenceChanged
        var currentGeofenceChange: Int? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()){
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e("BroadcastReceiver", errorMessage)
            return
        }
        _geofenceChanged.value = geofencingEvent.triggeringGeofences[0].requestId.toLong()
        Log.d("BroadcastReceiver", "the new value os ${geofenceChanges.value}")

        when (geofencingEvent.geofenceTransition){
            Geofence.GEOFENCE_TRANSITION_ENTER->{
                currentGeofenceChange = Geofence.GEOFENCE_TRANSITION_ENTER
                displayNotification(context, "Geofence ENTER")
            }
            Geofence.GEOFENCE_TRANSITION_DWELL ->{
                currentGeofenceChange = Geofence.GEOFENCE_TRANSITION_DWELL
                displayNotification(context, "Geofence DWELL")
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                currentGeofenceChange = Geofence.GEOFENCE_TRANSITION_EXIT
                displayNotification(context, "Geofence EXIT")
            }
            else -> {
                currentGeofenceChange = null
                displayNotification(context, "Invalid type")
            }
        }
    }

    private fun displayNotification (context: Context, geofenceTransition: String){
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Geofence")
            .setContentText(geofenceTransition)
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

    private fun createNotificationChannel (notificationManager: NotificationManager){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }
}