package com.example.geofenceapp.data

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Dao
interface GeofenceDao {
    @Query("SELECT * FROM geofence_table ORDER BY id")
    fun readGeofences (): Flow<MutableList<GeofenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofence(geofenceEntity: GeofenceEntity)

    @Update(entity = GeofenceEntity::class)
    suspend fun updateGeofenceName(obj: GeofenceUpdateName)

    @Update(entity = GeofenceEntity::class)
    suspend fun updateGeofenceEnters(obj: GeofenceUpdateEnters)

    @Update(entity = GeofenceEntity::class)
    suspend fun updateGeofenceDwells(obj: GeofenceUpdateDwells)

    @Query("SELECT * FROM geofence_table WHERE id=:id LIMIT 1")
    fun getGeofenceById(id: Long): LiveData<GeofenceEntity>

    @Query("SELECT * FROM geofence_table WHERE geoId= :geoId1 LIMIT 1")
    fun readGeofencesWithId(geoId1: Long): Flow<MutableList<GeofenceEntity>>


    @Delete
    suspend fun deleteGeofence(geofenceEntity: GeofenceEntity)
}