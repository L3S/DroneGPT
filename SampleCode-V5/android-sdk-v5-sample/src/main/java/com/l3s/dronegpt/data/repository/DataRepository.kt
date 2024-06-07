package com.l3s.dronegpt.data.repository

import android.app.Application
import com.l3s.dronegpt.data.database.AppDatabase
import com.l3s.dronegpt.data.database.ChatContent
import com.l3s.dronegpt.data.database.Experiment
import com.l3s.dronegpt.data.database.Image
import dji.sampleV5.aircraft.DJIApplication

class DataRepository(application: Application) {
    private val database: AppDatabase = (application as DJIApplication).droneGptDatabase

    fun getAllData(): DatabaseData {
        val experiments = database.experimentDao().getAllExperimentsSync()
        val chatContent = database.chatContentDao().getAllChatContent()
        val images = database.imageDao().getAllImages()

        return DatabaseData(experiments, chatContent, images)
    }
}

data class DatabaseData(val experiments: List<Experiment>, val chatContent: List<ChatContent>, val images: List<Image>)