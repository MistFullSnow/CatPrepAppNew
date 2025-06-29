package com.example.catprepapp

import android.graphics.Color
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
import com.github.mikephil.charting.charts.HorizontalBarChart // NEW IMPORT
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
    
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
                        Toast.makeText(context, "Failed to load dashboard data.", Toast.LENGTH_SHORT).show()
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
        setupKeyStats(data)
        setupSectionalPerformance(data)
        setupHorizontalBarChart(data) // <-- CHANGED from setupRadarChart
        setupWeakestTopics(data)
    }
    
    private fun setupKeyStats(data: DashboardResponse) {
        view?.findViewById<TextView>(R.id.totalQuestionsText)?.text = data.totalQuestions.toString()
        view?.findViewById<TextView>(R.id.avgConfidenceText)?.text = "${data.avgConfidence}%"
        view?.findViewById<TextView>(R.id.studyDaysText)?.text = data.studyDays.toString()
    }

    private fun setupSectionalPerformance(data: DashboardResponse) {
        view?.findViewById<TextView>(R.id.qaScoreText)?.text = "${data.sectionalConfidence.qa}%"
        view?.findViewById<TextView>(R.id.dilrScoreText)?.text = "${data.sectionalConfidence.dilr}%"
        view?.findViewById<TextView>(R.id.varcScoreText)?.text = "${data.sectionalConfidence.varc}%"
    }
    
    // --- NEW AND IMPROVED CHART FUNCTION ---
    private fun setupHorizontalBarChart(data: DashboardResponse) {
        val barChart = view?.findViewById<HorizontalBarChart>(R.id.topicBarChart) ?: return
        if (data.topicPerformance.isEmpty()) {
            barChart.visibility = View.GONE
            return
        }
        barChart.visibility = View.VISIBLE

        // Sort topics by PPM score so the best are at the top
        val sortedTopics = data.topicPerformance.sortedByDescending { it.ppm }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        
        // The library plots from bottom to top, so we reverse the list for display
        sortedTopics.asReversed().forEachIndexed { index, topic ->
            entries.add(BarEntry(index.toFloat(), topic.ppm.toFloat()))
            // The library doesn't support multi-line labels well, so we abbreviate long ones
            labels.add(abbreviateTopic(topic.topic))
        }

        val dataSet = BarDataSet(entries, "Topic Score")
        dataSet.color = Color.WHITE
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        barChart.data = BarData(dataSet)
        
        // --- STYLING AND SCROLLING ---
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawValueAboveBar(true)
        barChart.setPinchZoom(false) // Disable pinch zoom

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.setDrawAxisLine(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.WHITE
        xAxis.textSize = 12f
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        
        val leftAxis = barChart.axisLeft
        leftAxis.textColor = Color.WHITE
        leftAxis.axisMinimum = 0f

        barChart.axisRight.isEnabled = false
        
        // This makes only a few bars visible at a time, enabling scrolling
        barChart.setVisibleXRangeMaximum(5f)
        barChart.moveViewToX(labels.size.toFloat()) // Start scrolled to the end (top of the list)
        
        barChart.invalidate() // Refresh chart
    }
    
    // Helper function to shorten long topic names
    private fun abbreviateTopic(topic: String): String {
        return if (topic.length > 20) {
            topic.substring(0, 18) + "..."
        } else {
            topic
        }
    }

    private fun setupWeakestTopics(data: DashboardResponse) {
        val weakestTopicsLayout = view?.findViewById<LinearLayout>(R.id.weakestTopicsLayout) ?: return
        weakestTopicsLayout.removeAllViews()
        if (data.weakestTopics.isNotEmpty()) {
            data.weakestTopics.forEach { topic ->
                val topicView = TextView(context).apply {
                    text = "• ${topic.topic} (Score: ${topic.ppm}, Conf: ${topic.avgConfidence}%)"
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    setPadding(0, 8, 0, 8)
                }
                weakestTopicsLayout.addView(topicView)
            }
        } else {
             val noTopicsView = TextView(context).apply {
                    text = "Not enough data for topic analysis yet."
                    setTextColor(Color.GRAY)
                    textSize = 16f
                }
            weakestTopicsLayout.addView(noTopicsView)
        }
    }
}
