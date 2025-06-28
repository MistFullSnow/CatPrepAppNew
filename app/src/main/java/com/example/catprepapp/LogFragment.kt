package com.example.catprepapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catprepapp.network.ApiClient
import com.example.catprepapp.network.LogHistoryItem
import com.example.catprepapp.network.LogRequestBody
import com.example.catprepapp.network.TopicsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogFragment : Fragment() {

    private lateinit var logHistoryRecyclerView: RecyclerView
    private lateinit var adapter: LogHistoryAdapter
    private var logHistoryList = mutableListOf<LogHistoryItem>()
    private lateinit var topicSpinner: Spinner

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find all UI components
        topicSpinner = view.findViewById(R.id.topicSpinner)
        val questionsEditText = view.findViewById<EditText>(R.id.questionsEditText)
        val confidenceSeekBar = view.findViewById<SeekBar>(R.id.confidenceSeekBar)
        val submitButton = view.findViewById<Button>(R.id.submitButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.logProgressBar)
        val confidenceValueText = view.findViewById<TextView>(R.id.confidenceValueText)
        
        // Setup RecyclerView
        logHistoryRecyclerView = view.findViewById(R.id.logHistoryRecyclerView)
        logHistoryRecyclerView.layoutManager = LinearLayoutManager(context)
        adapter = LogHistoryAdapter(logHistoryList) { logItem, position -> onDeleteClicked(logItem, position) }
        logHistoryRecyclerView.adapter = adapter

        // Setup SeekBar Listener
        confidenceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                confidenceValueText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Setup Submit Button Listener
        submitButton.setOnClickListener {
            val topic = if (topicSpinner.selectedItemPosition > 0 && topicSpinner.selectedItem.toString().startsWith("---").not()) {
                topicSpinner.selectedItem.toString()
            } else ""
            
            val questions = questionsEditText.text.toString().toIntOrNull() ?: 0
            val confidence = confidenceSeekBar.progress

            if (topic.isBlank()) {
                Toast.makeText(context, "Please select a valid topic", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val secretKey = "CATPREP123" // IMPORTANT: Replace with your key

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
                            questionsEditText.text.clear()
                            confidenceSeekBar.progress = 70
                            confidenceValueText.text = "70%"
                            topicSpinner.setSelection(0)
                            fetchLogHistory()
                        } else {
                            Toast.makeText(context, "Submission failed", Toast.LENGTH_LONG).show()
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

        // Fetch initial data for the screen
        fetchInitialData()
    }
    
    private fun fetchInitialData() {
        CoroutineScope(Dispatchers.IO).launch {
            val topics = fetchTopics()
            withContext(Dispatchers.Main) {
                if (topics != null) {
                    setupSpinner(topics)
                } else {
                    Toast.makeText(context, "Could not load topics.", Toast.LENGTH_SHORT).show()
                }
            }
            // Fetch history after topics are set up
            fetchLogHistory()
        }
    }
    
    private suspend fun fetchTopics(): TopicsResponse? {
        return try {
            val response = ApiClient.apiService.getTopics()
            if(response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun setupSpinner(topics: TopicsResponse) {
        val topicListWithHeaders = mutableListOf<String>()
        topicListWithHeaders.add("Select a Topic...") // This is a prompt
        topicListWithHeaders.add("--- QA ---")
        topicListWithHeaders.addAll(topics.qa)
        topicListWithHeaders.add("--- DILR ---")
        topicListWithHeaders.addAll(topics.dilr)
        topicListWithHeaders.add("--- VARC ---")
        topicListWithHeaders.addAll(topics.varc)
        
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, topicListWithHeaders)
        topicSpinner.adapter = spinnerAdapter
    }

    private fun fetchLogHistory() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getLogHistory()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        logHistoryList.clear()
                        logHistoryList.addAll(response.body()!!.history)
                        adapter.notifyDataSetChanged()
                    } else {
                        // Silently fail or show a non-intrusive message
                    }
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
    
    private fun onDeleteClicked(logItem: LogHistoryItem, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Log")
            .setMessage("Are you sure you want to delete this log entry?")
            .setPositiveButton("Delete") { _, _ ->
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
