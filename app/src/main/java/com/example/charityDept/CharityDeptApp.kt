package com.example.charityDept

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.PackageInfoFlags
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.charityDept.core.sync.CleanerScheduler
import com.example.charityDept.core.sync.SyncCoordinatorScheduler
import com.example.charityDept.data.local.dao.ChildDao
import com.example.charityDept.data.local.db.AppDatabase
import com.example.charityDept.data.local.seed.AttendencesSeedScheduler
import com.example.charityDept.data.local.seed.ChildrenSeedScheduler
import com.example.charityDept.data.local.seed.EventsSeedScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Inject
import com.example.charityDept.data.local.seed.AssessmentTaxonomySeedScheduler

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestore

@HiltAndroidApp
class CharityDeptApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    // ✅ FIX: your interface expects the PROPERTY, not the function
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DbEntryPoint {
        fun appDb(): AppDatabase
        fun childDao(): ChildDao
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())


        // ✅ Seed scheduler (idempotent via unique work)
        EventsSeedScheduler.enqueue(this)
        AttendencesSeedScheduler.enqueue(this)
        ChildrenSeedScheduler.enqueue(this)
        AssessmentTaxonomySeedScheduler.enqueue(this)


        // Firestore persistent cache BEFORE any Firestore access
        runCatching {
            val cacheSettings = PersistentCacheSettings.newBuilder()
                .setSizeBytes(500L * 1024 * 1024) // 500MB
                .build()

            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()

            Firebase.firestore.firestoreSettings = settings

            // --- versionName / versionCode without BuildConfig ---
            val pm = applicationContext.packageManager
            val pkg = applicationContext.packageName
            val pInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkg, PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkg, 0)
            }

            val versionName = pInfo.versionName ?: "unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (pInfo.longVersionCode).toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }

            // --- buildType without BuildConfig ---
            val isDebug = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val buildType = if (isDebug) "debug" else "release"

            // Crashlytics keys
            Firebase.crashlytics.setCrashlyticsCollectionEnabled(!isDebug)

            Firebase.crashlytics.setCustomKey("version_name", versionName)
            Firebase.crashlytics.setCustomKey("version_code", versionCode)
            Firebase.crashlytics.setCustomKey("build_type", buildType)

            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            Firebase.crashlytics.setCustomKey("device_name", deviceName)
            Firebase.crashlytics.setCustomKey("brand", Build.BRAND)
            Firebase.crashlytics.setCustomKey("device_code", Build.DEVICE)
            Firebase.crashlytics.setCustomKey("product", Build.PRODUCT)
            Firebase.crashlytics.setCustomKey("android_sdk", Build.VERSION.SDK_INT)
            Firebase.crashlytics.setCustomKey("android_release", Build.VERSION.RELEASE)

            // Attach/clear user on auth state changes
            Firebase.auth.addAuthStateListener { auth ->
                val u = auth.currentUser
                if (u != null) {
                    Firebase.crashlytics.setUserId(u.uid)
                    Firebase.crashlytics.setCustomKey("user_name", (u.displayName ?: "unknown").take(64))
                    val emailHash = u.email?.let { sha256(it) } ?: ""
                    Firebase.crashlytics.setCustomKey("user_email_hash", emailHash)
                    Firebase.crashlytics.setCustomKey("user_alias", (u.email?.substringBefore('@') ?: "guest").take(32))
                } else {
                    Firebase.crashlytics.setUserId("")
                    Firebase.crashlytics.setCustomKey("user_name", "")
                    Firebase.crashlytics.setCustomKey("user_email_hash", "")
                    Firebase.crashlytics.setCustomKey("user_alias", "")
                }
            }
        }

        // Prewarm DB so Database Inspector shows it as OPEN
        val ep = EntryPointAccessors.fromApplication(this, DbEntryPoint::class.java)
        val db = ep.appDb()
        db.openHelper.writableDatabase.query("SELECT 1")

        // Schedule ongoing sync
//        ChildrenSyncScheduler.enqueuePeriodicPush(this)
//        ChildrenSyncScheduler.enqueuePeriodicPull(this)

        CleanerScheduler.enqueuePeriodic(this, retentionDays = 30L)

        SyncCoordinatorScheduler.enqueuePushAllNow(this, cleanerRetentionDays = 30L)
        SyncCoordinatorScheduler.enqueuePullAllNow(this, cleanerRetentionDays = 30L)

        SyncCoordinatorScheduler.enqueuePullAllPeriodic(this, cleanerRetentionDays = 30L)
        SyncCoordinatorScheduler.enqueuePushAllPeriodic(this, cleanerRetentionDays = 30L)
    }

    companion object {
        private fun sha256(text: String): String {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            return md.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}

