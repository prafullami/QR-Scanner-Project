package com.example.sync
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sync.ScanRecord
import android.widget.TextView
import android.widget.CheckBox

class ScanAdapter(private val list: List<ScanRecord>) :
    RecyclerView.Adapter<ScanAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val qr = view.findViewById<TextView>(R.id.txtQr)
        val given = view.findViewById<TextView>(R.id.txtGiven)
        val taken = view.findViewById<TextView>(R.id.txtTaken)
        val missing = view.findViewById<CheckBox>(R.id.checkMissing)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_scan, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

//        holder.qr.text = item.qr
//        holder.given.visibility = if (item.given) View.VISIBLE else View.INVISIBLE
//        holder.taken.visibility = if (item.taken) View.VISIBLE else View.INVISIBLE
//        holder.missing.isChecked = item.sdMissing
    }
}