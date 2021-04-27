package com.example.geofenceapp.ui

import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.geofenceapp.adapters.GeofencesAdapter
import com.example.geofenceapp.databinding.FragmentGeofencesBinding
import com.example.geofenceapp.viewmodel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GeofencesFragment : Fragment(){
    private var _binding: FragmentGeofencesBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()

    private val geofenceAdapter by lazy{ GeofencesAdapter(sharedViewModel) }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeofencesBinding.inflate(inflater, container, false)
        binding.sharedViewModel = sharedViewModel

        setUpToolbar()
        setUpRecyclerView()
        sharedViewModel.readGeofences.observe (viewLifecycleOwner, {
            geofenceAdapter.setData(it)
//            binding.geofencesRecyclerView.scheduleLayoutAnimation()
        })

        return binding.root
    }

    private fun setUpToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setUpRecyclerView() {
        binding.geofencesRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.geofencesRecyclerView.adapter = geofenceAdapter

        val itemTouchHelper = ItemTouchHelper(geofenceAdapter.swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.geofencesRecyclerView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}