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

// We do NOT need "import android.R"

class LogFragment : Fragment() {

    // --- Class properties ---
    private lateinit var historyAdapter: LogHistoryAdapter
    private var logHistoryList = mutableListOf<LogHistoryItem>()
    private var topicsResponse: TopicsResponse? = null
    private var selectedSubject: String? = null
    private var selectedTopic: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // The R here refers to our app's own generated R file.
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- Find Views ---
        val subjectSelector = view.findViewById<TextView>(R.id.subjectSelector)
        val topicSelector = view.findViewById<TextView>(R.id.topicSelector)
        val logHistoryRecyclerView = view.findViewById<RecyclerView>(R.id.logHistoryRecyclerView)
        val questionsEditText = view.findViewById<EditText>(R.id.questionsEditText)
        val confidenceSeekBar = view.findViewById<SeekBar>(R.id.confidenceSeekBar)
        val submitButton = view.findViewById<Button>(R.id.submitButton)
        val confidenceValueText = view.findViewById<TextView>(R.id.confidenceValueText)
        
        // --- Setup RecyclerView ---
        logHistoryRecyclerView.layoutManager = LinearLayoutManager(context)
        historyAdapter = LogHistoryAdapter(logHistoryList) { logItem, position ->
            showDeleteConfirmationDialog(logItem, position)
        }
        logHistoryRecyclerView.adapter = historyAdapter
        
        // --- Setup Listeners ---
        confidenceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                confidenceValueText.text = "$progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        subjectSelector.setOnClickListener {
            showSubjectSelectorDialog(subjectSelector, topicSelector)
        }

        topicSelector.setOnClickListener {
            showTopicSelectorDialog(topicSelector)
        }

        submitButton.setOnClickListener {
            val topic = selectedTopic ?: ""
            val questions = questionsEditText.text.toString().toIntOrNull() ?: 0
            val confidence = confidenceSeekBar.progress

            if (topic.isBlank()) {
                Toast.makeText(context, "Please select a valid topic", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val secretKey = "CATPREP123" // IMPORTANT: Replace
            val requestBody = LogRequestBody(secret = secretKey, topic = topic, questions = questions, confidence = confidence)

            submitForm(requestBody, view, questionsEditText, confidenceSeekBar, confidenceValueText, subjectSelector)
        }

        // --- Fetch initial data ---
        fetchInitialData()
    }
    
    // --- Dialogs and Data Logic ---
    private fun showSubjectSelectorDialog(subjectTextView: TextView, topicTextView: TextView) {
        val subjects = arrayOf("QA", "DILR", "VARC")
        // Use the app's default dialog theme
        AlertDialog.Builder(requireContext())
            .setTitle("Select Subject")
            .setItems(subjects) { dialog, which ->
                selectedSubject = subjects[which]
                subjectTextView.text = selectedSubject
                selectedTopic = null
                topicTextView.text = "Select Topic..."
                dialog.dismiss()
            }
            .show()
    }

    private fun showTopicSelectorDialog(topicTextView: TextView) {
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
        if (topicsForSubject.isEmpty()) return

        AlertDialog.Builder(requireContext())
            .setTitle("Select Topic for $selectedSubject")
            .setItems(topicsForSubject) { dialog, which ->
                selectedTopic = topicsForSubject[which]
                topicTextView.text = selectedTopic
                dialog.dismiss()
            }
            .show()
    }

    private fun submitForm(requestBody: LogRequestBody, view: View, qET: EditText, cSB: SeekBar, cVT: TextView, sS: TextView) {
        val progressBar = view.findViewById<ProgressBar>(R.id.logProgressBar)
        val submitButton = view.findViewById<Button>(R.id.submitButton)
        
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
                        qET.text.clear()
                        cSB.progress = 70
                        cVT.text = "70%"
                        sS.text = "Select Subject..."
                        view.findViewById<TextView>(R.id.topicSelector).text = "Select Topic..."
                        selectedSubject = null
                        selectedTopic = null
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

    private fun fetchInitialData() {
        view?.findViewById<ProgressBar>(R.id.logProgressBar)?.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            topicsResponse = fetchTopics()
            val fetchedHistory = fetchLogHistoryData()
            
            withContext(Dispatchers.Main) {
                view?.findViewById<ProgressBar>(R.id.logProgressBar)?.visibility = View.GONE
                if(fetchedHistory != null) {
                    logHistoryList.clear()
                    logHistoryList.addAll(fetchedHistory)
                    historyAdapter.notifyDataSetChanged()
                }
            }
        }
    }
    
    private suspend fun fetchTopics(): TopicsResponse? {
        return try {
            ApiClient.apiService.getTopics().body()
        } catch (e: Exception) { null }
    }

    private suspend fun fetchLogHistoryData(): List<LogHistoryItem>? {
         return try {
            ApiClient.apiService.getLogHistory().body()?.history
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
    
    private fun showDeleteConfirmationDialog(logItem: LogHistoryItem, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Log")
            .setMessage("Are you sure?")
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
