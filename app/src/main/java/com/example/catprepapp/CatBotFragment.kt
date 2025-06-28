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

    private lateinit var chatTextView: TextView
    private lateinit var chatEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatScrollView: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cat_bot, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
    }

    private fun sendMessage(query: String) {
        // Append user's message to the chat
        val userMessage = "\n\nYOU:\n$query"
        chatTextView.append(userMessage)
        chatEditText.text.clear()
        
        // Show a "typing" indicator
        chatTextView.append("\n\nCATBOT:\nTyping...")
        scrollToBottom()

        val secretKey = "CATPREP123" // IMPORTANT: Replace
        val requestBody = ChatRequestBody(secret = secretKey, query = query)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.askCatbot(requestBody)
                withContext(Dispatchers.Main) {
                    // Remove "Typing..."
                    val currentText = chatTextView.text.toString()
                    chatTextView.text = currentText.removeSuffix("Typing...")

                    if (response.isSuccessful && response.body() != null) {
                        val reply = response.body()!!.reply
                        chatTextView.append(reply)
                    } else {
                        chatTextView.append("Sorry, I encountered an error.")
                    }
                    scrollToBottom()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val currentText = chatTextView.text.toString()
                    chatTextView.text = currentText.removeSuffix("Typing...")
                    chatTextView.append("Sorry, can't connect right now.")
                    scrollToBottom()
                }
            }
        }
    }

    private fun scrollToBottom() {
        chatScrollView.post {
            chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
}
