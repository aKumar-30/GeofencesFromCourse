package com.example.geofenceapp.adapters

import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.geofenceapp.data.GeofenceEntity
import com.example.geofenceapp.data.GeofenceUpdate
import com.example.geofenceapp.databinding.GeofencesRowLayoutBinding
import com.example.geofenceapp.ui.GeofencesFragmentDirections
import com.example.geofenceapp.util.MyDiffUtil
import com.example.geofenceapp.viewmodel.SharedViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.GlobalScope
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

//        holder.binding.nameEditTextView.doOnTextChanged { text, start, before, count -> //TODO: Make more efficient
//            if (text.toString()!=geofenceEntities[position].name){
//                Log.d("GeofencesAdapter", "in on text chanfed interestng")

//            }
//        }

        holder.binding.nameEditTextView.setOnEditorActionListener( object: TextView.OnEditorActionListener { //TODO: Make sure it covers all ways
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event != null &&
                    event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event == null || !event.isShiftPressed){
                        holder.binding.nameEditTextView.inputType = InputType.TYPE_NULL
                        val text = holder.binding.nameEditTextView.text.toString()
                        if (text != geofenceEntities[position].name){
                            val geofenceUpdate = GeofenceUpdate()
                            geofenceUpdate.id = geofenceEntities[position].id
                            geofenceUpdate.name = holder.binding.nameEditTextView.text.toString()
                            sharedViewModel.updateGeofence(geofenceUpdate)
                            geofenceEntities[position].name = text
                            // Only runs if there is a view that is currently focused
                        }
                        return true // consume.
                    }
                }
                return false // pass on to other listeners.
            }
        }
        )
    }
    private fun removeItem(holder: MyViewHolder, position: Int) {
        GlobalScope.launch{
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
            "Removed: " + removedItems.name,
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
