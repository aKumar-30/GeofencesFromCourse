package com.example.geofenceapp.util

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import com.example.geofenceapp.util.Constants.PERMISSION_BACKGROUND_LOCATION_REQUEST_CODE
import com.example.geofenceapp.util.Constants.PERMISSION_LOCATION_REQUEST_CODE
import com.vmadalin.easypermissions.EasyPermissions

object Permissions {
    fun hasLocationPosition (context: Context) =
        EasyPermissions.hasPermissions(context,
        Manifest.permission.ACCESS_FINE_LOCATION)

    fun requestLocationPosition (fragment: Fragment) {
        EasyPermissions.requestPermissions(
            fragment, "This application cannot work without location permissions",
            PERMISSION_LOCATION_REQUEST_CODE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun hasBackgroundLocationPosition (context: Context):Boolean{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return EasyPermissions.hasPermissions(context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        return true
    }

    fun requestBackgroundLocationPosition (fragment: Fragment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                fragment, "Background location is essential to this application",
                PERMISSION_BACKGROUND_LOCATION_REQUEST_CODE,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }
}