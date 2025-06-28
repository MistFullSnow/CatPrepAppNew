package com.example.catprepapp

import android.app.AlertDialog // <-- NEW IMPORT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catprepapp.network.ApiClient
import com.example.catprepapp.network.LogHistoryItem // <-- NEW IMPORT
import com.example.catprepapp.network.LogRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogFragment : Fragment() {

    private lateinit var logHistoryRecyclerView: RecyclerView
    private lateinit var adapter: LogHistoryAdapter // <-- Hold a reference to the adapter
    private var logHistoryList = mutableListOf<LogHistoryItem>() // <-- Hold the data list

    // ... onCreateView remains the same ...
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // --- Setup RecyclerView first ---
        logHistoryRecyclerView = view.findViewById(R.id.logHistoryRecyclerView)
        logHistoryRecyclerView.layoutManager = LinearLayoutManager(context)
        // Create the adapter instance, passing the onDeleteClicked function
        adapter = LogHistoryAdapter(logHistoryList) { logItem, position ->
            onDeleteClicked(logItem, position)
        }
        logHistoryRecyclerView.adapter = adapter

        // --- Find other views ---
        val topicEditText = view.findViewById<EditText>(R.id.topicEditText)
        // ... (find other form views: questionsEditText, confidenceSeekBar, etc.) ...
        val questionsEditText = view.findViewById<EditText>(R.id.questionsEditText)
        val confidenceSeekBar = view.findViewById<SeekBar>(R.id.confidenceSeekBar)
        val submitButton = view.findViewById<Button>(R.id.submitButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.logProgressBar)
        val confidenceValueText = view.findViewById<TextView>(R.id.confidenceValueText)
        
        // ... (The existing SeekBar and SubmitButton listener code remains the same) ...
        // ... I'm omitting it here for brevity, but it should be present in your file ...
        confidenceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                confidenceValueText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        submitButton.setOnClickListener {
            val topic = topicEditText.text.toString()
            val questions = questionsEditText.text.toString().toIntOrNull() ?: 0
            val confidence = confidenceSeekBar.progress

            if (topic.isBlank()) {
                Toast.makeText(context, "Please enter a topic", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val secretKey = "CATPREP123" // IMPORTANT: Replace

            val requestBody = LogRequestBody(secret = secretKey, topic = topic, questions = questions, confidence = confidence)
            
            submitButton.isEnabled = false
            progressBar.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = ApiClient.apiService.submitLog(requestBody)
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        submitButton.isEnabled = true
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Log submitted successfully!", Toast.LENGTH_LONG).show()
                            topicEditText.text.clear()
                            questionsEditText.text.clear()
                            confidenceSeekBar.progress = 70
                            confidenceValueText.text = "70%"
                            fetchLogHistory()
                        } else {
                            Toast.makeText(context, "Submission failed: ${response.code()}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        submitButton.isEnabled = true
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }


        // --- Initial fetch ---
        fetchLogHistory()
    }

    private fun fetchLogHistory() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getLogHistory()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        logHistoryList.clear()
                        logHistoryList.addAll(response.body()!!.history)
                        adapter.notifyDataSetChanged() // Notify adapter of new data
                    } else {
                        Toast.makeText(context, "Failed to load history", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                     Toast.makeText(context, "Error fetching history: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // --- NEW DELETE FUNCTION ---
    private fun onDeleteClicked(logItem: LogHistoryItem, position: Int) {
        // Show a confirmation dialog
        AlertDialog.Builder(context)
            .setTitle("Delete Log")
            .setMessage("Are you sure you want to delete this log entry?")
            .setPositiveButton("Delete") { _, _ ->
                // User clicked Delete, proceed with API call
                performDelete(logItem, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDelete(logItem: LogHistoryItem, position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.deleteLog(rowId = logItem.id)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Log deleted", Toast.LENGTH_SHORT).show()
                        // Remove the item from the list in the UI
                        adapter.removeItem(position)
                    } else {
                        Toast.makeText(context, "Deletion failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
