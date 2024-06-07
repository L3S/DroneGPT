package com.l3s.dronegpt.data.repository

import android.app.Application
import com.l3s.dronegpt.data.database.AppDatabase
import com.l3s.dronegpt.data.database.ChatContent
import dji.sampleV5.aircraft.DJIApplication

class ChatContentRepository(application: Application) {
    private val database: AppDatabase = (application as DJIApplication).droneGptDatabase

    fun getChatContentByExperimentId(experimentId: Int) = database.chatContentDao().getChatContentByExperimentId(experimentId)

    fun getAllChatContent() = database.chatContentDao().getAllChatContent()

    fun insertContent(experimentId: Int, content: String, isUserContent: Boolean) {
        database.chatContentDao().insertChatContent(
            ChatContent(0, experimentId, content, isUserContent)
        )
    }
}
