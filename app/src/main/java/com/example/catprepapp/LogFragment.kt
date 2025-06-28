package com.example.catprepapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.catprepapp.network.ApiClient
import com.example.catprepapp.network.LogRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val topicEditText = view.findViewById<EditText>(R.id.topicEditText)
        val questionsEditText = view.findViewById<EditText>(R.id.questionsEditText)
        val confidenceSeekBar = view.findViewById<SeekBar>(R.id.confidenceSeekBar)
        val submitButton = view.findViewById<Button>(R.id.submitButton)
        val progressBar = view.findViewById<ProgressBar>(R.id.logProgressBar)

        submitButton.setOnClickListener {
            val topic = topicEditText.text.toString()
            val questions = questionsEditText.text.toString().toIntOrNull() ?: 0
            val confidence = confidenceSeekBar.progress

            // Simple validation
            if (topic.isBlank()) {
                Toast.makeText(context, "Please enter a topic", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Get the secret key from ApiService.kt
            // NOTE: In a real app, this would be stored more securely.
            val secretKey = "CATPREP123" // IMPORTANT: Replace with your actual key

            val requestBody = LogRequestBody(
                secret = secretKey,
                topic = topic,
                questions = questions,
                confidence = confidence
            )

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
    }
}
