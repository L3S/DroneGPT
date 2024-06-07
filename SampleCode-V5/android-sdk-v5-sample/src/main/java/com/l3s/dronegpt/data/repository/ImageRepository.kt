package com.l3s.dronegpt.data.repository

import android.app.Application
import com.l3s.dronegpt.data.database.AppDatabase
import com.l3s.dronegpt.data.database.Image
import dji.sampleV5.aircraft.DJIApplication

class ImageRepository(application: Application) {
    private val database: AppDatabase = (application as DJIApplication).droneGptDatabase

    fun getAllImages() = database.imageDao().getAllImages()

    fun getImagesByExperimentId(experimentId: Int) = database.imageDao().getImagesByExperimentId(experimentId)

    fun insert(image: Image) = database.imageDao().insertImage(image)
}
