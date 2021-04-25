package com.example.geofenceapp.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow

class NetworkListener: ConnectivityManager.NetworkCallback(){

    private val isNetworkAvailable = MutableStateFlow(false)

    @RequiresApi(Build.VERSION_CODES.N)
    fun checkNetworkAvailability(context: Context): MutableStateFlow<Boolean>{
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(this)

        var isConnected = false

        connectivityManager.allNetworks.forEach { network ->
            val netWorkCapability = connectivityManager.getNetworkCapabilities(network)
            netWorkCapability.let{ networkCap ->
                if(networkCap!!.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    isConnected = true
                    return@forEach
                }
            }
        }

        isNetworkAvailable.value = isConnected

        return isNetworkAvailable
    }

    override fun onLost(network: Network) {
        isNetworkAvailable.value = false
    }

    override fun onAvailable(network: Network) {
        isNetworkAvailable.value = true
    }
}