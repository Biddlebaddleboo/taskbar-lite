package com.farmerbb.taskbar.helper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherHelperTest {
    private val launcherHelper = LauncherHelper.getInstance()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun testGetInstance() {
        Assert.assertNotNull(launcherHelper)
        for (i in 1..20) {
            Assert.assertEquals(launcherHelper, LauncherHelper.getInstance())
        }
    }

    @Test
    fun testIsHomeScreen() {
        Assert.assertFalse(launcherHelper.isOnHomeScreen(context))
        launcherHelper.setOnPrimaryHomeScreen(true)
        Assert.assertTrue(launcherHelper.isOnHomeScreen(context))
        launcherHelper.setOnPrimaryHomeScreen(false)
        Assert.assertFalse(launcherHelper.isOnHomeScreen(context))
    }
}
