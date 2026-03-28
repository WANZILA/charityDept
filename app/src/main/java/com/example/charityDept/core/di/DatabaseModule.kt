package com.example.charityDept.core.di


import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.charityDept.core.sync.SyncCoordinatorScheduler
import com.example.charityDept.data.local.dao.AssessmentTaxonomyDao
//import com.example.charityDept.data.dao.UgAdminDao
import com.example.charityDept.data.local.dao.AttendanceDao
import com.example.charityDept.data.local.dao.ChildDao
import com.example.charityDept.data.local.dao.EventDao
import com.example.charityDept.data.local.dao.FamilyDao
import com.example.charityDept.data.local.dao.KpiDao
import com.example.charityDept.data.local.dao.UgAdminDao
import com.example.charityDept.data.local.db.AppDatabase
import com.example.charityDept.data.local.seed.AssessmentTaxonomySeeder
import com.example.charityDept.data.local.seed.UgAdminSeeder
//import com.example.charityDept.data.db.AppDatabase
//import com.example.charityDept.data.model.ChildDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Qualifier
import javax.inject.Singleton

private const val DB_NAME = "zionkids.db"

// /// CHANGED: SQL for a handy inspector view (no entity changes)
private const val CREATE_CHILDREN_DEBUG_VIEW = """
CREATE VIEW IF NOT EXISTS children_debug AS
SELECT
  childId,
  fName,
  lName,
  -- millis (raw) for reference
  createdAt              AS createdAt_ms,
  updatedAt              AS updatedAt_ms,
  dob                    AS dob_ms,
  leftHomeDate           AS leftHomeDate_ms,
  leaveStreetDate        AS leaveStreetDate_ms,
  acceptedJesusDate      AS acceptedJesusDate_ms,
  resettlementDate       AS resettlementDate_ms,
  -- human-friendly local times
  CASE WHEN createdAt IS NOT NULL
       THEN strftime('%Y-%m-%d %H:%M', createdAt/1000, 'unixepoch','localtime')
       ELSE NULL END     AS createdAt_local,
  CASE WHEN updatedAt IS NOT NULL
       THEN strftime('%Y-%m-%d %H:%M', updatedAt/1000, 'unixepoch','localtime')
       ELSE NULL END     AS updatedAt_local,
  CASE WHEN dob IS NOT NULL
       THEN strftime('%Y-%m-%d', dob/1000, 'unixepoch','localtime')
       ELSE NULL END     AS dob_local,
  CASE WHEN leftHomeDate IS NOT NULL
       THEN strftime('%Y-%m-%d %H:%M', leftHomeDate/1000, 'unixepoch','localtime')
       ELSE NULL END     AS leftHomeDate_local,
  CASE WHEN leaveStreetDate IS NOT NULL
       THEN strftime('%Y-%m-%d %H:%M', leaveStreetDate/1000, 'unixepoch','localtime')
       ELSE NULL END     AS leaveStreetDate_local,
  CASE WHEN acceptedJesusDate IS NOT NULL
       THEN strftime('%Y-%m-%d %H:%M', acceptedJesusDate/1000, 'unixepoch','localtime')
       ELSE NULL END     AS acceptedJesusDate_local,
  CASE WHEN resettlementDate IS NOT NULL
       THEN strftime('%Y-%m-%d %H:%M', resettlementDate/1000, 'unixepoch','localtime')
       ELSE NULL END     AS resettlementDate_local
FROM children;
"""

//@Qualifier
//@ChildDaoRef
//@Retention(AnnotationRetention.BINARY)
//annotation class ChildDaoRef

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
//            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8
            )

// /// CHANGED: keep debug view
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(CREATE_CHILDREN_DEBUG_VIEW)
                }
                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.execSQL(CREATE_CHILDREN_DEBUG_VIEW)
                }
            // /// CHANGED: create the human-readable view on first create and on open (idempotent)
//            .addCallback(object : RoomDatabase.Callback() {
//                override fun onCreate(db: SupportSQLiteDatabase) {
//                    db.execSQL(CREATE_CHILDREN_DEBUG_VIEW)
//                }
//                override fun onOpen(db: SupportSQLiteDatabase) {
//                    db.execSQL(CREATE_CHILDREN_DEBUG_VIEW)
//                }
            })
            .build()
            // /// CHANGED: seed admin tables once (safe: checks count)
            .also { db ->
                CoroutineScope(Dispatchers.IO).launch {
                    UgAdminSeeder(context, db.ugAdminDao()).seedIfEmpty()
                    AssessmentTaxonomySeeder(db.assessmentTaxonomyDao()).seedIfEmpty()
                }
            }
//        fun provideDb(@ApplicationContext context: Context): AppDatabase =
//        Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
//            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // /// CHANGED: WAL enabled
//            .build()
//    fun provideDb(@ApplicationContext context: Context): AppDatabase =
//        Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
//            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
//            /// CHANGED: apply 1→2 migration (non-destructive)
//            .addMigrations(AppDatabase.MIGRATION_1_2)
//            // .fallbackToDestructiveMigration() // optional for debug only
//            .build()
//    fun provideDb(@ApplicationContext context: Context): AppDatabase =
//        Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
//            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING) // /// CHANGED: WAL enabled
//            .build()

    @Provides
    @Singleton
//    @ChildDaoRef
    fun provideChildDao(db: AppDatabase): ChildDao = db.childDao()

    @Provides
    @Singleton
    fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()

    @Provides
    @Singleton
    fun provideFamilyDao(db: AppDatabase): FamilyDao = db.familyDao()


    @Provides
    @Singleton
    fun provideKpiDao(db: AppDatabase): KpiDao = db.kpiDao()

    @Provides
    @Singleton
    fun provideAttendanceDao(db: AppDatabase): AttendanceDao = db.attendanceDao()


    // /// CHANGED: new dao provider
    @Provides @Singleton
    fun provideUgAdminDao(db: AppDatabase): UgAdminDao = db.ugAdminDao()
    @Provides
    @Singleton
    fun provideSyncCoordinatorScheduler(): SyncCoordinatorScheduler = SyncCoordinatorScheduler

    @Provides
    @Singleton
    fun provideAssessmentQuestionDao(db: AppDatabase) = db.assessmentQuestionDao()

    @Provides
    @Singleton
    fun provideAssessmentAnswerDao(db: AppDatabase) = db.assessmentAnswerDao()

    @Provides
    @Singleton
    fun provideAssessmentTaxonomyDao(db: AppDatabase): AssessmentTaxonomyDao =
        db.assessmentTaxonomyDao()


}

