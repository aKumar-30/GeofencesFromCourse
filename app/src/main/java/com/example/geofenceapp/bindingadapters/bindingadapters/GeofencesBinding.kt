package com.example.geofenceapp.bindingadapters.bindingadapters

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import coil.load
import com.example.geofenceapp.R
import com.example.geofenceapp.data.GeofenceEntity
import com.example.geofenceapp.util.ExtensionFunctions.disable
import com.example.geofenceapp.util.ExtensionFunctions.enable

@BindingAdapter("setVisibility")
fun View.setVisibility(data: List<GeofenceEntity>){
    if (data.isNullOrEmpty()){
        this.visibility = View.VISIBLE
    } else {
        this.visibility = View.INVISIBLE
    }
}

//@BindingAdapter("handleMotionTransition")
//fun MotionLayout.handleMotionTransition(deleteImageView: ImageView){
//    deleteImageView.disable()
//    this.setTransitionListener(object: MotionLayout.TransitionListener{
//        override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
//        }
//
//        override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {
//        }
//
//        override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
//        }
//
//        override fun onTransitionCompleted(motionLayout: MotionLayout?, transition: Int) {
//            if(motionLayout!=null && transition== R.id.start){
//                deleteImageView.disable()
//            }
//            else if (motionLayout!=null && transition==R.id.end){
//                deleteImageView.enable()
//            }
//        }
//    })
//}

@BindingAdapter("setSnapshot")
fun ImageView.setSnapshot(snapshot: Bitmap?){
    if (snapshot != null) {
        this.load(snapshot)
    }
    else {
        Log.d("GeofenceBinding", "snaoshot is null")
        this.load(ContextCompat.getDrawable(context, R.drawable.ic_placeholder))
    }
}

@SuppressLint("SetTextI18n")
@BindingAdapter("setLat","setLong", requireAll = true)
fun TextView.setCoordinates(lat: Double, long: Double){
    val lat2 = String.format("%.4f", lat)
    val long2 = String.format("%.4f", long)
    this.text = "$lat2, $long2"
}