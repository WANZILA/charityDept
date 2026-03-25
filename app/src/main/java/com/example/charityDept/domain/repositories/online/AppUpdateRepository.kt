package com.example.charityDept.domain.repositories.online

import com.example.charityDept.core.di.AppUpdateRepositoryRef
import com.example.charityDept.data.model.AppUpdateConfig
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

interface AppUpdateRepository {
    fun streamConfig(): Flow<AppUpdateConfig?>
}

@Singleton
class AppUpdateRepositoryImpl @Inject constructor(
    @AppUpdateRepositoryRef private val appUpdateDoc: DocumentReference // should point to /appConfig/mobile
) : AppUpdateRepository {

    override fun streamConfig(): Flow<AppUpdateConfig?> = callbackFlow {
        val reg = appUpdateDoc.addSnapshotListener { snap, err ->
            if (err != null || snap == null || !snap.exists()) {
                trySend(null)
                return@addSnapshotListener
            }
            val d = snap.data ?: run {
                trySend(null); return@addSnapshotListener
            }

            // Coerce numbers -> String, or pass through existing String
            val minStr = (d["minVersionCode"] as? Number)?.toInt()?.toString()
                ?: (d["minVersionCode"] as? String)
                ?: "1"

            val latestStr = (d["latestVersionCode"] as? Number)?.toInt()?.toString()
                ?: (d["latestVersionCode"] as? String)

            val cfg = AppUpdateConfig(
                minVersionCode    = minStr,
                latestVersionCode = latestStr,
                downloadUrl       = d["downloadUrl"] as? String,
                forceMessage      = d["forceMessage"] as? String,
                softMessage       = d["softMessage"] as? String,
                force             = d["force"] as? Boolean ?: true
            )

            trySend(cfg).isSuccess
        }
        awaitClose { reg.remove() }
    }
}

