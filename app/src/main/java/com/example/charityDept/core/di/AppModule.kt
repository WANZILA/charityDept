package com.example.charityDept.core.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module(includes = [NetworkModule::class,
                    RepositoryModule::class,
                    OfflineRepositoryModule::class])
class AppModule {
//    @Provides
//    @Singleton
//    fun provideKidRegistrationRepository(
//        @ApplicationContext context: Context,
//    ): ChildRegistrationRepository = ChildRegistrationRepository(context)
}
