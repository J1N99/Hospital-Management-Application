package com.example.hospitalmanagementapplication.doctor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hospitalmanagementapplication.R

class AdapterTimeSlots(private val timeSlots: List<String>, private val events: List<String>) : RecyclerView.Adapter<AdapterTimeSlots.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_event, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timeSlot = timeSlots[position]
        val event = events[position]

        holder.timeTextView.text = timeSlot
        holder.eventTextView.text = event
    }

    override fun getItemCount(): Int {
        return timeSlots.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val eventTextView: TextView = itemView.findViewById(R.id.eventTextView)
    }
}
