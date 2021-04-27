package com.example.geofenceapp.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.geofenceapp.R
import com.example.geofenceapp.broadcastreceiver.GeofenceBroadcastReceiver
import com.example.geofenceapp.databinding.FragmentMapsBinding
import com.example.geofenceapp.util.ExtensionFunctions.disable
import com.example.geofenceapp.util.ExtensionFunctions.enable
import com.example.geofenceapp.util.ExtensionFunctions.hide
import com.example.geofenceapp.util.ExtensionFunctions.observeOnce
import com.example.geofenceapp.util.ExtensionFunctions.show
import com.example.geofenceapp.util.Permissions
import com.example.geofenceapp.viewmodel.SharedViewModel
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener, EasyPermissions.PermissionCallbacks, GoogleMap.SnapshotReadyCallback, GoogleMap.OnMyLocationButtonClickListener{

    private var _binding: FragmentMapsBinding ?=null
    private val binding get() = _binding!!

    private val args by navArgs<MapsFragmentArgs>()

    private lateinit var map: GoogleMap

    private lateinit var circle: Circle
    private var allCircles: MutableList<Circle> = mutableListOf()

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private lateinit var geoCoder: Geocoder

    private var legallyAdd = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geoCoder = Geocoder(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =  FragmentMapsBinding.inflate(inflater, container, false)
        binding.geofenceProgressBar.enable()

        binding.addGeofenceFab.setOnClickListener{
            findNavController().navigate(R.id.action_mapsFragment_to_add_geofence_graph)
        }
        binding.viewGeoFencesFab.setOnClickListener{
            findNavController().navigate(R.id.action_mapsFragment_to_geofencesFragment)
        }
        binding.settingsFab.setOnClickListener {
            val dialog = SettingsDialog()
            dialog.show(childFragmentManager, null)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap!!
        binding.geofenceProgressBar.disable()
        when (context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {
                map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        requireContext(),
                        R.raw.mapstyledark
                    )
                )
            }
            Configuration.UI_MODE_NIGHT_NO -> {
                map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        requireContext(),
                        R.raw.mapstyle
                    )
                )
            }
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        requireContext(),
                        R.raw.mapstyle
                    )
                )
            }
        }

        map.isMyLocationEnabled = true
