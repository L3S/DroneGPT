package com.l3s.dronegpt.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.l3s.dronegpt.data.database.Experiment
import com.l3s.dronegpt.data.database.Image
import com.l3s.dronegpt.data.repository.DataRepository
import com.l3s.dronegpt.data.repository.ExperimentsRepository
import com.l3s.dronegpt.data.repository.ImageRepository
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sdk.keyvalue.value.camera.GeneratedMediaFileInfo
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class DroneGPTViewModel(application: Application) : AndroidViewModel(application) {

    private val experimentRepository = ExperimentsRepository(application)
    private val imageRepository = ImageRepository(application)
    private val dataRepository = DataRepository(application)

    val experiments: LiveData<List<Experiment>> = experimentRepository.getAllExperiments()
    val selectedExperiment = MutableLiveData<Experiment>()

    private var _experimentsList = MutableLiveData<List<Experiment>>()
    val experimentsList : LiveData<List<Experiment>>
        get() = _experimentsList

    private var _deleteCheck = MutableLiveData<Boolean>(false)
    val deleteCheck : LiveData<Boolean>
        get() = _deleteCheck

    fun setSelectedExperiment(experiment: Experiment) {
        selectedExperiment.value = experiment
    }

    fun getAllExperiments() = viewModelScope.launch(Dispatchers.IO) {
        _experimentsList.postValue(experimentRepository.getAllExperiments().value)
        _deleteCheck.postValue(false)
    }

    fun createGPT3Experiment(areaDescription: String, flightHeight: Int) =  viewModelScope.launch(Dispatchers.IO) {
        experimentRepository.insertGPT3Experiment(areaDescription, flightHeight)
    }

    fun createGPT4Experiment(areaDescription: String, flightHeight: Int) = viewModelScope.launch(Dispatchers.IO) {
        experimentRepository.insertGPT4Experiment(areaDescription, flightHeight)
    }

    fun setExperimentExecutedCode(experimentId: Int, executedCode: String) = viewModelScope.launch(Dispatchers.IO) {
        experimentRepository.setExecutedCode(experimentId, executedCode)
    }

    fun setExperimentFlightLogs(experimentId: Int, flightLogs: String) = viewModelScope.launch(Dispatchers.IO) {
        experimentRepository.setFlightLogs(experimentId, flightLogs)
    }

    fun createImage(experimentId: Int, generatedImageInfo: GeneratedMediaFileInfo, captureLocation: LocationCoordinate3D) =  viewModelScope.launch(Dispatchers.IO) {
        imageRepository.insert(
            Image(0, experimentId, generatedImageInfo.index, generatedImageInfo.fileSize, generatedImageInfo.createTime.toString(), captureLocation)
        )
    }

    fun exportDataToJsonFile(externalFilesDir: File?) = viewModelScope.launch(Dispatchers.IO) {
        val data = dataRepository.getAllData()

        try {
            val fileName = "dronegpt_app_database_${System.currentTimeMillis()}.txt"
            val outputFile = File(externalFilesDir, fileName)
            val gson = Gson()
            outputFile.writeText(gson.toJson(data))
            ToastUtils.showToast("data successfully exported to json file")
        } catch (e: IOException) {
            ToastUtils.showToast("could not export data: $e")
        }

    }


//    fun setLogsPath(experimentId: Int, flightRecordPath: String, flightCompactLogsPath: String) {
//        experimentRepository.setLogsPath(experimentId, flightRecordPath, flightCompactLogsPath)
//    }


//    fun deleteSelectedContent(id : Int) = viewModelScope.launch(Dispatchers.IO) {
//        databaseRepository.deleteSelectedContent(id)
//        _deleteCheck.postValue(true)
//    }

}