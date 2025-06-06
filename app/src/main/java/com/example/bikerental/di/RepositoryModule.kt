package com.example.bikerental.di

import com.example.bikerental.data.repository.BikeRepositoryImpl
import com.example.bikerental.data.repository.PaymentSettingsRepository
import com.example.bikerental.data.repository.RideRepositoryImpl
import com.example.bikerental.domain.repository.BikeRepository
import com.example.bikerental.domain.repository.RideRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
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
    
    @Binds
    @Singleton
    abstract fun bindRideRepository(
        rideRepositoryImpl: RideRepositoryImpl
    ): RideRepository
    
    companion object {
        @Provides
        @Singleton
        fun providePaymentSettingsRepository(): PaymentSettingsRepository {
            return PaymentSettingsRepository()
        }
    }
} 