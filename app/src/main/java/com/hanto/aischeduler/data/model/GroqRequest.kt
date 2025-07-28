package com.hanto.aischeduler.data.model

data class GroqRequest(
    val messages: List<GroqMessage>,
    val model: String = "llama3-8b-8192",
    val temperature: Double = 0.1,
    val max_tokens: Int = 1024
)

data class GroqMessage(
    val role: String, // "system", "user"
    val content: String
)