package com.example.charityDept.domain.repositories.online

//// domain/repositories/online/InviteRepository.kt
//package com.example.charityDept.domain.repositories.online

import android.app.Application
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InviteRepository @Inject constructor(
    private val app: Application,
    private val auth: FirebaseAuth
) {
    // Configure to match your Console settings
    private fun actionCodeSettings(): ActionCodeSettings =
        ActionCodeSettings.newBuilder()
            .setUrl("https://yourapp.page.link/invite") // TODO: put your link here
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                app.packageName,
                true,  // install if not available
                "33"   // minimum version (or null)
            )
            .build()

    /**
     * Sends a passwordless sign-in link. The account is created when the recipient clicks the link.
     */
    suspend fun sendInvite(email: String) {
        auth.sendSignInLinkToEmail(email.trim(), actionCodeSettings()).await()
    }
}

