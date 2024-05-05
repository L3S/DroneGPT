package com.l3s.dronegpt.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "experiments")
data class Experiment(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val parameters: ExperimentParameters

)