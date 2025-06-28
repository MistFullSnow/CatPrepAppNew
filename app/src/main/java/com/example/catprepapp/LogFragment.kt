override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val topicEditText = view.findViewById<EditText>(R.id.topicEditText)
    val questionsEditText = view.findViewById<EditText>(R.id.questionsEditText)
    val confidenceSeekBar = view.findViewById<SeekBar>(R.id.confidenceSeekBar)
    val submitButton = view.findViewById<Button>(R.id.submitButton)
    val progressBar = view.findViewById<ProgressBar>(R.id.logProgressBar)
    val confidenceValueText = view.findViewById<TextView>(R.id.confidenceValueText) // Find the new TextView

    // --- NEW CODE: SeekBar Listener ---
    confidenceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            // Update the TextView as the slider moves
            confidenceValueText.text = "$progress%"
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
    })
    // --- END OF NEW CODE ---

    submitButton.setOnClickListener {
        val topic = topicEditText.text.toString()
        val questions = questionsEditText.text.toString().toIntOrNull() ?: 0
        val confidence = confidenceSeekBar.progress

        if (topic.isBlank()) {
            Toast.makeText(context, "Please enter a topic", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }
        
        val secretKey = "CATPREP123" // IMPORTANT: Replace with your actual key

        val requestBody = LogRequestBody(
            secret = secretKey,
            topic = topic,
            questions = questions,
            confidence = confidence
        )

        submitButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.submitLog(requestBody)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    submitButton.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Log submitted successfully!", Toast.LENGTH_LONG).show()
                        topicEditText.text.clear()
                        questionsEditText.text.clear()
                        confidenceSeekBar.progress = 70
                        confidenceValueText.text = "70%" // Reset the text view
                    } else {
                        Toast.makeText(context, "Submission failed: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    submitButton.isEnabled = true
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
