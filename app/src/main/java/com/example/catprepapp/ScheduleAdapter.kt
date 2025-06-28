package com.example.catprepapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.catprepapp.network.ScheduleItem

class ScheduleAdapter(private val scheduleList: List<ScheduleItem>) :
    RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val topicTextView: TextView = itemView.findViewById(R.id.topicTextView)
        val notesTextView: TextView = itemView.findViewById(R.id.notesTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val item = scheduleList[position]
        holder.timeTextView.text = item.time
        holder.topicTextView.text = "Topic: ${item.topic}"
        holder.notesTextView.text = "Notes: ${item.notes}"
    }

    override fun getItemCount() = scheduleList.size
}
