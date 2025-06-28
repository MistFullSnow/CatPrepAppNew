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
    private lateinit var historyAdapter: LogHistoryAdapter
    private var logHistoryList = mutableListOf<LogHistoryItem>()
    // NEW: TextViews to act as our fake spinners
    private lateinit var subjectSelector: TextView
    private lateinit var topicSelector: TextView
    private var topicsResponse: TopicsResponse? = null
    private var selectedSubject: String? = null
    private var selectedTopic: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews(view)
        fetchInitialData()
    }

    private fun setupViews(view: View) {
        subjectSelector = view.findViewById(R.id.subjectSelector)
        topicSelector = view.findViewById(R.id.topicSelector)
        logHistoryRecyclerView = view.findViewById(R.id.logHistoryRecyclerView)
        logHistoryRecyclerView.layoutManager = LinearLayoutManager(context)
        historyAdapter = LogHistoryAdapter(logHistoryList) { logItem, position -> onDeleteClicked(logItem, position) }
        logHistoryRecyclerView.adapter = historyAdapter
        
        setupListeners(view)
    }
    
    private fun setupListeners(view: View) {
        val questionsEditText = view.findViewById<EditText>(R.id.questionsEditText)
        val confidenceSeekBar = view.findViewById<SeekBar>(R.id.confidenceSeekBar)
        val submitButton = view.findViewById<Button>(R.id.submitButton)
        val confidenceValueText = view.findViewById<TextView>(R.id.confidenceValueText)

        confidenceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                confidenceValueText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // --- NEW: Click listener for our fake subject spinner ---
        subjectSelector.setOnClickListener {
            showSubjectSelectorDialog()
        }

        // --- NEW: Click listener for our fake topic spinner ---
        topicSelector.setOnClickListener {
            showTopicSelectorDialog()
        }

        submitButton.setOnClickListener {
            val topic = selectedTopic ?: ""
            val questions = questionsEditText.text.toString().toIntOrNull() ?: 0
            val confidence = confidenceSeekBar.progress

            if (topic.isBlank()) {
                Toast.makeText(context, "Please select a valid topic", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // ... (The rest of the submit logic remains exactly the same) ...
            val secretKey = "CATPREP123" // IMPORTANT: Replace
            val requestBody = LogRequestBody(secret = secretKey, topic = topic, questions = questions, confidence = confidence)

            submitButton.isEnabled = false
            view.findViewById<ProgressBar>(R.id.logProgressBar)?.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = ApiClient.apiService.submitLog(requestBody)
                    withContext(Dispatchers.Main) {
                        view.findViewById<ProgressBar>(R.id.logProgressBar)?.visibility = View.GONE
                        submitButton.isEnabled = true
                        if (response.isSuccessful) {
                            Toast.makeText(context, "Log submitted successfully!", Toast.LENGTH_LONG).show()
                            // Reset fields
                            questionsEditText.text.clear()
                            confidenceSeekBar.progress = 70
                            confidenceValueText.text = "70%"
                            subjectSelector.text = "Select Subject..."
                            topicSelector.text = "Select Topic..."
                            selectedSubject = null
                            selectedTopic = null
                            fetchLogHistory()
                        } else {
                            Toast.makeText(context, "Submission failed", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                       view.findViewById<ProgressBar>(R.id.logProgressBar)?.visibility = View.GONE
                       submitButton.isEnabled = true
                       Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    // --- NEW: Functions to show the selection dialogs ---
    private fun showSubjectSelectorDialog() {
        val subjects = arrayOf("QA", "DILR", "VARC")
        AlertDialog.Builder(requireContext(), R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle("Select Subject")
            .setItems(subjects) { dialog, which ->
                selectedSubject = subjects[which]
                subjectSelector.text = selectedSubject
                
                // Reset topic when subject changes
                selectedTopic = null
                topicSelector.text = "Select Topic..."
                dialog.dismiss()
            }
            .show()
    }

    private fun showTopicSelectorDialog() {
        if (selectedSubject == null) {
            Toast.makeText(context, "Please select a subject first", Toast.LENGTH_SHORT).show()
            return
        }

        val topicsForSubject = when (selectedSubject) {
            "QA" -> topicsResponse?.qa?.toTypedArray() ?: arrayOf()
            "DILR" -> topicsResponse?.dilr?.toTypedArray() ?: arrayOf()
            "VARC" -> topicsResponse?.varc?.toTypedArray() ?: arrayOf()
            else -> arrayOf()
        }

        if (topicsForSubject.isEmpty()) {
            Toast.makeText(context, "No topics found for this subject", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext(), R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle("Select Topic for $selectedSubject")
            .setItems(topicsForSubject) { dialog, which ->
                selectedTopic = topicsForSubject[which]
                topicSelector.text = selectedTopic
                dialog.dismiss()
            }
            .show()
    }
    
    // --- The rest of the functions (fetchInitialData, fetchTopics, etc.) remain the same ---
    private fun fetchInitialData() {
        view?.findViewById<ProgressBar>(R.id.logProgressBar)?.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val fetchedTopics = fetchTopics()
            val fetchedHistory = fetchLogHistoryData()
            
            withContext(Dispatchers.Main) {
                view?.findViewById<ProgressBar>(R.id.logProgressBar)?.visibility = View.GONE
                topicsResponse = fetchedTopics
                if (fetchedHistory != null) {
                    logHistoryList.clear()
                    logHistoryList.addAll(fetchedHistory)
                    historyAdapter.notifyDataSetChanged()
                }
            }
        }
    }
    private suspend fun fetchTopics(): TopicsResponse? {
        return try {
            val response = ApiClient.apiService.getTopics()
            if(response.isSuccessful) response.body() else null
        } catch (e: Exception) { null }
    }
    private suspend fun fetchLogHistoryData(): List<LogHistoryItem>? {
         return try {
            val response = ApiClient.apiService.getLogHistory()
            if (response.isSuccessful) response.body()?.history else null
        } catch (e: Exception) { null }
    }
    private fun fetchLogHistory() {
        CoroutineScope(Dispatchers.IO).launch {
            val newHistory = fetchLogHistoryData()
            withContext(Dispatchers.Main) {
                if (newHistory != null) {
                    logHistoryList.clear()
                    logHistoryList.addAll(newHistory)
                    historyAdapter.notifyDataSetChanged()
                }
            }
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
                        historyAdapter.removeItem(position)
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
