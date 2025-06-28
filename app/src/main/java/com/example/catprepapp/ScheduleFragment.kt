package com.example.catprepapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.catprepapp.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScheduleFragment : Fragment() {

    private lateinit var scheduleTextView: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the views from the layout
        scheduleTextView = view.findViewById(R.id.scheduleTextView)
        progressBar = view.findViewById(R.id.progressBar)

        // Fetch the data from the API
        fetchSchedule()
    }

    private fun fetchSchedule() {
        // Show the progress bar and launch a coroutine for the network call
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getSchedule()

                // Switch to the Main thread to update the UI
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val scheduleItems = response.body()!!.schedule
                        if (scheduleItems.isNotEmpty()) {
                            // Format the data into a nice string
                            val scheduleText = StringBuilder()
                            for (item in scheduleItems) {
                                scheduleText.append("🕒 ${item.time}\n")
                                scheduleText.append("📚 ${item.topic}\n")
                                scheduleText.append("📝 ${item.notes}\n\n")
                            }
                            scheduleTextView.text = scheduleText.toString()
                        } else {
                            scheduleTextView.text = "No schedule found for today."
                        }
                    } else {
                        // Handle API error
                        scheduleTextView.text = "Failed to load schedule. Error: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                // Handle network or other exceptions
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    scheduleTextView.text = "An error occurred: ${e.message}"
                }
            }
        }
    }
}
