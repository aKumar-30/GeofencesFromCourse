package com.example.geofenceapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.geofenceapp.databinding.SettingsDialogBinding
import com.example.geofenceapp.util.ExtensionFunctions.observeOnce
import com.example.geofenceapp.viewmodel.SharedViewModel

class SettingsDialog: AppCompatDialogFragment(){
    private var _binding: SettingsDialogBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsDialogBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.okButton.setOnClickListener {
            saveValues()
            dismiss()
        }
        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        readValues()
    }

    private fun readValues() {
        sharedViewModel.readDefaultRadius.observeOnce(viewLifecycleOwner, Observer { defaultRadius ->
            binding.slider2.value = defaultRadius
        })
    }
    private fun saveValues() {
        sharedViewModel.saveDefaultRadius(binding.slider2.value)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}