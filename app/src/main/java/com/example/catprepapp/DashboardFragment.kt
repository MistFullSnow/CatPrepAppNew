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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.catprepapp.network.ApiClient
import com.example.catprepapp.network.DashboardResponse
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
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
        // Re-populate the simple stats if you want them
        // setupKeyStats(data) 
        setupPieChart(data)
        setupRadarChart(data)
        setupWeakestTopics(data)
    }

    private fun setupPieChart(data: DashboardResponse) {
        val pieChart = view?.findViewById<PieChart>(R.id.sectionalPieChart) ?: return
        
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(data.sectionalConfidence.qa.toFloat(), "QA"))
        entries.add(PieEntry(data.sectionalConfidence.dilr.toFloat(), "DILR"))
        entries.add(PieEntry(data.sectionalConfidence.varc.toFloat(), "VARC"))

        val dataSet = PieDataSet(entries, "Sectional Confidence")
        dataSet.colors = listOf(
            ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light),
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_light),
            ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
        )
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 14f
        
        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.legend.textColor = Color.WHITE
        pieChart.invalidate()
    }
    
    private fun setupRadarChart(data: DashboardResponse) {
        val radarChart = view?.findViewById<RadarChart>(R.id.topicRadarChart) ?: return
        if (data.topicPerformance.isEmpty()) return

        val entries = ArrayList<RadarEntry>()
        val labels = ArrayList<String>()
        
        data.topicPerformance.forEach {
            entries.add(RadarEntry(it.ppm.toFloat()))
            labels.add(it.topic)
        }

        val dataSet = RadarDataSet(entries, "Topic PPM Score")
        dataSet.color = Color.CYAN
        dataSet.fillColor = Color.CYAN
        dataSet.setDrawFilled(true)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 10f
        
        radarChart.data = RadarData(dataSet)
        radarChart.description.isEnabled = false
        
        val xAxis = radarChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.textColor = Color.WHITE
        
        val yAxis = radarChart.yAxis
        yAxis.textColor = Color.WHITE
        yAxis.axisMinimum = 0f
        
        radarChart.legend.isEnabled = false
        radarChart.webColor = Color.GRAY
        radarChart.webColorInner = Color.DKGRAY
        radarChart.invalidate()
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
        }
    }
}
