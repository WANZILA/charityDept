package com.example.charityDept.core.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ChildImageFileHelper {

    data class CameraCaptureTarget(
        val uri: Uri,
        val absolutePath: String
    )

    fun createCameraTempTarget(
        context: Context,
        childId: String
    ): CameraCaptureTarget {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "child_${childId}_camera_temp.jpg")
        if (file.exists()) file.delete()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return CameraCaptureTarget(
            uri = uri,
            absolutePath = file.absolutePath
        )
    }

    fun getChildProfileFile(
        context: Context,
        childId: String
    ): File {
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        return File(dir, "child_${childId}_profile.jpg")
    }

    fun getChildProfileStagedFile(
        context: Context,
        childId: String
    ): File {
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        return File(dir, "child_${childId}_profile_staged.jpg")
    }

    fun copyUriToChildProfileStagedFile(
        context: Context,
        sourceUri: Uri,
        childId: String
    ): String {
        val dest = getChildProfileStagedFile(context, childId)
        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Unable to open cropped image input stream" }
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return dest.absolutePath
    }

    fun deleteChildProfileStagedFile(
        context: Context,
        childId: String
    ) {
        val staged = getChildProfileStagedFile(context, childId)
        if (staged.exists()) staged.delete()
    }

    fun promoteChildProfileStagedFile(
        context: Context,
        childId: String
    ): String? {
        val staged = getChildProfileStagedFile(context, childId)
        if (!staged.exists()) return null

        val real = getChildProfileFile(context, childId)

        staged.inputStream().use { input ->
            real.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        staged.delete()
        return real.absolutePath
    }
}