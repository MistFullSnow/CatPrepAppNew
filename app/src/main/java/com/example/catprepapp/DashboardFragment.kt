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
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.mikephil.charting.components.YAxis


class DashboardFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }
    
    override fun onResume() {
        super.onResume()
        fetchDashboardData()
    }

    private fun fetchDashboardData() {
        // This function remains the same
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
                    } else { Toast.makeText(context, "Failed to load dashboard data.", Toast.LENGTH_SHORT).show() }
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
        setupVerticalBarChart(data)
        setupWeakestTopics(data)
    }
    
    private fun setupKeyStats(data: DashboardResponse) {
        // This function remains the same
        view?.findViewById<TextView>(R.id.totalQuestionsText)?.text = data.totalQuestions.toString()
        view?.findViewById<TextView>(R.id.avgConfidenceText)?.text = "${data.avgConfidence}%"
        view?.findViewById<TextView>(R.id.studyDaysText)?.text = data.studyDays.toString()
    }

    private fun setupSectionalPerformance(data: DashboardResponse) {
        // This function remains the same
        view?.findViewById<TextView>(R.id.qaScoreText)?.text = "${data.sectionalConfidence.qa}%"
        view?.findViewById<TextView>(R.id.dilrScoreText)?.text = "${data.sectionalConfidence.dilr}%"
        view?.findViewById<TextView>(R.id.varcScoreText)?.text = "${data.sectionalConfidence.varc}%"
    }
    
    // --- THE NEW, SIMPLIFIED, AND CORRECT BAR CHART FUNCTION ---
    private fun setupVerticalBarChart(data: DashboardResponse) {
        val barChart = view?.findViewById<BarChart>(R.id.topicBarChart) ?: return
        if (data.topicPerformance.isEmpty()) {
            barChart.visibility = View.GONE
            return
        }
        barChart.visibility = View.VISIBLE
    
        val sortedTopics = data.topicPerformance.sortedByDescending { it.ppm }
    
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        
        sortedTopics.forEachIndexed { index, topic ->
            entries.add(BarEntry(index.toFloat(), topic.ppm.toFloat()))
            // Debug: Log the original topic name to see what we're getting
            println("Original topic: '${topic.topic}' (length: ${topic.topic.length})")
            
            // Try smart wrapping first, then force wrapping if needed
            val wrappedLabel = smartWrapText(topic.topic, 10) // Try 10 chars first
            println("Wrapped topic: '$wrappedLabel'")
            labels.add(wrappedLabel)
        }
    
        val dataSet = BarDataSet(entries, "Topic Score")
        dataSet.color = Color.WHITE
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 10f
    
        val barData = BarData(dataSet)
        // Make bars wider - this is key for proper spacing
        barData.barWidth = 0.8f
        barChart.data = barData
        
        // Basic chart styling
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setPinchZoom(false)
        barChart.setDrawGridBackground(false)
        barChart.setDrawValueAboveBar(true)
        barChart.isDragEnabled = true
        barChart.setScaleEnabled(false) // Disable scaling to prevent squishing
        
        // X-Axis configuration
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.WHITE
        xAxis.textSize = 10f
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.labelRotationAngle = 0f // Keep labels horizontal
        xAxis.isGranularityEnabled = true
        xAxis.setLabelCount(labels.size, false)
        xAxis.setAvoidFirstLastClipping(true)
        
        // Y-Axis configuration
        val yAxisLeft = barChart.axisLeft
        yAxisLeft.textColor = Color.WHITE
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.gridColor = Color.DKGRAY
        yAxisLeft.textSize = 10f
    
        barChart.axisRight.isEnabled = false
        
        // Calculate proper width for the chart
        val barCount = labels.size
        val minBarWidth = 120 // Minimum width per bar in dp
        val chartWidth = barCount * minBarWidth
        
        // Set the chart width programmatically
        val layoutParams = barChart.layoutParams
        layoutParams.width = (chartWidth * resources.displayMetrics.density).toInt()
        barChart.layoutParams = layoutParams
        
        // Set extra offsets to prevent label clipping - increased bottom offset for multi-line labels
        barChart.setExtraOffsets(10f, 10f, 10f, 60f) // left, top, right, bottom
        
        barChart.invalidate()
    }
    
    // Helper function to force wrap text - breaks long text even without spaces
    private fun forceWrapText(text: String, maxLength: Int): String {
        if (text.length <= maxLength) return text
        
        val result = StringBuilder()
        var currentIndex = 0
        
        while (currentIndex < text.length) {
            val endIndex = minOf(currentIndex + maxLength, text.length)
            val chunk = text.substring(currentIndex, endIndex)
            
            if (result.isNotEmpty()) {
                result.append("\n")
            }
            result.append(chunk)
            currentIndex = endIndex
        }
        
        return result.toString()
    }
    
    // Alternative helper function that tries to break at natural points
    private fun smartWrapText(text: String, maxLength: Int): String {
        if (text.length <= maxLength) return text
        
        // First try to split on common delimiters
        val delimiters = listOf(" ", ",", "and", "on", "of", "in", "at", "to", "for")
        
        for (delimiter in delimiters) {
            if (text.contains(delimiter, ignoreCase = true)) {
                val parts = text.split(delimiter, ignoreCase = true)
                if (parts.size > 1) {
                    val result = StringBuilder()
                    var currentLine = StringBuilder()
                    
                    for (i in parts.indices) {
                        val part = parts[i]
                        val connector = if (i < parts.size - 1) delimiter else ""
                        
                        if (currentLine.length + part.length + connector.length <= maxLength) {
                            currentLine.append(part)
                            if (connector.isNotEmpty()) currentLine.append(connector)
                        } else {
                            if (result.isNotEmpty()) result.append("\n")
                            if (currentLine.isNotEmpty()) {
                                result.append(currentLine.toString())
                                currentLine = StringBuilder()
                            }
                            currentLine.append(part)
                            if (connector.isNotEmpty()) currentLine.append(connector)
                        }
                    }
                    
                    if (currentLine.isNotEmpty()) {
                        if (result.isNotEmpty()) result.append("\n")
                        result.append(currentLine.toString())
                    }
                    
                    return result.toString()
                }
            }
        }
        
        // If no delimiters found, force break
        return forceWrapText(text, maxLength)
    }
    private fun setupWeakestTopics(data: DashboardResponse) {
        // This function remains the same
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
