package com.example.catprepapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // <-- NEW IMPORT
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.catprepapp.network.LogHistoryItem
import java.text.SimpleDateFormat
import java.util.*

// --- MODIFIED CONSTRUCTOR: It now accepts a function to handle deletion ---
class LogHistoryAdapter(
    private val logList: MutableList<LogHistoryItem>,
    private val onDeleteClick: (logItem: LogHistoryItem, position: Int) -> Unit
) : RecyclerView.Adapter<LogHistoryAdapter.LogViewHolder>() {

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.logDateText)
        val topicText: TextView = itemView.findViewById(R.id.logTopicText)
        val detailsText: TextView = itemView.findViewById(R.id.logDetailsText)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton) // <-- Find the button

        // --- NEW: Set up the click listener ---
        init {
            deleteButton.setOnClickListener {
                // When clicked, call the lambda function passed from the fragment
                onDeleteClick(logList[adapterPosition], adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log_history, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val item = logList[position]
        
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(item.date)
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            holder.dateText.text = formatter.format(date)
        } catch (e: Exception) {
            holder.dateText.text = item.date
        }

        holder.topicText.text = item.topic
        holder.detailsText.text = "${item.questions} Questions  |  ${item.confidence}% Confidence"
    }

    override fun getItemCount() = logList.size
    
    // --- NEW: Function to remove an item from the list and notify the UI ---
    fun removeItem(position: Int) {
        logList.removeAt(position)
        notifyItemRemoved(position)
    }
}
