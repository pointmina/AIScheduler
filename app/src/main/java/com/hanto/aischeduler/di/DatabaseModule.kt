package com.hanto.aischeduler.di

import android.content.Context
import com.hanto.aischeduler.data.database.SavedScheduleDao
import com.hanto.aischeduler.data.database.ScheduleDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideScheduleDatabase(@ApplicationContext context: Context): ScheduleDatabase {
        return ScheduleDatabase.getDatabase(context)
    }

    @Provides
    fun provideScheduleDao(database: ScheduleDatabase): SavedScheduleDao {
        return database.scheduleDao()
    }
}