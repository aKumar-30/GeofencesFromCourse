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
import com.example.geofenceapp.adapters.InfoWindowAdapter
import com.example.geofenceapp.broadcastreceiver.GeofenceBroadcastReceiver
import com.example.geofenceapp.data.GeofenceEntity
import com.example.geofenceapp.data.GeofenceUpdateName
import com.example.geofenceapp.databinding.FragmentMapsBinding
import com.example.geofenceapp.util.Constants
import com.example.geofenceapp.util.ExtensionFunctions.disable
import com.example.geofenceapp.util.ExtensionFunctions.enable
import com.example.geofenceapp.util.ExtensionFunctions.hide
import com.example.geofenceapp.util.ExtensionFunctions.observeOnce
import com.example.geofenceapp.util.ExtensionFunctions.reverseParse
import com.example.geofenceapp.util.ExtensionFunctions.show
import com.example.geofenceapp.util.Permissions
import com.example.geofenceapp.viewmodel.SharedViewModel
import com.google.android.gms.location.Geofence
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.android.PolyUtil
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsRoute
import com.google.maps.model.TravelMode
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMapLongClickListener, EasyPermissions.PermissionCallbacks,
    GoogleMap.SnapshotReadyCallback, GoogleMap.OnMyLocationButtonClickListener, InfoWindowAdapter.OnShowRoutePressedCallback{

    private var _binding: FragmentMapsBinding ?=null
    private val binding get() = _binding!!
    private val args by navArgs<MapsFragmentArgs>()
    private lateinit var map: GoogleMap
    private var currentLocation = LatLng(Constants.DEFAULT_LATITUDE, Constants.DEFAULT_LONGITUDE)

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
//        sharedViewModel.readValues(viewLifecycleOwner) //initializes value in the flow. without it,
//        // the radius autmatically is the default radius of 500f
        sharedViewModel.readDefaultRadius.observeOnce(
            viewLifecycleOwner,
            Observer { defaultRadius ->
                val x = defaultRadius
            })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission", "PotentialBehaviorOverride")
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
        map.setInfoWindowAdapter(InfoWindowAdapter(requireActivity(), this))
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

    private fun createRoute(location: LatLng) {
        Log.d("MapsFragment", "locationL latiude ${currentLocation.latitude} longitude ${currentLocation.longitude}")
        val origin = com.google.maps.model.LatLng(currentLocation.latitude, currentLocation.longitude)
        val dest = com.google.maps.model.LatLng(location.latitude, location.longitude)

        val apiRequest = DirectionsApi.newRequest(getGeoContext())
            .origin(origin)
            .destination(dest)
            .mode(TravelMode.DRIVING)
            .setCallback(object : com.google.maps.PendingResult.Callback<DirectionsResult> {
                override fun onResult(result: DirectionsResult) {
                    activity?.runOnUiThread {
                        if (result.routes.isNotEmpty() && result.routes[0]!=null) {
                            drawRoute(result.routes[0])
                            Log.d("Maps Fragment", "Call Success")
                        }
                    }
//                    val directionsRenderer = Renderer(map, context, MarkerManager(map), PolygonManager(map), PolylineManager(map), GroundOverlayManager(map), null)
//                    directionsRenderer.map = map
                }
                override fun onFailure(e: Throwable) {
                    Log.d("Maps Fragment", "Call fail + $e")
                }
            })
//        val directionsRenderer = Renderer(map, context, MarkerManager(map), PolygonManager(map), PolylineManager(map), GroundOverlayManager(map), null)
//        directionsRenderer.map = map
    }

    override fun onShowBackPressed(latLngEncoded: String) {
        sharedViewModel.getCurrentLocation(requireActivity())
        sharedViewModel.currentLocation.observe(viewLifecycleOwner, {
            Log.d("MapsFragment", "currentLocation observing")
            currentLocation = it
            val endLocation = reverseParse(latLngEncoded)
            if (endLocation != null)
                createRoute(endLocation)
        })
    }

    private var visibleRouteLine: Polyline?  = null
    private var visibleMarker: Marker? = null
    private fun drawRoute(directionsRoute: DirectionsRoute) {
        //delete previous route
        visibleMarker?.remove()
        visibleRouteLine?.remove()

        val decodedLine = PolyUtil.decode(directionsRoute.overviewPolyline.encodedPath)
        visibleRouteLine = map.addPolyline(
            PolylineOptions().addAll(decodedLine)
                .color(ContextCompat.getColor(requireContext(), R.color.blue_700))
                .geodesic(true)
                .width(12f))
        visibleMarker = map.addMarker(
            MarkerOptions().position(decodedLine[0])
                .draggable(false)
                .title("start")
        )
    }

    private fun getGeoContext() : GeoApiContext {
        return GeoApiContext.Builder()
            .apiKey(getString(R.string.google_maps_key))
            .build()
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
//            500f ->
//                15f
//            2f ->
//                13f
//            5f ->
//                11f
//            else ->
//                10f
            500f ->
                15f
            2000f - 4900f ->
                13f
            5000f - 6000f ->
                12f
            else ->
                10f
        }
    }

    private fun observeDatabase() {
        sharedViewModel.readGeofences.observeOnce(viewLifecycleOwner){ geofenceEntities->
            map.clear()  //not efficient, use a list to keep track of what has been done already instead
            geofenceEntities.forEach{ geofence->
                drawMarker(LatLng(geofence.latitude, geofence.longitude), geofence.name)
                drawCircle(
                    LatLng(geofence.latitude, geofence.longitude),
                    geofence.radius,
                    geofence.geoId
                )
            }
        }
        GeofenceBroadcastReceiver.geofenceChanges.observe(viewLifecycleOwner) {
            Log.d("MapsFragment", "observing changes and received double: $it")
            if (it!=0.0.toLong()){
                val x = GeofenceBroadcastReceiver.currentGeofenceChange
                sharedViewModel.readGeofencesWithQuery(it)
                sharedViewModel.readGeofencesWithQuery?.observeOnce(
                    viewLifecycleOwner,
                    Observer { entities ->
                        if (entities.isNotEmpty()) {
                            changeCircleColors(entities[0].geoId, x)
                            Log.d("MapsFragment", "between calls: ${entities[0].geoId}")
                            when (x) {
                                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                                    Log.d("MapsFragment", "going to updateEnter")
                                    updateEnter(entities[0])
                                }
                                Geofence.GEOFENCE_TRANSITION_DWELL -> {
                                    Log.d("MapsFragment", "going to updateDwell")
                                    updateDwell(entities[0])
                                }
                            }
                        }
                    })
            }
        }
    }

    private fun changeCircleColors(tag: Long, x: Int?) {
        //change color of circle
        for (cCircle in allCircles) {
            Log.d("MapsFragment", "there is 1 circle in cCircle")
            if (cCircle.tag == tag){
                Log.d("MapsFragment", "there is 1 circle with matchning tag in cCircle")
                when (x) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        Log.d("MapsFragment", "in enter")
                        cCircle.fillColor = getColor(R.color.background_in_geofence)
                        cCircle.strokeColor = getColor(R.color.stroke_in_geofence)
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        Log.d("MapsFragment", "in exit")
                        cCircle.fillColor = getColor(R.color.background_out_geofence)
                        cCircle.strokeColor = getColor(R.color.stroke_out_geofence)
                    }
                    Geofence.GEOFENCE_TRANSITION_DWELL -> {
                        Log.d("MapsFragment", "in dwell")
                        cCircle.fillColor = getColor(R.color.background_dwell_geofence)
                        cCircle.strokeColor = getColor(R.color.stroke_dwell_geofence)
                    }
                }
            }
        }
    }

    private fun updateEnter(entity: GeofenceEntity) {
        val geofenceUpdate = GeofenceUpdateName()
        geofenceUpdate.id = entity.id.toLong()
        val x = entity.numberEnters++
        geofenceUpdate.name = entity.name
        geofenceUpdate.enters = x
        geofenceUpdate.dwells = entity.numberDwells
        sharedViewModel.updateGeofenceName(geofenceUpdate)
    }

    private fun updateDwell(entity: GeofenceEntity) {
        val geofenceUpdate = GeofenceUpdateName()
        geofenceUpdate.id = entity.id.toLong()
        geofenceUpdate.name = entity.name
        geofenceUpdate.enters = entity.numberEnters
        val x = entity.numberDwells++
        geofenceUpdate.dwells = x
        sharedViewModel.updateGeofenceName(geofenceUpdate)
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

    override fun onMapLongClick(location: LatLng) {
        if (Permissions.hasBackgroundLocationPosition(requireContext()) && activity!=null){
                if (!legallyAdd) {
                    Log.d("MapsFragment", "legallyAdd: $legallyAdd")
                    Toast.makeText(
                        context,
                        "Error: Finishing previous geofence",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else if (sharedViewModel.geoFencePrepared.value == true){
                    setUpGeofence(location)
                }
                else{
                    legallyAdd = false
                        Toast.makeText(
                            context,
                            "Creating a new geofence",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("MapsFragment", "in geofence not prepared")
                        sharedViewModel.setVariablesForPreset(geoCoder, location)
                        if (sharedViewModel.geoFencePrepared.value == true) {
                            setUpGeofence(location)
                        } else {
                            Toast.makeText(
                                context,
                                "Error: please try another location",
                                Toast.LENGTH_SHORT
                            ).show()
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
                map.uiSettings.apply { //so nobody can move map before screenshot
                    isZoomGesturesEnabled = false
                    isMyLocationButtonEnabled = false
                    isScrollGesturesEnabled = false
                    isScrollGesturesEnabledDuringRotateOrZoom = false
                }

                drawMarker(location, sharedViewModel.geoName)
                drawCircle(location, sharedViewModel.geoRadius, sharedViewModel.geoId)

                Log.d("MapsFragment", "radius is: ${sharedViewModel.geoRadius}")
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
        val descriptionParsed: String = "${location.latitude}, ${location.longitude}"
        map.addMarker(
            MarkerOptions().position(location).title(name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .snippet(descriptionParsed)
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