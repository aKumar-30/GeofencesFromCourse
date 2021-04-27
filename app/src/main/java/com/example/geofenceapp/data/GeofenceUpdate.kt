package com.example.geofenceapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
class GeofenceUpdate {
    @ColumnInfo(name="id")
    var id: Int = 0
    @ColumnInfo(name = "name")
    var name: String = ""
}