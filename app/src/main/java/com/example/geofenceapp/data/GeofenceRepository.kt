package com.example.geofenceapp.data

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@ViewModelScoped
class GeofenceRepository @Inject constructor(private val geofenceDao: GeofenceDao){
    val readGeofences: Flow<MutableList<GeofenceEntity>> = geofenceDao.readGeofences()

    fun readGeofencesWithQuery(currentGeoId: Double): Flow<MutableList<GeofenceEntity>>{
        return geofenceDao.readGeofencesWithId(currentGeoId)
    }

    suspend fun updateGeofence(obj: GeofenceUpdate) {
        return geofenceDao.updateGeofence(obj)
    }

    suspend fun insertGeofence(geofenceEntity: GeofenceEntity){
        geofenceDao.insertGeofence(geofenceEntity)
    }
    suspend fun deleteGeofence(geofenceEntity: GeofenceEntity){
        geofenceDao.deleteGeofence(geofenceEntity)
    }

}