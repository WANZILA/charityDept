package com.example.charityDept.core.di

import com.example.charityDept.data.sources.remote.ApiService
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule{
    @Provides
    @Reusable
    @JvmStatic
    fun provideApiService(@Named("myapi") retrofit: Retrofit) : ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
