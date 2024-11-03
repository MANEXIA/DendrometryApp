package com.example.myappkotlin

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class classificationAdapter(private var classificationList: List<DataClassification>, context: Context) : RecyclerView.Adapter<classificationAdapter.classificationViewHolder>(){

    private val db : ClassificationDatabaseHelper = ClassificationDatabaseHelper(context)

    class classificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
          val heightResult: TextView = itemView.findViewById(R.id.heightResult)
          val diameterResult: TextView = itemView.findViewById(R.id.diameterResult)
          val volumeResult: TextView = itemView.findViewById(R.id.volumeResult)
          val diameterClass: TextView = itemView.findViewById(R.id.diameterClass)
          val dateVolumeClass: TextView = itemView.findViewById(R.id.dateVolumeClass)
          val deleteBtn: ImageView = itemView.findViewById(R.id.delBtnData)
      }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): classificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.classification_items, parent, false)
        return classificationViewHolder(view)
    }

    override fun getItemCount(): Int = classificationList.size

    override fun onBindViewHolder(holder: classificationViewHolder, position: Int) {
        val classification = classificationList[position]
        holder.heightResult.text = "Height: ${classification.height}"
        holder.diameterResult.text = "Diameter: ${classification.diameter}"
        holder.volumeResult.text = "Volume: ${String.format("%.4f", classification.volume)}m³"
        holder.diameterClass.text = "Size: ${classification.diameterClass}"
        holder.dateVolumeClass.text = classification.date

        holder.deleteBtn.setOnClickListener{
            db.deleteClassificationItem(classification.id)
            refreshData(db.getClassifications())
            Toast.makeText(holder.itemView.context, "Item Deleted", Toast.LENGTH_SHORT).show()
        }
    }


    fun refreshData(newData: List<DataClassification>){
        classificationList = newData
        notifyDataSetChanged()
    }


}