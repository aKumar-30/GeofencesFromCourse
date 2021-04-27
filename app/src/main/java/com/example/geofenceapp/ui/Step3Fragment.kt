package com.example.geofenceapp.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.geofenceapp.R
import com.example.geofenceapp.databinding.FragmentStep3Binding
import com.example.geofenceapp.util.ExtensionFunctions.observeOnce
import com.example.geofenceapp.viewmodel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Step3Fragment : Fragment() {
    private var _binding: FragmentStep3Binding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStep3Binding.inflate(inflater, container, false)
        binding.sharedViewModel = sharedViewModel
        binding.lifecycleOwner = this

        sharedViewModel.readDefaultRadius.observeOnce(viewLifecycleOwner, Observer { defaultRadius ->
            binding.slider.value = defaultRadius
        })
        binding.step3Back.setOnClickListener{
            findNavController().navigate(R.id.action_step3Fragment_to_step2Fragment)
        }
        binding.step3Done.setOnClickListener{
            sharedViewModel.geoRadius = binding.slider.value
            sharedViewModel.geoFenceReady = true
            findNavController().navigate(R.id.action_step3Fragment_to_mapFragment)
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}