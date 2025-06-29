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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragment : Fragment() {
    // ... (onCreateView is the same) ...
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchDashboardData()
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
        setupBarChart(data)
        setupWeakestTopics(data)
    }
    
    private fun setupBarChart(data: DashboardResponse) {
        val barChart = view?.findViewById<BarChart>(R.id.performanceBarChart) ?: return
        
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, data.totalQuestions.toFloat()))
        entries.add(BarEntry(1f, data.avgConfidence.toFloat()))
        entries.add(BarEntry(2f, data.streakDays.toFloat()))

        val dataSet = BarDataSet(entries, "Performance Metrics")
        dataSet.colors = listOf(Color.WHITE, Color.LTGRAY, Color.GRAY)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barChart.data = barData

        // Chart styling
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawValueAboveBar(true)
        barChart.setFitBars(true)
        
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = Color.WHITE
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(listOf("Questions", "Confidence", "Days"))
        
        barChart.axisLeft.textColor = Color.WHITE
        barChart.axisRight.isEnabled = false
        
        barChart.invalidate() // Refresh chart
    }

    private fun setupWeakestTopics(data: DashboardResponse) {
        val weakestTopicsLayout = view?.findViewById<LinearLayout>(R.id.weakestTopicsLayout) ?: return
        
        weakestTopicsLayout.removeAllViews()
        if (data.weakestTopics.isNotEmpty()) {
            data.weakestTopics.forEach { topic ->
                val topicView = TextView(context).apply {
                    text = "• ${topic.topic} (${topic.avgConfidence}% confidence)"
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
