package com.example.geofenceapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class Step2ViewModel : ViewModel(){
    private val _nextButtonEnabled = MutableLiveData(false)
    val nextButtonEnabled: LiveData<Boolean> get() = _nextButtonEnabled

    private val _internetAvailable = MutableLiveData(true)
    val internetAvailable: LiveData<Boolean> get() = _internetAvailable

    fun enableNextButton (value: Boolean){
        Log.d("SharedViewModel", "enable next button with value: $value")
        _nextButtonEnabled.value = value
    }
    fun setInternetAvailable (value: Boolean){
        _internetAvailable.value = value
    }
}