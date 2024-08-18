package com.l3s.dronegpt.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.l3s.dronegpt.data.database.ChatContent
import com.l3s.dronegpt.data.database.Experiment
import com.l3s.dronegpt.data.repository.ChatContentRepository
import com.l3s.dronegpt.data.repository.OpenaiRepository
import com.l3s.dronegpt.model.GptText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val databaseRepository = ChatContentRepository(application)
    private val openaiRepository = OpenaiRepository()

    private var _contentList = MutableLiveData<List<ChatContent>>()
    val contentList : LiveData<List<ChatContent>>
        get() = _contentList

    private var _deleteCheck = MutableLiveData<Boolean>(false)
    val deleteCheck : LiveData<Boolean>
        get() = _deleteCheck

    private var _gptInsertCheck = MutableLiveData<Boolean>(false)
    val gptInsertCheck : LiveData<Boolean>
        get() = _gptInsertCheck

    fun postResponse(experiment: Experiment, query : String) = viewModelScope.launch {
        //retrieve previous chat messages and add them to the request as context
        val chatContentByExperiment = withContext(Dispatchers.IO) {
            databaseRepository.getChatContentByExperimentId(experiment.id)
        }

        val jsonObject: JsonObject = JsonObject().apply{
            // params
//            addProperty("model", "gpt-3.5-turbo")
            addProperty("model", experiment.openaiModel)
            add("messages", JsonArray().apply {
                // add chat history
                chatContentByExperiment.forEach { chatContent ->
                    add(JsonObject().apply {
                        addProperty("role", if (chatContent.isUserContent) "user" else "assistant")
                        addProperty("content", chatContent.content)
                    })
                }

                // add the new message
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", query)
                })
            })
            addProperty("temperature", 0)
        }
        val response = openaiRepository.postResponse(jsonObject!!)
        val gson = Gson()
        val tempjson = gson.toJson(response.choices.get(0))
        val tempgson = gson.fromJson(tempjson, GptText::class.java)
        insertContent(experiment.id, tempgson.message.content, false)
    }

    fun getContentData(experimentId: Int) = viewModelScope.launch(Dispatchers.IO) {
        _contentList.postValue(databaseRepository.getChatContentByExperimentId(experimentId))
        _deleteCheck.postValue(false)
        _gptInsertCheck.postValue(false)
    }

    fun insertContent(experimentId: Int, content: String, isUserContent: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        databaseRepository.insertContent(experimentId, content, isUserContent)
        if (!isUserContent) {
            _gptInsertCheck.postValue(true)
        }
    }

}