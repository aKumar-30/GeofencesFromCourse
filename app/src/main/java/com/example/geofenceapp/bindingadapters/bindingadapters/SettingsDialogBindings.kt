package com.example.geofenceapp.bindingadapters.bindingadapters

import android.content.Context
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.example.geofenceapp.R
import com.google.android.material.slider.Slider

@BindingAdapter("updateSettingsSliderTextView")
fun Slider.updateSettingsSliderTextView(textView: TextView){
    this.setTextValue(context, textView, value)
    this.addOnChangeListener{ _, value, _ ->
        this.setTextValue(context, textView, value)
    }

}

fun Slider.setTextValue(context: Context, textView: TextView, value: Float){
    val kilometers = value/1000
    if (value >= 1000f){
        textView.text = context.getString(R.string.display_kilometers, kilometers.toString())
    } else {
        textView.text = context.getString(R.string.display_meters, value.toString())
    }
}