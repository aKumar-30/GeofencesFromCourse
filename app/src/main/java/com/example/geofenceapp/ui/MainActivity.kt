package com.example.geofenceapp.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.example.geofenceapp.R
import com.example.geofenceapp.util.Permissions
import com.example.geofenceapp.viewmodel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Permissions.hasLocationPosition(this)){
            findNavController(R.id.navHostFragment).navigate(
                R.id.action_permissionFragment_to_mapsFragment
            )
        }
    }
}