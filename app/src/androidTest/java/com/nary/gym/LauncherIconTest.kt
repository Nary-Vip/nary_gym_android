package com.nary.gym

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherIconTest {
    @Test
    fun appIcon_isVoltAdaptive_withMonochromeLayer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = context.packageManager.getApplicationIcon(context.packageName) as AdaptiveIconDrawable
        assertEquals(0xFFBDF532.toInt(), (icon.background as ColorDrawable).color)
        assertTrue(icon.foreground is VectorDrawable)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) assertTrue(icon.monochrome is VectorDrawable)
    }
}
