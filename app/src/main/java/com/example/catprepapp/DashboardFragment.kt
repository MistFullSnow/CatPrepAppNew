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
    // --- THE FINAL, CORRECTED BAR CHART FUNCTION ---
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
            labels.add(topic.topic)
        }
    
        val dataSet = BarDataSet(entries, "Topic Score")
        dataSet.color = Color.WHITE
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 10f
    
        val barData = BarData(dataSet)
        barData.barWidth = 0.6f // A good width for the bars
        barChart.data = barData
        
        // --- SIMPLE, CLEAN, AND READABLE STYLING ---
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setPinchZoom(false)
        barChart.setDrawGridBackground(false)
        barChart.setDrawValueAboveBar(true)
        barChart.isDragEnabled = true // Make sure scrolling is enabled
        
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.textColor = Color.WHITE
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        
        // --- THIS IS THE FIX FOR WRAPPING LABELS ---
        xAxis.setLabelCount(labels.size, false) // Ensure all labels are considered
        xAxis.labelRotationAngle = 0f          // Keep labels horizontal
        barChart.extraBottomOffset = 16f         // Add extra space at the bottom for wrapped labels
    
        val yAxisLeft = barChart.axisLeft
        yAxisLeft.textColor = Color.WHITE
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.gridColor = Color.DKGRAY
    
        barChart.axisRight.isEnabled = false
        
        // Set a fixed number of bars visible at once to enable scrolling
        barChart.setVisibleXRangeMaximum(4f)
        barChart.moveViewTo(0f, 0f, YAxis.AxisDependency.LEFT) // Start at the beginning
        
        barChart.invalidate()
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
