package com.example.geofenceapp.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.geofenceapp.databinding.PlacesRowLayoutBinding
import com.example.geofenceapp.util.MyDiffUtil
import com.google.android.libraries.places.api.model.AutocompletePrediction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PlaceAdapter : RecyclerView.Adapter<PlaceAdapter.MyViewHolder>() {

    var predictions = emptyList<AutocompletePrediction>()

    private val _placeId = MutableStateFlow("")
    val placeId: StateFlow<String> get()=_placeId

    class MyViewHolder( val binding: PlacesRowLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(prediction: AutocompletePrediction){
            binding.prediction = prediction
        }

        companion object{
            fun from(parent: ViewGroup): MyViewHolder{
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = PlacesRowLayoutBinding.inflate(layoutInflater, parent, false)
                return MyViewHolder(binding)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(predictions[position])

        holder.binding.rootLayout.setOnClickListener{
            Log.d("Places Adapter", "in the root layout set listener")
            setPlaceId(predictions[position].placeId)
        }
    }

    override fun getItemCount(): Int {
        return predictions.size
    }

    fun setData (newPredictions: List<AutocompletePrediction>){ //TODO: Make more efficient with extra DiffUtil class
        val myDiffUtil = MyDiffUtil(predictions, newPredictions)
        val myDiffUtilResult = DiffUtil.calculateDiff(myDiffUtil)
        predictions = newPredictions
        myDiffUtilResult.dispatchUpdatesTo(this)
//        notifyDataSetChanged()
    }

     fun setPlaceId(placeId: String){
        _placeId.value = placeId
    }
}
