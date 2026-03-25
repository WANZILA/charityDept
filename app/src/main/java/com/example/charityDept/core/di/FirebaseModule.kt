// com/example/zionkids/core/di/FirestoreModule.kt
package com.example.charityDept.core.di

import android.content.Context
import com.example.charityDept.core.sync.SyncCoordinatorScheduler
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirestoreModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    @ChildrenRef
    fun provideChildrenCollection(db: FirebaseFirestore): CollectionReference =
        db.collection("children")

    @Provides
    @Singleton
    @EventsRef
    fun provideEventsCollection(db: FirebaseFirestore): CollectionReference =
        db.collection("events")

    @Provides
    @Singleton
    @AttendanceRef
    fun provideAttendanceRef(db: FirebaseFirestore): CollectionReference =
        db.collection("attendances")

    @Provides
    @Singleton
    @UsersRef
    fun provideUsersRef(db: FirebaseFirestore): CollectionReference =
        db.collection("users")

    @Provides
    @Singleton
    @DeletedUsersRef
    fun provideDeletedUsersRef(db: FirebaseFirestore): CollectionReference =
        db.collection("deletedUsers")

    @Provides
    @Singleton
    @DeletedUsersRef
    fun provideLockedAccountsRef(db: FirebaseFirestore): CollectionReference =
        db.collection("authAttempts")

    @Provides @Singleton
    fun provideAppConfigMobileDoc(db: FirebaseFirestore): DocumentReference =
        db.collection("appConfig").document("mobile")



    /**
     * Secondary Auth used ONLY for creating new users so we don't disturb the admin's session.
     * IMPORTANT: We DO NOT provide a Firestore instance for this secondary app.
     * All Firestore writes (e.g., /users/{newUid}) must go through the DEFAULT Firestore,
     * which carries the admin's auth and satisfies your Firestore rules.
     */
    @Provides
    @Singleton
    @AdminAuth
    fun provideSecondaryAuth(@ApplicationContext ctx: Context): FirebaseAuth {
        val secondaryName = "adminAuthApp"

        // Reuse existing secondary app if present
        val existing = FirebaseApp.getApps(ctx).firstOrNull { it.name == secondaryName }
        val secondaryApp = if (existing != null) {
            existing
        } else {
            // Initialize secondary app with the SAME options as the default app (same project)
            val defaultApp = FirebaseApp.getInstance() // throws if not initialized yet
            FirebaseApp.initializeApp(ctx, defaultApp.options, secondaryName)!!
        }

        return FirebaseAuth.getInstance(secondaryApp)
    }

    @Provides
    @Singleton
    @AppVersionCode
    fun provideAppVersionCode(@ApplicationContext ctx: Context): Int {
        val pm = ctx.packageManager
        val pkg = pm.getPackageInfo(ctx.packageName, 0)
        // Use longVersionCode when available (API 28+), otherwise versionCode
        return if (android.os.Build.VERSION.SDK_INT >= 28) {
            pkg.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            pkg.versionCode
        }
    }

    // Qualified doc for /appConfig/mobile
    @Provides
    @Singleton
    @AppUpdateRepositoryRef
    fun provideAppConfigMobileDocQualified(db: FirebaseFirestore): DocumentReference =
        db.collection("appConfig").document("mobile")

    // Properly-qualified locked accounts collection
    @Provides
    @Singleton
    @LockedAccountsRef
    fun provideLockedAccountsCollection(db: FirebaseFirestore): CollectionReference =
        db.collection("authAttempts")


    @Provides
    @Singleton
    @TechnicalSkills
    fun provideTechnicalSkillsCollection(db: FirebaseFirestore): CollectionReference =
        db.collection("technicalSkills")


    @Provides
    @Singleton
    @Streets
    fun provideStreetCollection(db: FirebaseFirestore): CollectionReference =
        db.collection("streets")

    @Provides
    @Singleton
    @AssessmentQuestionsRef
    fun provideAssessmentQuestionsRef(firestore: FirebaseFirestore): CollectionReference =
        firestore.collection("assessment_questions")

    @Provides
    @Singleton
    @AssessmentAnswersRef // ✅ CHANGED (was @AssessmentQuestionsRef)
    fun provideAssessmentAnswersRef(firestore: FirebaseFirestore): CollectionReference =
        firestore.collection("assessment_answers")
//    @Provides
//    @Singleton
//    @AssessmentQuestionsRef
//    fun provideAssessmentAnswersRef(firestore: FirebaseFirestore): CollectionReference =
//        firestore.collection("assessment_answers")


}

