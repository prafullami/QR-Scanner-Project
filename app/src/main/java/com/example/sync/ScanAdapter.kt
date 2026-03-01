package com.example.sync

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class DisplayItem(
    val deviceId: String,
    val givenTime: String,
    val takenTime: String,
    val sdMissingTime: String
)

class ScanAdapter(private var list: List<DisplayItem>) :
    RecyclerView.Adapter<ScanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val qr: TextView = view.findViewById(R.id.txtQr)
        val given: TextView = view.findViewById(R.id.txtGiven)
        val taken: TextView = view.findViewById(R.id.txtTaken)
        val missing: TextView = view.findViewById(R.id.txtMissing)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_scan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.qr.text = item.deviceId

        // GIVEN column
        holder.given.text =
            if (item.givenTime.isNotEmpty()) "✔" else ""

        // TAKEN column
        holder.taken.text =
            if (item.takenTime.isNotEmpty()) "✔" else ""

        // SD Missing column
        holder.missing.text =
            if (item.sdMissingTime.isNotEmpty()) "✔" else ""
    }


    fun updateData(newList: List<DisplayItem>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = list.size
}