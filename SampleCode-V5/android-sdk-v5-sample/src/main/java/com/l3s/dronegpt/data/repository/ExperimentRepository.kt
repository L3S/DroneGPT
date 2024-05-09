package com.l3s.dronegpt.data.repository

import com.l3s.dronegpt.data.database.Experiment
import com.l3s.dronegpt.data.database.ExperimentDao
import com.l3s.dronegpt.model.ExperimentParameters
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ExperimentRepository {
    val experiments: Flow<List<String>>//TODO: Flow or Livedata?

    suspend fun add(experimentParameters: ExperimentParameters)
    suspend fun setExecutedCode(code: String)
    suspend fun setLogIndex(index: Int)
}

class DefaultExperimentRepository @Inject constructor(
    private val experimentDao: ExperimentDao
) : ExperimentRepository {

}