package com.example.geofenceapp.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow


@Dao
interface GeofenceDao {
    @Query("SELECT * FROM geofence_table ORDER BY id")
    fun readGeofences (): Flow<MutableList<GeofenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofence(geofenceEntity: GeofenceEntity)

    @Update(entity = GeofenceEntity::class)
    suspend fun updateGeofence(obj: GeofenceUpdate)
//    @RawQuery
//    suspend fun readGeofencesWithId(query: SupportSQLiteQuery?): Flow<MutableList<GeofenceEntity>>?
//
//    suspend fun readGeofencesWithQuery(currentGeoId: Double): Flow<MutableList<GeofenceEntity>>?{
//        // List of bind parameters
//        val args: MutableList<Any> = ArrayList()
//
//        val queryString = "SELECT * FROM geofence_table WHERE geoId = ?%"
//        args.add(currentGeoId);
//
//        val query = SimpleSQLiteQuery(queryString, args.toTypedArray())
//        return readGeofencesWithId(query)
//    }
    @Query("SELECT * FROM geofence_table WHERE geoId= :geoId1")
    fun readGeofencesWithId(geoId1: Double): Flow<MutableList<GeofenceEntity>>

//    suspend fun readGeofencesWithQuery(currentGeoId: Double): Flow<MutableList<GeofenceEntity>>?{
//        // List of bind parameters
//        val args: MutableList<Any> = ArrayList()
//
//        val queryString = "SELECT * FROM geofence_table WHERE geoId = ?%"
//        args.add(currentGeoId);
//
//        val query = SimpleSQLiteQuery(queryString, args.toTypedArray())
//        return readGeofencesWithId(query)
//    }
    @Delete
    suspend fun deleteGeofence(geofenceEntity: GeofenceEntity)
}