package com.farmerbb.taskbar.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.os.UserManager
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.Constants
import com.farmerbb.taskbar.LauncherAppsHelper.generateTestLauncherActivityInfo
import com.farmerbb.taskbar.util.AppEntry
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StartMenuControllerTest {
    private lateinit var uiController: StartMenuController
    private lateinit var context: Context
    private val host: UIHost = MockUIHost()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        uiController = StartMenuController(context)
        uiController.onCreateHost(host)
    }

    @After
    fun tearDown() {
        uiController.onDestroyHost(host)
    }

    @Test
    fun testGenerateAppEntries() {
        val queryList: MutableList<LauncherActivityInfo> = ArrayList()
        val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val packageManager = context.packageManager
        var appEntries = uiController.generateAppEntries(context, userManager,
                packageManager, queryList)
        Assert.assertEquals(0, appEntries.size.toLong())
        val activityInfo = ActivityInfo()
        activityInfo.packageName = Constants.TEST_PACKAGE
        activityInfo.name = Constants.TEST_LABEL
        activityInfo.nonLocalizedLabel = activityInfo.name
        activityInfo.applicationInfo = ApplicationInfo()
        activityInfo.applicationInfo.packageName = activityInfo.packageName
        val launcherActivityInfo = generateTestLauncherActivityInfo(
                context, activityInfo, Constants.DEFAULT_TEST_USER_ID
        )
        queryList.add(launcherActivityInfo)
        appEntries = uiController.generateAppEntries(context, userManager,
                packageManager, queryList)
        Assert.assertEquals(1, appEntries.size.toLong())
        verifyAppEntryContent(activityInfo, appEntries[0])
        queryList.add(launcherActivityInfo)
        appEntries = uiController.generateAppEntries(context, userManager,
                packageManager, queryList)
        Assert.assertEquals(2, appEntries.size.toLong())
        verifyAppEntryContent(activityInfo, appEntries[0])
        verifyAppEntryContent(activityInfo, appEntries[1])
    }

    private fun verifyAppEntryContent(activityInfo: ActivityInfo, appEntry: AppEntry) {
        Assert.assertEquals(activityInfo.nonLocalizedLabel, appEntry.label)
        Assert.assertEquals(activityInfo.packageName, appEntry.packageName)
        val componentNameOne = ComponentName(activityInfo.packageName, activityInfo.name)
        Assert.assertEquals(componentNameOne.flattenToString(), appEntry.componentName)
        Assert.assertEquals(Constants.DEFAULT_TEST_USER_ID.toLong(), appEntry.getUserId(context))
    }
}
