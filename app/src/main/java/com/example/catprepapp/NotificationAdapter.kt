package com.example.catprepapp

import android.app.AlertDialog
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
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(item.timestamp)
            val formatter = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())
            holder.timestampText.text = formatter.format(date)
        } catch (e: Exception) {
            holder.timestampText.text = item.timestamp
        }

        // --- NEW: Set click listener on the entire card ---
        holder.itemView.setOnClickListener {
            // Create and show a dialog with the full content
            AlertDialog.Builder(holder.itemView.context, R.style.AlertDialog_Dark)
                .setTitle(item.title)
                .setMessage(item.body)
                .setPositiveButton("Close", null)
                .show()
        }
    }

    override fun getItemCount() = notificationList.size
}
