package com.example.narytodo.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID
import java.net.URL

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
        URL(imageUrl).openStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return imageUrl
    }
    return file.absolutePath
}