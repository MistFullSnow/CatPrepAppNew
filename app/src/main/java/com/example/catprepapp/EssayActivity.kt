package com.example.catprepapp

import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.catprepapp.network.ApiClient
import com.example.catprepapp.network.VocabRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EssayActivity : AppCompatActivity() {

    private lateinit var contentTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_essay)
        
        val titleTextView = findViewById<TextView>(R.id.essayTitleText)
        contentTextView = findViewById(R.id.essayContentText)

        fetchEssay(titleTextView, contentTextView)
        setupTextSelectionCallback()
    }

    private fun fetchEssay(title: TextView, content: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getDailyEssay()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        title.text = response.body()!!.title
                        content.text = response.body()!!.content
                    } else {
                        title.text = "Error"
                        content.text = "Could not load essay."
                    }
                }
            } catch (e: Exception) { /* ... */ }
        }
    }

    private fun setupTextSelectionCallback() {
        contentTextView.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                // Inflate a menu for the CAB
                mode?.menuInflater?.inflate(R.menu.text_selection_menu, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                if (item?.itemId == R.id.action_save_word) {
                    val start = contentTextView.selectionStart
                    val end = contentTextView.selectionEnd
                    val selectedWord = contentTextView.text.substring(start, end)
                    saveWord(selectedWord)
                    mode?.finish() // Close the context menu
                    return true
                }
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {}
        }
    }

    private fun saveWord(word: String) {
        val secretKey = "YOUR_SECRET_KEY" // IMPORTANT: Replace
        val requestBody = VocabRequestBody(secret = secretKey, word = word)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.apiService.saveVocabWord(requestBody)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EssayActivity, "'$word' saved!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { /* ... */ }
        }
    }
}
