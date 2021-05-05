package com.example.geofenceapp.adapters

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.geofenceapp.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class InfoWindowAdapter (activity: Activity, val callback: OnShowRoutePressedCallback): GoogleMap.InfoWindowAdapter {
    interface OnShowRoutePressedCallback{
        fun onShowBackPressed(latLngEncoded: String)
    }
    private val contentView =
        (activity as Activity).layoutInflater.inflate(R.layout.info_window_layout, null)
    override fun getInfoWindow(marker: Marker?): View {
        if (marker!= null)
            renderViews(marker)
        return contentView
    }

    override fun getInfoContents(marker: Marker?): View {
        if (marker!=null)
            renderViews(marker)
        return contentView
    }
    private fun renderViews(marker: Marker){
        val title = marker.title
        val description = marker.snippet

        val titleTextView = contentView.findViewById<TextView>(R.id.customTitle_textView)
        titleTextView.text = title

        val latlngTextView = contentView.findViewById<TextView>(R.id.customLatLng_textView)
        latlngTextView.text = description

        val showRouteButton = contentView.findViewById<Button>(R.id.showRoute_button)
        showRouteButton.setOnClickListener {
            Log.d("InfoWindowAdapter", "showRouteButton.setOnClickListener: In here")
            callback.onShowBackPressed(description)
        }
    }
}