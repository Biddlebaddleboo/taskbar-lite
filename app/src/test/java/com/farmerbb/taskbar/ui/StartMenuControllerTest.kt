package com.farmerbb.taskbar.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.os.UserManager
import android.view.Gravity
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.Constants
import com.farmerbb.taskbar.LauncherAppsHelper.generateTestLauncherActivityInfo
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.util.AppEntry
import com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_LEFT
import com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_RIGHT
import com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_LEFT
import com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_RIGHT
import com.farmerbb.taskbar.util.Constants.POSITION_TOP_LEFT
import com.farmerbb.taskbar.util.Constants.POSITION_TOP_RIGHT
import com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_LEFT
import com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_RIGHT
import com.farmerbb.taskbar.util.TaskbarPosition
import com.farmerbb.taskbar.util.U
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.rule.PowerMockRule
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*", "androidx.*")
@PrepareForTest(value = [U::class, TaskbarPosition::class])
class StartMenuControllerTest {
    @get:Rule
    val rule = PowerMockRule()
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
    fun testGetStartMenuLayoutId() {
        Assert.assertEquals(
                R.layout.tb_start_menu_left.toLong(),
                uiController.getStartMenuLayoutId(POSITION_BOTTOM_LEFT)
                        .toLong())
        Assert.assertEquals(
                R.layout.tb_start_menu_right.toLong(),
                uiController.getStartMenuLayoutId(POSITION_BOTTOM_RIGHT)
                        .toLong())
        Assert.assertEquals(
                R.layout.tb_start_menu_top_left.toLong(),
                uiController.getStartMenuLayoutId(POSITION_TOP_LEFT)
                        .toLong())
        Assert.assertEquals(
                R.layout.tb_start_menu_vertical_left.toLong(),
                uiController.getStartMenuLayoutId(POSITION_TOP_VERTICAL_LEFT)
                        .toLong())
        Assert.assertEquals(
                R.layout.tb_start_menu_vertical_left.toLong(),
                uiController.getStartMenuLayoutId(POSITION_BOTTOM_VERTICAL_LEFT)
                        .toLong())
        Assert.assertEquals(
                R.layout.tb_start_menu_top_right.toLong(),
                uiController.getStartMenuLayoutId(POSITION_TOP_RIGHT)
                        .toLong())
        Assert.assertEquals(
                R.layout.tb_start_menu_vertical_right.toLong(),
                uiController.getStartMenuLayoutId(POSITION_TOP_VERTICAL_RIGHT)
                        .toLong())
        Assert.assertEquals(
                R.layout.tb_start_menu_vertical_right.toLong(),
                uiController.getStartMenuLayoutId(POSITION_BOTTOM_VERTICAL_RIGHT)
                        .toLong())
    }

    @Test
    fun testGetStartMenuGravity() {
        Assert.assertEquals(
                (Gravity.BOTTOM or Gravity.LEFT).toLong(),
                uiController.getStartMenuGravity(POSITION_BOTTOM_LEFT).toLong())
        Assert.assertEquals(
                (Gravity.BOTTOM or Gravity.LEFT).toLong(),
                uiController.getStartMenuGravity(POSITION_BOTTOM_VERTICAL_LEFT).toLong())
        Assert.assertEquals(
                (Gravity.BOTTOM or Gravity.RIGHT).toLong(),
                uiController.getStartMenuGravity(POSITION_BOTTOM_RIGHT).toLong())
        Assert.assertEquals(
                (Gravity.BOTTOM or Gravity.RIGHT).toLong(),
                uiController.getStartMenuGravity(POSITION_BOTTOM_VERTICAL_RIGHT).toLong())
        Assert.assertEquals(
                (Gravity.TOP or Gravity.LEFT).toLong(),
                uiController.getStartMenuGravity(POSITION_TOP_LEFT).toLong())
        Assert.assertEquals(
                (Gravity.TOP or Gravity.LEFT).toLong(),
                uiController.getStartMenuGravity(POSITION_TOP_VERTICAL_LEFT).toLong())
        Assert.assertEquals(
                (Gravity.TOP or Gravity.RIGHT).toLong(),
                uiController.getStartMenuGravity(POSITION_TOP_RIGHT).toLong())
        Assert.assertEquals(
                (Gravity.TOP or Gravity.RIGHT).toLong(),
                uiController.getStartMenuGravity(POSITION_TOP_VERTICAL_RIGHT).toLong())
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
