package com.l3s.dronegpt.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.l3s.dronegpt.model.ExperimentParameters

class ExperimentParametersConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromExperimentParameters(parameters: ExperimentParameters): String {
        return gson.toJson(parameters)
    }

    @TypeConverter
    fun toExperimentParameters(parametersString: String): ExperimentParameters {
        return gson.fromJson(parametersString, ExperimentParameters::class.java)
    }

}
