package com.example.charityDept.core.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.graphics.Bitmap

object ChildImageFileHelper {

    data class CameraCaptureTarget(
        val uri: Uri,
        val absolutePath: String
    )

    fun createClientProfileCropDestination(
        context: Context,
        clientId: String
    ): Uri {
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val dest = File(dir, "client_${clientId}_profile.jpg")
        if (dest.exists()) dest.delete()
        return Uri.fromFile(dest)
    }

    fun getClientProfileFile(
        context: Context,
        clientId: String
    ): File {
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        return File(dir, "client_${clientId}_profile.jpg")
    }

    fun createCameraTempTarget(
        context: Context,
        clientId: String
    ): CameraCaptureTarget {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "client_${clientId}_camera_temp.jpg")
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

    fun getClientProfileStagedFile(
        context: Context,
        clientId: String
    ): File {
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        return File(dir, "client_${clientId}_profile_staged.jpg")
    }

    fun copyUriToClientProfileStagedFile(
        context: Context,
        sourceUri: Uri,
        clientId: String
    ): String {
        val dest = getClientProfileStagedFile(context, clientId)
        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Unable to open cropped image input stream" }
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return dest.absolutePath
    }

    fun deleteClientProfileStagedFile(
        context: Context,
        clientId: String
    ) {
        val staged = getClientProfileStagedFile(context, clientId)
        if (staged.exists()) staged.delete()
    }

    fun promoteClientProfileStagedFile(
        context: Context,
        clientId: String
    ): String? {
        val staged = getClientProfileStagedFile(context, clientId)
        if (!staged.exists()) return null

        val real = getClientProfileFile(context, clientId)

        staged.inputStream().use { input ->
            real.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        staged.delete()
        return real.absolutePath
    }

// app/src/main/java/com/example/upliftadmin/core/utils/ClientImageFileHelper.kt
// /// CHANGED: Save each client profile image with a unique timestamped filename so camera/gallery preview refreshes immediately.

    fun copyUriToClientProfileFile(
        context: Context,
        sourceUri: Uri,
        clientId: String
    ): String {
        val dir = File(context.filesDir, "images").apply { mkdirs() }
//        val dest = File(dir, "client_${clientId}_profile_${System.currentTimeMillis()}.jpg")
        val dest = File(dir, "client_${clientId}_profile.jpg")
        copyUriToFile(
            resolver = context.contentResolver,
            sourceUri = sourceUri,
            destFile = dest
        )

        return dest.absolutePath
    }

    fun copyCameraTempFileToClientProfileFile(
        context: Context,
        tempFilePath: String,
        clientId: String
    ): String {
        val tempFile = File(tempFilePath)
        require(tempFile.exists()) { "Captured image file not found" }
        require(tempFile.length() > 0L) { "Captured image file is empty" }

        val dir = File(context.filesDir, "images").apply { mkdirs() }
//        val dest = File(dir, "client_${clientId}_profile_${System.currentTimeMillis()}.jpg")
        val dest = File(dir, "client_${clientId}_profile.jpg")
        FileInputStream(tempFile).use { input ->
            FileOutputStream(dest, false).use { output ->
                input.copyTo(output)
                output.flush()
            }
        }

        return dest.absolutePath
    }
    private fun copyUriToFile(
        resolver: ContentResolver,
        sourceUri: Uri,
        destFile: File
    ) {
        resolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(destFile, false).use { output ->
                input.copyTo(output)
                output.flush()
            }
        } ?: throw IllegalStateException("Unable to open selected image")
    }
}