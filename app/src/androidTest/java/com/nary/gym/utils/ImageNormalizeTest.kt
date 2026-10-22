package com.nary.gym.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImageNormalizeTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun largePng_isSavedAsJpegWithinLimits() {
        val png = File(context.cacheDir, "big.png").apply {
            outputStream().use { Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val path = copyImageToInternalStorage(context, Uri.fromFile(png))
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
}
