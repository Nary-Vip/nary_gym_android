package com.nary.gym.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

private const val DOWNLOAD_TIMEOUT_MS = 15_000
private const val MAX_IMAGE_SIDE = 1600
private const val JPEG_QUALITY = 85

suspend fun copyImageToInternalStorage(
    context: Context,
    uri: Uri
): String {
    val file = File(File(context.filesDir, "workouts"), "${UUID.randomUUID()}.jpg")
    return try {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            val bitmap = try {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val scale = MAX_IMAGE_SIDE.toFloat() / maxOf(info.size.width, info.size.height)
                    if (scale < 1f) {
                        decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
                    }
                }
            } catch (e: IOException) {
                Log.w("Imageutils", "Failed to decode picked image", e)
                return@withContext ""
            }
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            bitmap.recycle()
            file.absolutePath
        }
    } catch (e: CancellationException) {
        file.delete()
        throw e
    }
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