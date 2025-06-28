package com.example.catprepapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.catprepapp.network.ApiClient
import com.example.catprepapp.network.DashboardResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchDashboardData()
    }
    
    // This function will be called every time the tab becomes visible
    override fun onResume() {
        super.onResume()
        fetchDashboardData()
    }

    private fun fetchDashboardData() {
        val progressBar = view?.findViewById<ProgressBar>(R.id.dashboardProgressBar)
        val contentView = view?.findViewById<LinearLayout>(R.id.dashboardContent)

        progressBar?.visibility = View.VISIBLE
        contentView?.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getDashboard()
                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        populateDashboard(response.body()!!)
                        contentView?.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(context, "Failed to load dashboard", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun populateDashboard(data: DashboardResponse) {
        val totalQuestionsText = view?.findViewById<TextView>(R.id.totalQuestionsText)
        val avgConfidenceText = view?.findViewById<TextView>(R.id.avgConfidenceText)
        val streakDaysText = view?.findViewById<TextView>(R.id.streakDaysText)
        val weakestTopicsLayout = view?.findViewById<LinearLayout>(R.id.weakestTopicsLayout)

        totalQuestionsText?.text = data.totalQuestions.toString()
        avgConfidenceText?.text = "${data.avgConfidence}%"
        streakDaysText?.text = data.streakDays.toString()

        // Clear previous weakest topics and add the new ones
        weakestTopicsLayout?.removeAllViews()
        if (data.weakestTopics.isNotEmpty()) {
            for (topic in data.weakestTopics) {
                val topicView = TextView(context).apply {
                    text = "• ${topic.topic} (${topic.avgConfidence}% confidence)"
                    setTextColor(resources.getColor(android.R.color.white, null))
                    textSize = 16f
                    setPadding(0, 8, 0, 8)
                }
                weakestTopicsLayout?.addView(topicView)
            }
        } else {
             val noTopicsView = TextView(context).apply {
                    text = "Not enough data for topic analysis yet."
                    setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    textSize = 16f
                }
            weakestTopicsLayout?.addView(noTopicsView)
        }
    }
}
