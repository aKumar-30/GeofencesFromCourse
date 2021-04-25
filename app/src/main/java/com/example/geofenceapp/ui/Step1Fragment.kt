package com.example.geofenceapp.ui

import android.annotation.SuppressLint
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.geofenceapp.R
import com.example.geofenceapp.databinding.FragmentStep1Binding
import com.example.geofenceapp.viewmodel.SharedViewModel
import com.example.geofenceapp.viewmodel.Step1ViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.IOException

@AndroidEntryPoint
class Step1Fragment : Fragment() {
    private var _binding: FragmentStep1Binding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val step1ViewModel: Step1ViewModel by viewModels()

    private lateinit var geoCoder: Geocoder
    private lateinit var placesClient: PlacesClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Places.initialize(requireContext(), getString(R.string.google_maps_key))
        placesClient = Places.createClient(requireContext())
        geoCoder = Geocoder(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStep1Binding.inflate(inflater, container, false)
        binding.sharedViewModel = sharedViewModel
        binding.step1ViewModel = step1ViewModel
        binding.lifecycleOwner = this

        step1ViewModel.enableNextButton(false)
        binding.step1Back.setOnClickListener{
            findNavController().navigate(R.id.action_step1Fragment_to_mapFragment)
        }
        getCountryCodeFromCurrentLocation()

        return binding.root
    }

    @SuppressLint("MissingPermission")
    private fun getCountryCodeFromCurrentLocation() {
        lifecycleScope.launch {
            val placeFields = listOf(Place.Field.LAT_LNG)
            val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)

            val placeResponse = placesClient.findCurrentPlace(request)
            placeResponse.addOnCompleteListener{
                try {
                    if (it.isSuccessful){
                        val response = it.result
                        val latLng = response.placeLikelihoods[0].place.latLng!!
                        val address = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                        sharedViewModel.geoCountryCode = address[0].countryCode
                    } else {
                        val exception = it.exception
                        if (exception is ApiException){
                            Log.e("Step1Fragment", exception.statusCode.toString())
                        }
                    }
                } catch (e: IOException){
                } finally {
                    enableNextButton()
                }
            }
            enableNextButton()
        }
    }

    private fun enableNextButton(){
        if (sharedViewModel.geoName.isNotEmpty()){
            step1ViewModel.enableNextButton(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}