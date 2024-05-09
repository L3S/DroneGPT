package com.l3s.dronegpt.data.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.l3s.dronegpt.model.ExperimentParameters

@Entity(tableName = "experiments")
data class Experiment(

    val parameters: ExperimentParameters,
    val executedCode: String,
    val logIndex: Int
) {
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
}

@Dao
interface ExperimentDao {

    @Query("SELECT * FROM experiments")
    fun getExperiments(): List<Experiment>

    @Query("SELECT * FROM experiments WHERE id = :experimentId")
    fun getExperimentById(experimentId: Int): LiveData<Experiment>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertExperiment(experiment: Experiment)

    @Query("DELETE FROM experiments WHERE id = :experimentId")
    fun deleteExperiment(experimentId: Int)
}