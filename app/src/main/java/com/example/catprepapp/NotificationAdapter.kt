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
        
        // --- Timestamp formatting ---
        try {
            // This format now matches what we save from Apps Script
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC") // Timestamps from script are UTC
            val date = parser.parse(item.timestamp)
            val formatter = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())
            holder.timestampText.text = formatter.format(date)
        } catch (e: Exception) {
            holder.timestampText.text = item.timestamp // Fallback for safety
        }

        // --- CORRECTED "SHOW MORE" LOGIC ---
        // Set initial state
        holder.bodyText.maxLines = 3
        holder.showMoreText.text = "Show More"
        holder.showMoreText.visibility = View.GONE // Hide by default

        // Use post to reliably get line count after layout
        holder.bodyText.post {
            if (holder.bodyText.lineCount > 3) {
                holder.showMoreText.visibility = View.VISIBLE
            }
        }

        holder.showMoreText.setOnClickListener {
            val isExpanded = holder.bodyText.maxLines != 3
            if (isExpanded) {
                holder.bodyText.maxLines = 3
                holder.showMoreText.text = "Show More"
            } else {
                holder.bodyText.maxLines = Integer.MAX_VALUE
                holder.showMoreText.text = "Show Less"
            }
        }
    }

    override fun getItemCount() = notificationList.size
}
