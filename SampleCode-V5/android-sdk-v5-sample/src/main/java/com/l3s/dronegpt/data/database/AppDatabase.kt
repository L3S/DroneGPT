package com.l3s.dronegpt.data.database

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [Experiment::class, ChatContent::class, Image::class], version = 8)
abstract class AppDatabase : RoomDatabase() {
    abstract fun experimentDao(): ExperimentDao
    abstract fun chatContentDao(): ChatContentDao
    abstract fun imageDao(): ImageDao
}