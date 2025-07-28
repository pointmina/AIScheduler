package com.hanto.aischeduler.data.model

data class GroqResponse(
    val choices: List<GroqChoice>,
    val usage: GroqUsage
)

data class GroqChoice(
    val message: GroqMessage,
    val index: Int,
    val finish_reason: String
)

data class GroqUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)