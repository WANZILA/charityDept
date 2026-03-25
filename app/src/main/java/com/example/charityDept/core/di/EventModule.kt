//package com.example.charityDept.core.di
//
//import com.example.charityDept.domain.repositories.online.ChildrenRepository
//import com.example.charityDept.domain.repositories.online.ChildrenRepositoryImpl
//import com.google.firebase.firestore.CollectionReference
//import com.google.firebase.firestore.FirebaseFirestore
//import dagger.Binds
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object EventModule {
//    @Provides
//    @Singleton
//    fun provideEventsCollection(db: FirebaseFirestore): CollectionReference =
//        db.collection("events")
//}
//
//

