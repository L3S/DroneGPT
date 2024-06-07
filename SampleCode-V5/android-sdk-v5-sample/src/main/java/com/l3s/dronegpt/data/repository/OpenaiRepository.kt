package com.l3s.dronegpt.data.repository

import com.google.gson.JsonObject
import com.l3s.dronegpt.network.OpenaiApi
import com.l3s.dronegpt.network.RetrofitInstance

class OpenaiRepository {

    private val chatGPTClient = RetrofitInstance.getInstance().create(OpenaiApi::class.java)

    suspend fun postResponse(jsonData : JsonObject) = chatGPTClient.postRequest(jsonData)
}