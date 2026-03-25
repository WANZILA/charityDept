package com.example.charityDept.core.Utils

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

object HardRestart {
    fun trigger(context: Context) {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            ?: return

        val pi = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try { pi.send() } catch (_: Throwable) {}
        // Kill current process so Firestore fully re-initializes on next start
        exitProcess(0)
    }
}

