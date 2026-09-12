package com.nary.gym.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL

private const val DOWNLOAD_TIMEOUT_MS = 15_000

fun copyImageToInternalStorage(
    context: Context,
    uri: Uri
): String {
    val directory = File(context.filesDir, "workouts")
    if (!directory.exists()) {
        directory.mkdirs()
    }

    // Get the correct extension from the Uri
    val extension = context.contentResolver.getType(uri)?.let { mimeType ->
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    } ?: "jpg"

    val fileName = "${UUID.randomUUID()}.$extension"
    val file = File(directory, fileName)

    context.contentResolver.openInputStream(uri)?.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    return file.absolutePath
}

fun downloadImageToInternalStorage(
    context: Context,
    imageUrl: String
): String {
    if (!imageUrl.startsWith("http", ignoreCase = true)) {
        return imageUrl
    }

    val directory = File(context.filesDir, "workouts")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val extension = MimeTypeMap.getFileExtensionFromUrl(imageUrl).ifBlank { "jpg" }
    val fileName = "${imageUrl.hashCode()}.$extension"
    val file = File(directory, fileName)
    if (file.exists()) return file.absolutePath

    try {
        val connection = URL(imageUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = DOWNLOAD_TIMEOUT_MS
        connection.readTimeout = DOWNLOAD_TIMEOUT_MS
        connection.inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) {
        Log.w("Imageutils", "Failed to download workout image: $imageUrl", e)
        return imageUrl
    }
    return file.absolutePath
}