package com.hanto.aischeduler.data

import com.hanto.aischeduler.data.model.GroqRequest
import com.hanto.aischeduler.data.model.GroqResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GroqApiService {
    @POST("openai/v1/chat/completions")
    suspend fun generateSchedule(
        @Header("Authorization") authorization: String,
        @Body request: GroqRequest
    ): Response<GroqResponse>
}