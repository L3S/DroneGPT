package com.l3s.dronegpt.data.repository

import com.l3s.dronegpt.data.database.Image
import com.l3s.dronegpt.data.database.ImageDao
import javax.inject.Inject

interface ImageRepository {
    fun getImagesByExperimentId(experimentId: Int)
    suspend fun add(image: Image)
}

class DefaultImageRepository @Inject constructor(
    private val imageDao: ImageDao
) : ImageRepository {

}