package com.example.catprepapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.catprepapp.network.ApiClient
import com.example.catprepapp.network.ChatRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CatBotFragment : Fragment() {

    // ... view properties ...
    private lateinit var chatTextView: TextView
    private lateinit var chatEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatScrollView: ScrollView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_cat_bot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ... find views ...
        chatTextView = view.findViewById(R.id.chatTextView)
        chatEditText = view.findViewById(R.id.chatEditText)
        sendButton = view.findViewById(R.id.sendButton)
        chatScrollView = view.findViewById(R.id.chatScrollView)

        sendButton.setOnClickListener {
            val query = chatEditText.text.toString()
            if (query.isNotBlank()) {
                sendMessage(query)
            }
        }
        
        // --- NEW: Load history when the view is created ---
        loadChatHistory()
    }

    private fun loadChatHistory() {
        chatTextView.text = "Loading chat history..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.getChatHistory()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val history = response.body()!!.history
                        val chatText = StringBuilder("Welcome to CATBOT!\n")
                        for (message in history) {
                            if (message.user.isNotBlank()) chatText.append("\n\nYOU:\n${message.user}")
                            if (message.bot.isNotBlank()) chatText.append("\n\nCATBOT:\n${message.bot}")
                        }
                        chatTextView.text = chatText.toString()
                        scrollToBottom()
                    } else {
                        chatTextView.text = "Could not load chat history."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    chatTextView.text = "Error loading chat: ${e.message}"
                }
            }
        }
    }
    
    private fun sendMessage(query: String) {
        val userMessage = "\n\nYOU:\n$query"
        chatTextView.append(userMessage)
        chatEditText.text.clear()
        
        chatTextView.append("\n\nCATBOT:\nTyping...")
        scrollToBottom()

        val secretKey = "CATPREP123" // Replace
        val requestBody = ChatRequestBody(secret = secretKey, query = query)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // We only need to call askCatbot, it now handles history itself
                val response = ApiClient.apiService.askCatbot(requestBody) 
                withContext(Dispatchers.Main) {
                    val currentText = chatTextView.text.toString().removeSuffix("Typing...")
                    chatTextView.text = currentText

                    if (response.isSuccessful && response.body() != null) {
                        val reply = response.body()!!.reply
                        chatTextView.append(reply)
                    } else {
                        chatTextView.append("Sorry, I encountered an error.")
                    }
                    scrollToBottom()
                }
            } catch (e: Exception) {
                // ... error handling ...
            }
        }
    }

    private fun scrollToBottom() {
        chatScrollView.post { chatScrollView.fullScroll(View.FOCUS_DOWN) }
    }
}
