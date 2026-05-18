package com.example.narytodo.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

fun copyImageToInternalStorage(
    context: Context,
    uri: Uri
): String {

    val directory = File(context.filesDir, "workouts")

    if (!directory.exists()) {
        directory.mkdirs()
    }

    val fileName = "${UUID.randomUUID()}.jpg"

    val file = File(directory, fileName)

    context.contentResolver.openInputStream(uri)?.use { input ->

        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }

    return file.absolutePath
}