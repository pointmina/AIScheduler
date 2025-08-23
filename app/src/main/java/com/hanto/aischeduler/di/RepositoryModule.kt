package com.hanto.aischeduler.di


import com.hanto.aischeduler.domain.repository.SavedScheduleRepository
import com.hanto.aischeduler.domain.repository.SavedScheduleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSavedScheduleRepository(
        savedScheduleRepositoryImpl: SavedScheduleRepositoryImpl
    ): SavedScheduleRepository
}