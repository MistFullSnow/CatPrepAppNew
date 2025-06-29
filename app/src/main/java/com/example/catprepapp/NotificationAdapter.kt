package com.example.catprepapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.catprepapp.network.NotificationItem
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(private val notificationList: List<NotificationItem>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.notificationTitleText)
        val bodyText: TextView = itemView.findViewById(R.id.notificationBodyText)
        val timestampText: TextView = itemView.findViewById(R.id.notificationTimestampText)
        val showMoreText: TextView = itemView.findViewById(R.id.showMoreText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
    val item = notificationList[position]
    holder.titleText.text = item.title
    holder.bodyText.text = item.body
    
    // --- Timestamp formatting (no changes here) ---
    try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(item.timestamp)
        val formatter = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())
        holder.timestampText.text = formatter.format(date)
    } catch (e: Exception) {
        holder.timestampText.text = item.timestamp
    }

    // --- NEW AND CORRECTED "SHOW MORE" LOGIC ---
    // We declare a variable to track the state for this specific card
    var isExpanded = false 
    // Set the initial state
    holder.bodyText.maxLines = 3
    holder.showMoreText.text = "Show More"

    // Use post to wait for the layout to be drawn, so we can get the line count
    holder.bodyText.post {
        // Only show the "Show More" button if the text is actually truncated
        if (holder.bodyText.lineCount > 3) {
            holder.showMoreText.visibility = View.VISIBLE
        } else {
            holder.showMoreText.visibility = View.GONE
        }
    }

    // Set a fresh click listener every time
    holder.showMoreText.setOnClickListener {
        isExpanded = !isExpanded // Flip the state
        if (isExpanded) {
            holder.bodyText.maxLines = Integer.MAX_VALUE // Show all lines
            holder.showMoreText.text = "Show Less"
        } else {
            holder.bodyText.maxLines = 3 // Collapse back to 3 lines
            holder.showMoreText.text = "Show More"
        }
    }
}

    override fun getItemCount() = notificationList.size
}
