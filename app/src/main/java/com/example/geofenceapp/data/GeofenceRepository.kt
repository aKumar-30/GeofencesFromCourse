package com.example.geofenceapp.data

import androidx.lifecycle.LiveData
import androidx.room.Query
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@ViewModelScoped
class GeofenceRepository @Inject constructor(private val geofenceDao: GeofenceDao){
    val readGeofences: Flow<MutableList<GeofenceEntity>> = geofenceDao.readGeofences()

    fun readGeofencesWithQuery(currentGeoId: Long): Flow<MutableList<GeofenceEntity>>{
        return geofenceDao.readGeofencesWithId(currentGeoId)
    }

    suspend fun updateGeofenceName(obj: GeofenceUpdateName) {
        return geofenceDao.updateGeofenceName(obj)
    }
    suspend fun updateGeofenceEnters(obj: GeofenceUpdateEnters) {
        return geofenceDao.updateGeofenceEnters(obj)
    }
    suspend fun updateGeofenceDwells(obj: GeofenceUpdateDwells) {
        return geofenceDao.updateGeofenceDwells(obj)
    }

    fun getGeofenceById(id: Long): LiveData<GeofenceEntity> {
        return geofenceDao.getGeofenceById(id)
    }

    suspend fun insertGeofence(geofenceEntity: GeofenceEntity){
        geofenceDao.insertGeofence(geofenceEntity)
    }
    suspend fun deleteGeofence(geofenceEntity: GeofenceEntity){
        geofenceDao.deleteGeofence(geofenceEntity)
    }

}