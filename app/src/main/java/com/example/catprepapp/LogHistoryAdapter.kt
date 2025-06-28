package com.example.catprepapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.catprepapp.network.LogHistoryItem
import java.text.SimpleDateFormat
import java.util.*

class LogHistoryAdapter(private val logList: List<LogHistoryItem>) :
    RecyclerView.Adapter<LogHistoryAdapter.LogViewHolder>() {

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.logDateText)
        val topicText: TextView = itemView.findViewById(R.id.logTopicText)
        val detailsText: TextView = itemView.findViewById(R.id.logDetailsText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log_history, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val item = logList[position]
        
        // Format the date string nicely
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(item.date)
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            holder.dateText.text = formatter.format(date)
        } catch (e: Exception) {
            holder.dateText.text = item.date // Fallback to raw date string
        }

        holder.topicText.text = item.topic
        holder.detailsText.text = "${item.questions} Questions  |  ${item.confidence}% Confidence"
    }

    override fun getItemCount() = logList.size
}
