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

    // --- Class properties ---
    private lateinit var logHistoryRecyclerView: RecyclerView
    private lateinit var adapter: LogHistoryAdapter
    private var logHistoryList = mutableListOf<LogHistoryItem>()
    private lateinit var subjectSpinner: Spinner
    private lateinit var topicSpinner: Spinner
    private var topicsResponse: TopicsResponse? = null // Store the fetched topics

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Find all UI components ---
        subjectSpinner = view.findViewById(R.id.subjectSpinner)
        topicSpinner = view.findViewById(R.id.topicSpinner)
        // ... (find other views)
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
        
        // --- Setup Listeners ---
        setupListeners(view)

        // --- Fetch initial data ---
        fetchInitialData()
    }

    private fun setupListeners(view: View) {
        val questionsEditText = view.findViewById<EditText>(R.id.questionsEditText)
        val confidenceSeekBar = view.findViewById<SeekBar>(R.id.confidenceSeekBar)
        val submitButton = view.findViewById<Button>(R.id.submitButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.logProgressBar)
        val confidenceValueText = view.findViewById<TextView>(R.id.confidenceValueText)

        confidenceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                confidenceValueText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // --- NEW: Listener for the Subject Spinner ---
        subjectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // When a subject is selected, update the topic spinner
                updateTopicSpinner(parent?.getItemAtPosition(position).toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        submitButton.setOnClickListener {
            val topic = if (topicSpinner.selectedItemPosition > 0) topicSpinner.selectedItem.toString() else ""
            val questions = questionsEditText.text.toString().toIntOrNull() ?: 0
            val confidence = confidenceSeekBar.progress

            if (topic.isBlank()) {
                Toast.makeText(context, "Please select a valid topic", Toast.LENGTH_SHORT).show()
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
                            // Reset fields
                            questionsEditText.text.clear()
                            confidenceSeekBar.progress = 70
                            confidenceValueText.text = "70%"
                            subjectSpinner.setSelection(0)
                            fetchLogHistory()
                        } else {
                            Toast.makeText(context, "Submission failed", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                       // ... error handling ...
                    }
                }
            }
        }
    }
    
    private fun fetchInitialData() {
        CoroutineScope(Dispatchers.IO).launch {
            topicsResponse = fetchTopics() // Fetch and store topics
            withContext(Dispatchers.Main) {
                if (topicsResponse != null) {
                    setupSubjectSpinner()
                } else {
                    Toast.makeText(context, "Could not load topics.", Toast.LENGTH_SHORT).show()
                }
            }
            fetchLogHistory()
        }
    }
    
    private fun setupSubjectSpinner() {
        val subjects = listOf("Select Subject...", "QA", "DILR", "VARC")
        // Use our new custom dark layout
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_dark, subjects)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        subjectSpinner.adapter = adapter
    }

    private fun updateTopicSpinner(subject: String) {
        val topicsForSubject = when (subject) {
            "QA" -> topicsResponse?.qa ?: listOf()
            "DILR" -> topicsResponse?.dilr ?: listOf()
            "VARC" -> topicsResponse?.varc ?: listOf()
            else -> listOf()
        }
        val topicsWithPrompt = mutableListOf("Select Topic...").apply { addAll(topicsForSubject) }
        // Use our new custom dark layout
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_dark, topicsWithPrompt)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_dark)
        topicSpinner.adapter = adapter
    }

    // --- The rest of the functions (fetchTopics, fetchLogHistory, onDeleteClicked, performDelete) remain the same ---
    private suspend fun fetchTopics(): TopicsResponse? {
        return try {
            val response = ApiClient.apiService.getTopics()
            if(response.isSuccessful) response.body() else null
        } catch (e: Exception) {
            null
        }
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
                    }
                }
            } catch (e: Exception) {}
        }
    }
    
    private fun onDeleteClicked(logItem: LogHistoryItem, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Log")
            .setMessage("Are you sure you want to delete this log entry?")
            .setPositiveButton("Delete") { _, _ -> performDelete(logItem, position) }
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
