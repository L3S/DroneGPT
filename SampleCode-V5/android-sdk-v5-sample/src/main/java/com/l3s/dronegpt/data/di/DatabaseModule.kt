package com.l3s.dronegpt.data.di

import android.content.Context
import androidx.room.Room
import com.l3s.dronegpt.data.database.AppDatabase
import com.l3s.dronegpt.data.database.ChatContentDao
import com.l3s.dronegpt.data.database.ExperimentDao
import com.l3s.dronegpt.data.database.ImageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    fun provideExperimentDao(appDatabase: AppDatabase): ExperimentDao {
        return appDatabase.experimentDao()
    }

    @Provides
    fun provideChatContentDao(appDatabase: AppDatabase): ChatContentDao {
        return appDatabase.chatContentDao()
    }

    @Provides
    fun provideImageDao(appDatabase: AppDatabase): ImageDao {
        return appDatabase.imageDao()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "DroneGPTdb"
        ).build()
    }
}