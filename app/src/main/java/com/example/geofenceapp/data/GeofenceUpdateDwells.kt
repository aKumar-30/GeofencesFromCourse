package com.example.geofenceapp.data

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity
class GeofenceUpdateDwells {
    @ColumnInfo(name="id")
    var id: Long = 0
    @ColumnInfo(name = "numberDwells")
    var dwells: Int = 0
}