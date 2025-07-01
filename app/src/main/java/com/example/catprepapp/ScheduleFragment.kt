package com.example.catprepapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.catprepapp.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent
import android.widget.Button


class ScheduleFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.scheduleRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyView = view.findViewById(R.id.emptyView)

        recyclerView.layoutManager = LinearLayoutManager(context)
        
        fetchSchedule()

        val essayButton = view.findViewById<Button>(R.id.essayButton)
        essayButton.setOnClickListener {
            val intent = Intent(context, EssayActivity::class.java)
            startActivity(intent)
        }
    }

    private fun fetchSchedule() {
        progressBar.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        recyclerView.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getSchedule()
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val scheduleItems = response.body()!!.schedule
                        if (scheduleItems.isNotEmpty()) {
                            recyclerView.adapter = ScheduleAdapter(scheduleItems)
                            recyclerView.visibility = View.VISIBLE
                        } else {
                            emptyView.visibility = View.VISIBLE
                        }
                    } else {
                        emptyView.text = "Failed to load schedule. Error: ${response.code()}"
                        emptyView.visibility = View.VISIBLE 
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    emptyView.text = "An error occurred: ${e.message}"
                    emptyView.visibility = View.VISIBLE
                }
            }
        }
    }
}
