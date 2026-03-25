// com/example/zionkids/core/di/RepositoryModule.kt
package com.example.charityDept.core.di


import com.example.charityDept.domain.repositories.online.AppUpdateRepository
import com.example.charityDept.domain.repositories.online.AppUpdateRepositoryImpl
import com.example.charityDept.domain.repositories.online.AttendanceRepository
import com.example.charityDept.domain.repositories.online.AttendanceRepositoryImpl
import com.example.charityDept.domain.repositories.online.AuthRepository
import com.example.charityDept.domain.repositories.online.AuthRepositoryImpl
import com.example.charityDept.domain.repositories.online.ChildrenRepository
import com.example.charityDept.domain.repositories.online.ChildrenRepositoryImpl
import com.example.charityDept.domain.repositories.online.EventsRepository
import com.example.charityDept.domain.repositories.online.EventsRepositoryImpl
import com.example.charityDept.domain.repositories.online.LockedAccountsRepository
import com.example.charityDept.domain.repositories.online.LockedAccountsRepositoryImpl
//import com.example.charityDept.domain.repositories.online.StreetRepository
import com.example.charityDept.domain.repositories.online.StreetsRepository
import com.example.charityDept.domain.repositories.online.StreetsRepositoryImpl
import com.example.charityDept.domain.repositories.online.TechnicalSkillsRepository
import com.example.charityDept.domain.repositories.online.TechnicalSkillsRepositoryImpl
import com.example.charityDept.domain.repositories.online.UsersRepository
import com.example.charityDept.domain.repositories.online.UsersRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChildrenRepository(impl: ChildrenRepositoryImpl): ChildrenRepository

    @Binds
    @Singleton
    abstract fun bindEventsRepository(impl: EventsRepositoryImpl): EventsRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(impl: AttendanceRepositoryImpl): AttendanceRepository

    @Binds
    @Singleton
    abstract fun bindUsersRepository(impl: UsersRepositoryImpl): UsersRepository

    @Binds
    @Singleton
    abstract fun bindLockedAccountsRepository(impl: LockedAccountsRepositoryImpl): LockedAccountsRepository


    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(impl: AppUpdateRepositoryImpl): AppUpdateRepository

    @Binds
    @Singleton
    abstract fun bindTechnicalSkillsRepository(impl: TechnicalSkillsRepositoryImpl): TechnicalSkillsRepository

    @Binds
    @Singleton
    abstract fun bindStreetsRepository(impl: StreetsRepositoryImpl): StreetsRepository


}

