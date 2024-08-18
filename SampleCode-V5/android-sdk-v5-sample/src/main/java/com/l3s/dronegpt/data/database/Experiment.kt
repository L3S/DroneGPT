package com.l3s.dronegpt.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

//TODO: add FlightUtility logs?
@Entity(tableName = "experiments")
data class Experiment(
    val openaiModel: String,
    val areaDescription: String,
    val flightHeight: Int

) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    var executedCode: String = ""
    var flightLogs: String = ""
//    var flightRecordPath: String = ""
//    var flightCompactLogsPath: String = ""
}

@Dao
interface ExperimentDao {

    @Query("SELECT * FROM experiments")
    fun getAllExperiments(): LiveData<List<Experiment>>
    @Query("SELECT * FROM experiments")
    abstract fun getAllExperimentsSync(): List<Experiment>

    @Query("SELECT * FROM experiments WHERE id = :experimentId")
    fun getExperimentById(experimentId: Int): Experiment

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertExperiment(experiment: Experiment)

    @Query("DELETE FROM experiments WHERE id = :experimentId")
    fun deleteExperiment(experimentId: Int): Int

    @Query("UPDATE experiments SET executedCode = :executedCode WHERE id = :experimentId")
    fun setExecutedCode(experimentId: Int, executedCode: String): Int

    @Query("UPDATE experiments SET flightLogs = :flightLogs WHERE id = :experimentId")
    fun setFlightLogs(experimentId: Int, flightLogs: String): Int

}