package com.example.geofenceapp.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.geofenceapp.data.GeofenceEntity
import com.example.geofenceapp.databinding.GeofencesRowLayoutBinding
import com.example.geofenceapp.ui.GeofencesFragmentDirections
import com.example.geofenceapp.util.MyDiffUtil
import com.example.geofenceapp.viewmodel.SharedViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class GeofencesAdapter (private val sharedViewModel: SharedViewModel): RecyclerView.Adapter<GeofencesAdapter.MyViewHolder>() {

    var geofenceEntities = emptyList<GeofenceEntity>()

    val swipeHandler = object : SwipeToDeleteCallback(sharedViewModel.app) {
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            removeItem(viewHolder as MyViewHolder, viewHolder.adapterPosition)
        }
    }
    class MyViewHolder( val binding: GeofencesRowLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(geofenceEntity: GeofenceEntity){
            binding.geofencesEntity = geofenceEntity
            binding.executePendingBindings()
        }

        companion object{
            fun from(parent: ViewGroup): MyViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = GeofencesRowLayoutBinding.inflate(layoutInflater, parent, false)
                return MyViewHolder(binding)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(geofenceEntities[position])

//        holder.binding.deleteImageView.setOnClickListener{
//            removeItem(holder, position)
//        }
        holder.binding.snapshotImageView.setOnClickListener{
            val action = GeofencesFragmentDirections.actionGeofencesFragmentToMapsFragment(geofenceEntities[position])
            holder.itemView.findNavController().navigate(action)
        }
    }

    private fun removeItem(holder: MyViewHolder, position: Int) {
        sharedViewModel.viewModelScope.launch {
            val geofenceStopped =
                sharedViewModel.stopGeofence(listOf(geofenceEntities[position].geoId))
            if (geofenceStopped){
                sharedViewModel.deleteGeofence(geofenceEntities[position])
                sharedViewModel.geofenceRemoved = true
                showSnackBar(holder, geofenceEntities[position])
            } else {
                Log.d("GeofencesAdapter", "geofence NOT REMOVED")
            }
        }
    }

    private fun showSnackBar(holder: MyViewHolder, removedItems: GeofenceEntity) {
        Snackbar.make(
            holder.itemView,
            "Removed" + removedItems.name,
            Snackbar.LENGTH_LONG
        ).setAction("Undo"){
            sharedViewModel.insertGeofence(removedItems)
            sharedViewModel.startGeofence(
                removedItems.latitude,
                removedItems.longitude
            )
            sharedViewModel.geofenceRemoved = false
        }.show()
    }


    override fun getItemCount(): Int {
        return geofenceEntities.size
    }

    fun setData (newGeofenceEntities: List<GeofenceEntity>){ //TODO: Make more efficient with extra DiffUtil class
        val myDiffUtil = MyDiffUtil(geofenceEntities, newGeofenceEntities)
        val myDiffUtilResult = DiffUtil.calculateDiff(myDiffUtil)
        geofenceEntities = newGeofenceEntities
        myDiffUtilResult.dispatchUpdatesTo(this)
//        notifyDataSetChanged()
    }
}
