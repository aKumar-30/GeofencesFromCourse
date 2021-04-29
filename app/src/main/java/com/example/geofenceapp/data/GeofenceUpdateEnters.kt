package com.example.geofenceapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
class GeofenceUpdateEnters {
    @ColumnInfo(name="id")
    var id: Long = 0
    @ColumnInfo(name = "numberEnters")
    var enters: Int = 0
}