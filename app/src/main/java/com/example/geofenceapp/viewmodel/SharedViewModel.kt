package com.example.geofenceapp.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.geofenceapp.broadcastreceiver.GeofenceBroadcastReceiver
import com.example.geofenceapp.data.DataStoreRepository
import com.example.geofenceapp.data.GeofenceEntity
import com.example.geofenceapp.data.GeofenceRepository
import com.example.geofenceapp.util.Permissions
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class SharedViewModel @Inject constructor(application: Application,
private val dataStoreRepository: DataStoreRepository,
private val geofenceRepository: GeofenceRepository
): AndroidViewModel(application){
    val app = application
    private var geofencingClient = LocationServices.getGeofencingClient(app.applicationContext)

    var geoCountryCode = ""
    var geoName = "Default" //TODO MAKE BETTER
    var geoId = 0L

    var geoLocationName: String = "Search a city"
    var geoLatLng: LatLng = LatLng(0.0, 0.0)
    var geoRadius: Float = 500f

    var geoCitySelected: Boolean = false
    var geoFenceReady: Boolean = false
    var geoFencePrepared: Boolean = false
    var geoSnapshot: Bitmap? = null

    var geofenceRemoved = false

    fun resetSharedValues(){
        geoId = 0
        geoName = "Default"
        geoCountryCode=""
        geoLocationName = "Search a city"
        geoLatLng=LatLng(0.0, 0.0)
        geoRadius = 500f
        geoSnapshot = null
        geoCitySelected = false
        geoFenceReady = false
        geoFencePrepared = false
        geofenceRemoved = false
    }

    //DataStore
    val readFirstLaunch: LiveData<Boolean> = dataStoreRepository.readFirstLaunch.asLiveData()

    fun saveFirstLaunch (firstLaunch: Boolean) = viewModelScope.launch (Dispatchers.IO) {
        dataStoreRepository.saveFirstLaunch(firstLaunch)
    }

    //Database
    val readGeofences: LiveData<MutableList<GeofenceEntity>> = geofenceRepository.readGeofences.asLiveData()
    var readGeofencesWithQuery: LiveData<MutableList<GeofenceEntity>>? = null

     fun readGeofencesWithQuery(currentGeoId: Double){
        viewModelScope.launch (Dispatchers.IO) {
            readGeofencesWithQuery = geofenceRepository.readGeofencesWithQuery(currentGeoId).asLiveData()
        }
    }
    fun insertGeofence (geofenceEntity: GeofenceEntity){
        viewModelScope.launch (Dispatchers.IO) {
            geofenceRepository.insertGeofence(geofenceEntity)
        }
    }

    fun deleteGeofence (geofenceEntity: GeofenceEntity){
        viewModelScope.launch (Dispatchers.IO){
            geofenceRepository.deleteGeofence(geofenceEntity)
        }
    }

    private fun setPendingIntent(geoId: Int): PendingIntent{
        val intent = Intent(app, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            app,
            geoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("MissingPermission")
    fun startGeofence(
        latitude: Double,
        longitude: Double
    ) {
        if (Permissions.hasBackgroundLocationPosition(app)){
            val geofence = Geofence.Builder()
                .setRequestId(geoId.toString())
                .setCircularRegion(
                    latitude,
                    longitude,
                    geoRadius
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_EXIT
                    or Geofence.GEOFENCE_TRANSITION_DWELL
                    or Geofence.GEOFENCE_TRANSITION_ENTER
                )
                .setLoiteringDelay(5000) //TODO increase
                .build()
            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(
                    GeofencingRequest.INITIAL_TRIGGER_DWELL
                    or GeofencingRequest.INITIAL_TRIGGER_ENTER
                    or GeofencingRequest.INITIAL_TRIGGER_EXIT
                )
                .addGeofence(geofence)
                .build()

            geofencingClient.addGeofences(geofencingRequest, setPendingIntent(geoId.toInt())).run{
                addOnSuccessListener {
                    Log.d("Geofence", "successfully added geofence")
                }
                addOnCanceledListener {
                    Log.d("Geofence", "failed adding geofence")
                }
            }
        } else {
            Log.d("Geofence", "Permission not granted")
        }
    }

    suspend fun stopGeofence(geoIds: List<Long>): Boolean{
        return if (Permissions.hasBackgroundLocationPosition(app)){
            val result = CompletableDeferred<Boolean>()
            geofencingClient.removeGeofences(setPendingIntent(geoIds.first().toInt()))
                .addOnCompleteListener {
                    if (it.isSuccessful){
                        result.complete(true)
                    } else {
                        result.complete(false)
                    }
                }
            result.await()
        } else {
            false
        }
    }

    fun getBounds(center: LatLng, radius: Float): LatLngBounds{
        val southwestPoint = getSouthwest(center, radius)
        val northeastCorner = getNortheast(center, radius)
        return LatLngBounds(southwestPoint, northeastCorner)
    }
    fun getSouthwest(center: LatLng, radius: Float): LatLng{
        val distanceFromCenterToCorner = radius * sqrt(2.0)
        return SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 225.0)
    }
    fun getNortheast(center: LatLng, radius: Float): LatLng{
        val distanceFromCenterToCorner = radius * sqrt(2.0)
        return SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0)
    }
     fun addGeofenceToDatabase(location: LatLng) {
        val geofenceEntity = GeofenceEntity (
            geoId,
            geoName,
            geoLocationName,
            location.latitude,
            location.longitude,
            geoRadius,
            geoSnapshot!!
        )
        insertGeofence(geofenceEntity)
    }


    fun checkDeviceLocationSettings(context: Context): Boolean{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isLocationEnabled
        } else {
            val mode: Int = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            mode != Settings.Secure.LOCATION_MODE_OFF
        }
    }
}