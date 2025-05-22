package com.example.bikerental.di

import com.example.bikerental.data.repository.BikeRepositoryImpl
import com.example.bikerental.domain.repository.BikeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindBikeRepository(
        bikeRepositoryImpl: BikeRepositoryImpl
    ): BikeRepository
} 