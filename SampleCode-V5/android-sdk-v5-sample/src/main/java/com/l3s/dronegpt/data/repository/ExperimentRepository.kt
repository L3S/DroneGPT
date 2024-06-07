package com.l3s.dronegpt.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.l3s.dronegpt.ChatGPTUtility
import com.l3s.dronegpt.data.database.AppDatabase
import com.l3s.dronegpt.data.database.Experiment
import dji.sampleV5.aircraft.DJIApplication

class ExperimentsRepository(application: Application) {
    private val database: AppDatabase = (application as DJIApplication).droneGptDatabase

    fun getAllExperiments(): LiveData<List<Experiment>> =
        database.experimentDao().getAllExperiments()

    fun insertGPT3Experiment(areaDescription: String, flightHeight: Int) =
        database.experimentDao().insertExperiment(
            Experiment(ChatGPTUtility.gpt3Model, areaDescription, flightHeight)
        )

    fun insertGPT4Experiment(areaDescription: String, flightHeight: Int) =
        database.experimentDao().insertExperiment(
            Experiment(ChatGPTUtility.gpt4Model, areaDescription, flightHeight)
        )

    fun setExecutedCode(id: Int, code: String) = database.experimentDao().setExecutedCode(id, code)

    fun setFlightLogs(id: Int, flightLogs: String) = database.experimentDao().setFlightLogs(id, flightLogs)

//    fun setLogsPath(id: Int, flightRecordPath: String, flightCompactLogsPath: String) {
//        database.experimentDao().setFlightRecordPath(id, flightRecordPath)
//        database.experimentDao().setFlightCompactLogsPath(id, flightCompactLogsPath)
//    }
}

