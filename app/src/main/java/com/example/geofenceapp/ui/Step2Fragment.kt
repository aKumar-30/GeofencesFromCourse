package com.example.geofenceapp.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.geofenceapp.R
import com.example.geofenceapp.adapters.PlaceAdapter
import com.example.geofenceapp.databinding.FragmentStep2Binding
import com.example.geofenceapp.util.ExtensionFunctions.hide
import com.example.geofenceapp.util.ExtensionFunctions.show
import com.example.geofenceapp.util.NetworkListener
import com.example.geofenceapp.viewmodel.SharedViewModel
import com.example.geofenceapp.viewmodel.Step2ViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class Step2Fragment : Fragment() {
    private var _binding: FragmentStep2Binding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val step2ViewModel: Step2ViewModel by viewModels()
    private lateinit var placesClient: PlacesClient
    private lateinit var networkListener: NetworkListener
    private val predictionAdapter by lazy{ PlaceAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Places.initialize(requireContext(), getString(R.string.google_maps_key))
        placesClient = Places.createClient(requireContext())
    }
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStep2Binding.inflate(inflater, container, false)
        binding.sharedViewModel = sharedViewModel
        binding.step2ViewModel = step2ViewModel
        binding.lifecycleOwner = this

        checkInternetConnection()

        binding.predictionsRecyclerView.adapter = predictionAdapter
        binding.predictionsRecyclerView.layoutManager = LinearLayoutManager(context)

        if (sharedViewModel.geoCitySelected)
            step2ViewModel.enableNextButton(true)
        binding.geofenceNameLocationEt.doOnTextChanged{ text, _, _, _ ->
            step2ViewModel.enableNextButton(false)
            getPlaces(text)
        }
        binding.geofenceNameLocationEt.setOnKeyListener(object:View.OnKeyListener{
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (keyCode == KeyEvent.KEYCODE_ENTER){
                    Log.d("Step2Fragment", "inKeycode thing")
                    if (predictionAdapter.predictions.isNotEmpty())
                        predictionAdapter.setPlaceId(predictionAdapter.predictions[0].placeId)
                    return true
                }
                return false
            }
        })

        binding.step2Back.setOnClickListener{
            findNavController().navigate(R.id.action_step2Fragment_to_step1Fragment)
        }
        binding.step2Next.setOnClickListener{
            findNavController().navigate(R.id.action_step2Fragment_to_step3Fragment)
        }

        subscribeToObservers()
        return binding.root
    }

    private fun subscribeToObservers(){
        lifecycleScope.launch {
            predictionAdapter.placeId.collectLatest {placeId ->
                if(placeId.isNotEmpty())
                    onCitySelected(placeId)
            }
        }
    }

    private fun onCitySelected(placeId: String) {
        Log.d("Step2Fragment", "in onCitySelected")
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.LAT_LNG,
            Place.Field.NAME,
            Place.Field.ADDRESS
        )

        val request = FetchPlaceRequest.builder(placeId, placeFields).build()
        placesClient.fetchPlace(request)
            .addOnSuccessListener {
                sharedViewModel.geoLatLng = it.place.latLng!!
                sharedViewModel.geoLocationName = it.place.name!!
                sharedViewModel.geoCitySelected = true
                binding.geofenceNameLocationEt.setText(sharedViewModel.geoLocationName)
                binding.geofenceNameLocationEt.setSelection(sharedViewModel.geoLocationName.length)
                binding.predictionsRecyclerView.hide()
                step2ViewModel.enableNextButton(true)
                Log.d("Step2Fragment", "geolatlng is ${sharedViewModel.geoLatLng}")
                Log.d("Step2Fragment", "locationName is ${sharedViewModel.geoLocationName}")
                Log.d("Step2Fragment", "geoCitySelected is ${sharedViewModel.geoCitySelected}")
            }
            .addOnFailureListener{
                Log.e("Step2Fragment", it.message.toString())
            }
    }

    private fun handleNextButton() {
        step2ViewModel.enableNextButton(false)
    }

    private fun getPlaces(text: CharSequence?) {
        if(sharedViewModel.checkDeviceLocationSettings(requireContext())){
            lifecycleScope.launch {
                Log.d("Step2Fragment", "getPlaces: in getPlaces currently with text $text")
                if (text.isNullOrEmpty()){
                    predictionAdapter.setData(emptyList())
                } else {
                    binding.predictionsRecyclerView.show()
                    val token = AutocompleteSessionToken.newInstance()

                    val request = FindAutocompletePredictionsRequest.builder()
                        .setCountries(sharedViewModel.geoCountryCode)
//                        .setTypeFilter(TypeFilter.CITIES)
                        .setSessionToken(token)
                        .setQuery(text.toString())
                        .build()
                    placesClient.findAutocompletePredictions(request)
                        .addOnSuccessListener { response->
                            Log.d("Step2Fragment", "in findAutocompletePredictions success listener with ${response.autocompletePredictions}")
                            predictionAdapter.setData(response.autocompletePredictions)
                            binding.predictionsRecyclerView.scheduleLayoutAnimation()
                        }
                        .addOnFailureListener{ exception ->
                            if (exception is ApiException){
                                Log.e("Step2Fragment", exception.statusCode.toString())
                            }
                        }
                }
            }
        } else {
            Toast.makeText(context, "Please Enable location settings", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun checkInternetConnection(){
        lifecycleScope.launch {
            networkListener = NetworkListener()
            networkListener.checkNetworkAvailability(requireContext())
                .collect{ online->
                    step2ViewModel.setInternetAvailable(online)
                    if(online && sharedViewModel.geoCitySelected){
                        step2ViewModel.enableNextButton(true)
                    } else {
                        step2ViewModel.enableNextButton(false)
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}