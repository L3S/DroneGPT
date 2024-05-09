package com.l3s.dronegpt.network

import com.google.gson.JsonObject
import com.l3s.dronegpt.model.GptResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
interface OpenaiApi {
    @Headers(
        "Content-Type:application/json",
        "Authorization:Bearer API")
    @POST("v1/chat/completions")
    suspend fun postRequest(
        @Body json : JsonObject
    ) : GptResponse
}