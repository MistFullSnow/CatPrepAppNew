package com.example.catprepapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catprepapp.network.ApiClient
import com.example.catprepapp.network.LogRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogFragment : Fragment() {

    private lateinit var logHistoryRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Find all views ---
        val topicEditText = view.findViewById<EditText>(R.id.topicEditText)
        val questionsEditText = view.findViewById<EditText>(R.id.questionsEditText)
        val confidenceSeekBar = view.findViewById<SeekBar>(R.id.confidenceSeekBar)
        val submitButton = view.findViewById<Button>(R.id.submitButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.logProgressBar)
        val confidenceValueText = view.findViewById<TextView>(R.id.confidenceValueText)
        logHistoryRecyclerView = view.findViewById(R.id.logHistoryRecyclerView)
        logHistoryRecyclerView.layoutManager = LinearLayoutManager(context)

        // --- Setup SeekBar Listener ---
        confidenceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                confidenceValueText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // --- Setup Submit Button Listener ---
        submitButton.setOnClickListener {
            val topic = topicEditText.text.toString()
            val questions = questionsEditText.text.toString().toIntOrNull() ?: 0
            val confidence = confidenceSeekBar.progress

            if (topic.isBlank()) {
                Toast.makeText(context, "Please enter a topic", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val secretKey = "CATPREP123" // IMPORTANT: Replace with your actual key

            val requestBody = LogRequestBody(secret = secretKey, topic = topic, questions = questions, confidence = confidence)

            // Disable button and show progress bar
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
                            // Clear fields
                            topicEditText.text.clear()
                            questionsEditText.text.clear()
                            confidenceSeekBar.progress = 70
                            confidenceValueText.text = "70%"
                            // --- REFRESH THE LOG HISTORY ---
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
        
        // --- Initial fetch of log history ---
        fetchLogHistory()
    }

    private fun fetchLogHistory() {
        // You can add a progress bar for this part later if you wish
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getLogHistory()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val logItems = response.body()!!.history
                        logHistoryRecyclerView.adapter = LogHistoryAdapter(logItems)
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
}
