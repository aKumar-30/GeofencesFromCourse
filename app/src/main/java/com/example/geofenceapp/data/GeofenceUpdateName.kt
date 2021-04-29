package com.example.geofenceapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
class GeofenceUpdateName {
    @ColumnInfo(name="id")
    var id: Long = 0
    @ColumnInfo(name = "name")
    var name: String = ""
}