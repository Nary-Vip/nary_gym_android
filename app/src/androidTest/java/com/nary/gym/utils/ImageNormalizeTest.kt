package com.nary.gym.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImageNormalizeTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun bigPng() = File(context.cacheDir, "big.png").apply {
        outputStream().use { Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun largePng_isSavedAsJpegWithinLimits() {
        val png = bigPng()
        val path = runBlocking { copyImageToInternalStorage(context, Uri.fromFile(png)) }
        val out = File(path)
        try {
            assertTrue(path.endsWith(".jpg"))
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeFile(path, it) }
            assertEquals("image/jpeg", bounds.outMimeType)
            assertEquals(1600, maxOf(bounds.outWidth, bounds.outHeight))
            assertTrue(out.length() < 5 * 1024 * 1024)
        } finally {
            out.delete()
            png.delete()
        }
    }

    @Test
    fun cancelledConversion_leavesNoFileBehind() = runBlocking {
        val png = bigPng()
        val directory = File(context.filesDir, "workouts")
        val before = directory.listFiles().orEmpty().toSet()
        try {
            val job = launch { copyImageToInternalStorage(context, Uri.fromFile(png)) }
            yield()
            val deadline = System.currentTimeMillis() + 10_000
            while (directory.listFiles().orEmpty().toSet() == before) {
                assertTrue("conversion never started writing", System.currentTimeMillis() < deadline)
                Thread.sleep(1)
            }
            job.cancel()
            job.join()
            assertEquals(before, directory.listFiles().orEmpty().toSet())
        } finally {
            (directory.listFiles().orEmpty().toSet() - before).forEach { it.delete() }
            png.delete()
        }
    }
}
