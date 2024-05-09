package com.l3s.dronegpt.data.repository

import com.l3s.dronegpt.data.database.ChatContent
import com.l3s.dronegpt.data.database.ChatContentDao
import javax.inject.Inject

interface ChatContentRepository {
    fun getChatContentByExperimentId(experimentId: Int)

    suspend fun addChatContent(chatContent: ChatContent)
}

class DefaultChatContentRepository @Inject constructor(
    private val chatContentDao: ChatContentDao
) : ChatContentRepository {

}