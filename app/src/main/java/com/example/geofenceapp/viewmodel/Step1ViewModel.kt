package com.example.geofenceapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class Step1ViewModel: ViewModel(){
    private val _nextButtonEnabled = MutableLiveData(false)
            val nextButtonEnabled: LiveData<Boolean> get() = _nextButtonEnabled

    fun enableNextButton (value: Boolean){
        _nextButtonEnabled.value = value
    }
}