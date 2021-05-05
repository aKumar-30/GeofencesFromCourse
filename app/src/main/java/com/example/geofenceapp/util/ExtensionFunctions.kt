package com.example.geofenceapp.util

import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.geofenceapp.util.Constants.DEFAULT_LATITUDE
import com.example.geofenceapp.util.Constants.DEFAULT_LONGITUDE
import com.google.android.gms.maps.model.LatLng

object ExtensionFunctions {
    fun View.enable(){
        this.isEnabled = true
    }
    fun View.disable(){
        this.isEnabled = false
    }
    fun View.show(){
        this.visibility = View.VISIBLE
    }
    fun View.hide(){
        this.visibility = View.GONE
    }
    fun reverseParse(latlngString: String): LatLng?{
        val longitude: Double? = latlngString.substringAfterLast(" ", DEFAULT_LONGITUDE.toString()).toDoubleOrNull()
        val latitude: Double? = latlngString.substringBeforeLast(",", DEFAULT_LATITUDE.toString()).toDoubleOrNull()
        return if(longitude != null && latitude != null)
            LatLng(longitude, latitude)
        else
            null
    }
    fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>){
        observe (lifecycleOwner, object: Observer<T>{
            override fun onChanged(t: T) {
                observer.onChanged(t)
                removeObserver(this)
            }
        })
    }
}