//        map.setPadding(0, 0,1200,0)

        map.setOnMapLongClickListener(this)
        map.uiSettings.apply {
            isMyLocationButtonEnabled = true
            isMapToolbarEnabled = false
            isZoomGesturesEnabled = true
//            isZoomControlsEnabled = true TODO: Figure out how to do this
        }
        toDoAtStart()
        observeDatabase()
        backFromGeofencesFragment()
    }

    private fun toDoAtStart() {
        if(sharedViewModel.geoFenceReady){
            sharedViewModel.geoFenceReady = false
            sharedViewModel.geoFencePrepared.value = true
            displayInfoMessage()
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(sharedViewModel.geoLatLng, decideZoomLevel()),
                1750,
                null
            )
        } else {
            sharedViewModel.readGeofences.observeOnce(viewLifecycleOwner) { geofenceEntities ->
                if (geofenceEntities.isNotEmpty()) {
                    val latLngBounds: LatLngBounds.Builder = LatLngBounds.Builder()
                    geofenceEntities.forEach { geofence ->
                        latLngBounds.include(
                            sharedViewModel.getNortheast(
                                LatLng(
                                    geofence.latitude,
                                    geofence.longitude
                                ), geofence.radius
                            )
                        )
                        latLngBounds.include(
                            sharedViewModel.getSouthwest(
                                LatLng(
                                    geofence.latitude,
                                    geofence.longitude
                                ), geofence.radius
                            )
                        )
                    }
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(latLngBounds.build(), 10),
                        null
                    )
                }
            }
        }
    }

    private fun displayInfoMessage() {
        lifecycleScope.launch { //in lifecycleScope to have a delay
            binding.infoMessageTextView.show()
            delay(2000)
            binding.infoMessageTextView.animate().alpha(0F).duration = 500
            delay(1000)
            binding.infoMessageTextView.hide()
        }
    }

    private fun decideZoomLevel(): Float {
        return when (sharedViewModel.geoRadius){
            500f ->
                15f
            2f ->
                13f
            5f ->
                11f
            else ->
                10f
        }
    }

    private fun observeDatabase() {
        sharedViewModel.readGeofences.observe(viewLifecycleOwner){ geofenceEntities->
            map.clear()  //not efficient, use a list to keep track of what has been done already instead
            geofenceEntities.forEach{ geofence->
                drawMarker(LatLng(geofence.latitude, geofence.longitude), geofence.name)
                drawCircle(LatLng(geofence.latitude, geofence.longitude), geofence.radius, geofence.geoId)
            }
        }
        GeofenceBroadcastReceiver.geofenceChanges.observe(viewLifecycleOwner) {
            Log.d("MapsFragment", "observing changes and received double: $it")
            if (it!=0.0){
                sharedViewModel.readGeofencesWithQuery(it)
                sharedViewModel.readGeofencesWithQuery?.observeOnce(viewLifecycleOwner, Observer{ entities->
                    //change color of circle
                    for (cCircle in allCircles) {
                        if (cCircle.tag == entities[0].geoId){
                            when (GeofenceBroadcastReceiver.currentGeofenceChange) {
                                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                                    cCircle.fillColor = getColor(R.color.background_in_geofence)
                                    cCircle.strokeColor= getColor(R.color.stroke_in_geofence)
                                }
                                Geofence.GEOFENCE_TRANSITION_EXIT ->{
                                    cCircle.fillColor = getColor(R.color.background_out_geofence)
                                    cCircle.strokeColor= getColor(R.color.stroke_out_geofence)
                                }
                                Geofence.GEOFENCE_TRANSITION_DWELL ->{
                                    cCircle.fillColor = getColor(R.color.background_dwell_geofence)
                                    cCircle.strokeColor= getColor(R.color.stroke_dwell_geofence)
                                }
                                else ->{
                                    cCircle.fillColor = getColor(R.color.background_out_geofence)
                                    cCircle.strokeColor= getColor(R.color.stroke_out_geofence)
                                }
                            }
                        }
                    }
                })
            }
        }
    }

    private fun backFromGeofencesFragment() {
        if (args.geofenceEntity != null){
            val selectedGeofence = LatLng(
                args.geofenceEntity!!.latitude,
                args.geofenceEntity!!.longitude
            )
            zoomToGeofence(selectedGeofence, args.geofenceEntity!!.radius)
        }
    }

    override fun onMapLongClick(location: LatLng?) {
        if (Permissions.hasBackgroundLocationPosition(requireContext())){
            if (location != null){
                if (sharedViewModel.geoFencePrepared.value == true){
                    Log.d("MapsFragment", "in geofence prepared")
                    setUpGeofence(location)
                }
                else if (!legallyAdd) {
                    Toast.makeText(context, "Error: please wait until next geofence is added", Toast.LENGTH_SHORT).show()
                }
                else{
                        Toast.makeText(
                            context,
                            "Creating a new geofence",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("MapsFragment", "in geofence not prepared")
                        sharedViewModel.setVariablesForPreset(geoCoder, location, viewLifecycleOwner)
                        sharedViewModel.geoFencePrepared.observeOnce(viewLifecycleOwner, {
                            if (it){
                                Log.d("MapsFragment","in observering thing about to call the good stuff")
                                setUpGeofence(location)
                            } else {
                                Toast.makeText(context, "Error: please try another location", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
        }
        else {
            Permissions.requestBackgroundLocationPosition(this)
        }
    }
    private fun setUpGeofence(location: LatLng) {
        lifecycleScope.launch {
            if (sharedViewModel.checkDeviceLocationSettings(requireContext())){
                binding.viewGeoFencesFab.disable()
                binding.addGeofenceFab.disable()
                binding.settingsFab.disable()
                binding.geofenceProgressBar.enable()
                legallyAdd = false
                map.uiSettings.apply { //so nobody can move map before screenshot
                    isZoomGesturesEnabled = false
                    isMyLocationButtonEnabled = false
                    isScrollGesturesEnabled = false
                    isScrollGesturesEnabledDuringRotateOrZoom = false
                }

                drawMarker(location, sharedViewModel.geoName)
                drawCircle(location, sharedViewModel.geoRadius, sharedViewModel.geoId)
                zoomToGeofence(circle.center, circle.radius.toFloat())

                delay(2500)
                map.snapshot(this@MapsFragment)

                map.uiSettings.apply { //so nobody can move map before screenshot
                    isZoomGesturesEnabled = true
                    isMyLocationButtonEnabled = true
                    isScrollGesturesEnabled = true
                    isScrollGesturesEnabledDuringRotateOrZoom = true
                }

                delay(2000)
                sharedViewModel.addGeofenceToDatabase(location)

                delay(2000)
                sharedViewModel.startGeofence(location.latitude, location.longitude)

                sharedViewModel.resetSharedValues()
                binding.viewGeoFencesFab.enable()
                binding.addGeofenceFab.enable()
                binding.settingsFab.enable()
                binding.geofenceProgressBar.disable()

                legallyAdd = true
            } else {
                Toast.makeText(
                    context,
                    "Enable location settings",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun drawMarker(location: LatLng, name: String) {
        map.addMarker(
            MarkerOptions().position(location).title(name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
    }

    private fun drawCircle(location: LatLng, radius: Float, geoId: Long) {
        circle = map.addCircle(
            CircleOptions().center(location).radius(radius.toDouble())
                .strokeColor(getColor(R.color.stroke_out_geofence))
                .fillColor(getColor(R.color.background_out_geofence))
        )
        circle.tag = geoId
        allCircles.add(circle)
    }

    private fun getColor(color: Int): Int {
        return ContextCompat.getColor(requireContext(), color)
    }

    private fun zoomToGeofence(center: LatLng, radius: Float) {
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                sharedViewModel.getBounds(center, radius), 10
            ), 1000, null
        )
    }

    override fun onSnapshotReady(snapshot: Bitmap?) {
        sharedViewModel.geoSnapshot = snapshot
    }

    override fun onMyLocationButtonClick(): Boolean {
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms[0])){
            SettingsDialog.Builder(requireActivity()).build().show()
        } else {
            Permissions.requestBackgroundLocationPosition(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        toDoAtStart()
        Toast.makeText(
            context,
            "Permission granted. Long press to add Geofence",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}