package com.example.Dendrometry.dbmshelpers

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.Dendrometry.R
import java.util.Locale

class ClassificationAdapter(private var classificationList: List<DataClassification>, context: Context) : RecyclerView.Adapter<ClassificationAdapter.ClassificationViewHolder>(){

    private val db : ClassificationDatabaseHelper = ClassificationDatabaseHelper(context)

    class ClassificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
          val treeSpecies: TextView = itemView.findViewById(R.id.treeSpecies)
          val heightResult: TextView = itemView.findViewById(R.id.heightResult)
          val diameterResult: TextView = itemView.findViewById(R.id.diameterResult)
          val volumeResult: TextView = itemView.findViewById(R.id.volumeResult)
          val diameterClass: TextView = itemView.findViewById(R.id.diameterClass)
          val dateVolumeClass: TextView = itemView.findViewById(R.id.dateVolumeClass)
          val deleteBtn: ImageView = itemView.findViewById(R.id.delBtnData)
      }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.classification_items, parent, false)
        return ClassificationViewHolder(view)
    }

    override fun getItemCount(): Int = classificationList.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ClassificationViewHolder, position: Int) {
        val classification = classificationList[position]
        holder.treeSpecies.text = "Tree Species: ${classification.treeSpecies}"
        holder.heightResult.text = "Height: ${classification.height}"
        holder.diameterResult.text = "Diameter: ${classification.diameter}"
        holder.volumeResult.text = "Volume: ${String.format(Locale.US, "%.4f", classification.volume)}m³"
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