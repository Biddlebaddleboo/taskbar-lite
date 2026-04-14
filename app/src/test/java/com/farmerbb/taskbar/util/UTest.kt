package com.farmerbb.taskbar.util

import android.Manifest
import android.app.ActivityOptions
import android.app.AlertDialog
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.farmerbb.taskbar.R
import com.farmerbb.taskbar.helper.FreeformHackHelper
import com.farmerbb.taskbar.mockito.BooleanAnswer
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.rule.PowerMockRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowToast
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "android.*", "androidx.*")
@PrepareForTest(U::class)
@LooperMode(LooperMode.Mode.LEGACY)
class UTest {
    @get:Rule
    val rule = PowerMockRule()
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Assert.assertNotNull(context)
    }

    @Test
    @Throws(Exception::class)
    fun testShowPermissionDialogWithAndroidTVSettings() {
        testShowPermissionDialog(
                true,
                context.resources.getString(
                        R.string.tb_permission_dialog_message, U.getAppName(context)) +
                context.resources.getString(
                        R.string.tb_permission_dialog_instructions_tv, U.getAppName(context)),
                R.string.tb_action_open_settings
        )
    }

    @Test
    @Throws(Exception::class)
    fun testShowPermissionDialogNormal() {
        testShowPermissionDialog(
                false,
                context.resources.getString(
                        R.string.tb_permission_dialog_message, U.getAppName(context)) +
                context.resources.getString(
                        R.string.tb_permission_dialog_instructions_phone),
                R.string.tb_action_grant_permission
        )
    }

    @Throws(Exception::class)
    private fun testShowPermissionDialog(
        hasAndroidTVSettings: Boolean,
        message: String,
        buttonTextResId: Int
    ) {
        val onError = RunnableHooker()
        val onFinish = RunnableHooker()
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`<Any>(U::class.java, "hasAndroidTVSettings", context)
                .thenReturn(hasAndroidTVSettings)
        val dialog = U.showPermissionDialog(context, Callbacks(onError, onFinish))
        val shadowDialog = Shadows.shadowOf(dialog)
        val resources = context.resources
        Assert.assertEquals(
                resources.getString(R.string.tb_permission_dialog_title),
                shadowDialog.title
        )
        Assert.assertEquals(message, shadowDialog.message)
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        Assert.assertEquals(resources.getString(buttonTextResId), positiveButton.text)
        Assert.assertFalse(shadowDialog.isCancelable)
        positiveButton.performClick()
        Assert.assertTrue(onFinish.hasRun())
        Assert.assertFalse(onError.hasRun())
    }

    @Test
    fun testShowErrorDialog() {
        val onFinish = RunnableHooker()
        val appOpCommand = "app-op-command"
        val dialog = ReflectionHelpers.callStaticMethod<AlertDialog>(
                U::class.java,
                "showErrorDialog",
                ClassParameter.from(Context::class.java, context),
                ClassParameter.from(String::class.java, appOpCommand),
                ClassParameter.from(Callbacks::class.java, Callbacks(null, onFinish))
        )
        val shadowDialog = Shadows.shadowOf(dialog)
        val resources = context.resources
        Assert.assertEquals(
                resources.getString(R.string.tb_error_dialog_title),
                shadowDialog.title
        )
        Assert.assertEquals(
                resources.getString(
                        R.string.tb_error_dialog_message,
                        context.packageName,
                        appOpCommand
                ),
                shadowDialog.message
        )
        val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        Assert.assertEquals(resources.getString(R.string.tb_action_ok), button.text)
        Assert.assertFalse(shadowDialog.isCancelable)
        button.performClick()
        Assert.assertTrue(onFinish.hasRun())
    }

    @Test
    fun testHasWriteSecureSettingsPermissionForMarshmallowAndAboveVersion() {
        Assert.assertFalse(U.hasWriteSecureSettingsPermission(context))
        val application = ApplicationProvider.getApplicationContext<Application>()
        val shadowApplication = Shadows.shadowOf(application)
        shadowApplication.grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        Assert.assertTrue(U.hasWriteSecureSettingsPermission(context))
    }

    @Test
    @Config(sdk = [21])
    fun testHasWriteSecureSettingsPermissionVersionBelowMarshmallow() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val shadowApplication = Shadows.shadowOf(application)
        shadowApplication.grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
        Assert.assertFalse(U.hasWriteSecureSettingsPermission(context))
    }

    @Test
    fun testShowToast() {
        U.showToast(context, R.string.tb_pin_shortcut_not_supported)
        val toast = ShadowToast.getLatestToast()
        Assert.assertEquals(Toast.LENGTH_SHORT.toLong(), toast.duration.toLong())
        Assert.assertEquals(
                context.resources.getString(R.string.tb_pin_shortcut_not_supported),
                ShadowToast.getTextOfLatestToast()
        )
    }

    @Test
    fun testShowLongToast() {
        U.showToastLong(context, R.string.tb_pin_shortcut_not_supported)
        val toast = ShadowToast.getLatestToast()
        Assert.assertEquals(Toast.LENGTH_LONG.toLong(), toast.duration.toLong())
        Assert.assertEquals(
                context.resources.getString(R.string.tb_pin_shortcut_not_supported),
                ShadowToast.getTextOfLatestToast()
        )
    }

    @Test
    fun testCancelToast() {
        U.showToastLong(context, R.string.tb_pin_shortcut_not_supported)
        val shadowToast = Shadows.shadowOf(ShadowToast.getLatestToast())
        Assert.assertFalse(shadowToast.isCancelled)
        U.cancelToast()
        Assert.assertTrue(shadowToast.isCancelled)
    }

    @Test
    fun testCanEnableFreeformWithNougatAndAboveVersion() {
        Assert.assertTrue(U.canEnableFreeform(context))
    }

    @Test
    @Config(sdk = [23])
    fun testCanEnableFreeformWithMarshmallowAndBelowVersion() {
        Assert.assertFalse(U.canEnableFreeform(context))
    }

    @Test
    fun testHasFreeformSupportWithoutFreeformEnabled() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.canEnableFreeform(context)).thenReturn(false)
        Assert.assertFalse(U.canEnableFreeform(context))
    }

    @Test
    fun testHasFreeformSupportWithFreeformEnabledAndNMR1AboveVersion() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.canEnableFreeform(context)).thenReturn(true)
        Assert.assertFalse(U.hasFreeformSupport(context))
        // Case 1, system has feature freeform.
        val packageManager = context.packageManager
        val shadowPackageManager = Shadows.shadowOf(packageManager)
        shadowPackageManager
                .setSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT, true)
        Assert.assertTrue(U.hasFreeformSupport(context))
        shadowPackageManager
                .setSystemFeature(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT, false)
        // Case 2, enable_freeform_support in Settings.Global is not 0
        Settings.Global.putInt(context.contentResolver, "enable_freeform_support", 1)
        Assert.assertTrue(U.hasFreeformSupport(context))
        Settings.Global.putInt(context.contentResolver, "enable_freeform_support", 0)
    }

    @Test
    @Config(sdk = [25])
    fun testHasFreeformSupportWithFreeformEnabledAndNMR1AndBelowVersion() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.canEnableFreeform(context)).thenReturn(true)
        Assert.assertFalse(U.hasFreeformSupport(context))
        // Case 3, version is less than or equal to N_MRI, and force_resizable_activities
        // in Settings.Global is not 0
        Settings.Global.putInt(context.contentResolver, "force_resizable_activities", 1)
        Assert.assertTrue(U.hasFreeformSupport(context))
        Settings.Global.putInt(context.contentResolver, "force_resizable_activities", 0)
    }

    @Test
    fun testCanBootToFreeform() {
        PowerMockito.spy(U::class.java)
        val hasFreeformSupportAnswer = BooleanAnswer()
        val isOverridingFreeformHackAnswer = BooleanAnswer()
        PowerMockito.`when`(U.hasFreeformSupport(context)).thenAnswer(hasFreeformSupportAnswer)
        PowerMockito.`when`(U.isOverridingFreeformHack(context, true))
                .thenAnswer(isOverridingFreeformHackAnswer)
        // Case 1, all return true
        hasFreeformSupportAnswer.answer = true
        isOverridingFreeformHackAnswer.answer = true
        Assert.assertFalse(U.canBootToFreeform(context))
        // Case 2, true, false
        hasFreeformSupportAnswer.answer = true
        isOverridingFreeformHackAnswer.answer = false
        Assert.assertTrue(U.canBootToFreeform(context))
        // Case 3, false, true
        hasFreeformSupportAnswer.answer = false
        isOverridingFreeformHackAnswer.answer = true
        Assert.assertFalse(U.canBootToFreeform(context))
        // Case 4, false, false
        hasFreeformSupportAnswer.answer = false
        isOverridingFreeformHackAnswer.answer = false
        Assert.assertFalse(U.canBootToFreeform(context))
    }

    @Test
    fun testGetBackgroundTint() {
        val prefs = U.getSharedPreferences(context)
        prefs.edit()
                .putInt(Constants.PREF_BACKGROUND_TINT, Color.GREEN)
                .putBoolean(Constants.PREF_SHOW_BACKGROUND, false)
                .apply()
        // If the SHOW_BACKGROUND is false, it use transparent to replace origin tint.
        Assert.assertEquals(Color.TRANSPARENT.toLong(), U.getBackgroundTint(context).toLong())
        prefs.edit()
                .putInt(Constants.PREF_BACKGROUND_TINT, Color.GREEN)
                .apply()
        Assert.assertEquals(Color.GREEN.toLong(), U.getBackgroundTint(context).toLong())
        prefs.edit().remove(Constants.PREF_BACKGROUND_TINT).apply()
        Assert.assertEquals(
                context.resources.getInteger(R.integer.tb_translucent_gray).toLong(),
                U.getBackgroundTint(context).toLong()
        )
    }

    @Test
    fun testAccentColor() {
        val prefs = U.getSharedPreferences(context)
        prefs.edit().remove(Constants.PREF_ACCENT_COLOR).apply()
        Assert.assertEquals(
                context.resources.getInteger(R.integer.tb_translucent_white).toLong(),
                U.getAccentColor(context).toLong()
        )
        prefs.edit().putInt(Constants.PREF_ACCENT_COLOR, Color.GREEN).apply()
        Assert.assertEquals(Color.GREEN.toLong(), U.getAccentColor(context).toLong())
    }

    @Test
    fun testCanDrawOverlaysWithMarshmallowAndAboveVersion() {
        ShadowSettings.setCanDrawOverlays(true)
        Assert.assertTrue(U.canDrawOverlays(context))
        ShadowSettings.setCanDrawOverlays(false)
        Assert.assertFalse(U.canDrawOverlays(context))
    }

    @Test
    @Config(sdk = [22])
    fun testCanDrawOverlaysWithMarshmallowBelowVersion() {
        Assert.assertTrue(U.canDrawOverlays(context))
    }

    @Test
    fun testIsGame() {
        // We only test for un-support launching games fullscreen, because of
        // we don't have a good method to test code with ApplicationInfo.
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_LAUNCH_GAMES_FULLSCREEN, false).apply()
        Assert.assertFalse(U.isGame(context, context.packageName))
        prefs.edit().putBoolean(Constants.PREF_LAUNCH_GAMES_FULLSCREEN, true).apply()
        Assert.assertFalse(U.isGame(context, context.packageName))
        Assert.assertFalse(U.isGame(context, context.packageName + "un-exist-package"))
    }

    @Test
    fun testGetActivityOptionsWithQAndAboveVersion() {
        testGetActivityOptions(0, 5, 1, 1)
    }

    @Test
    @Config(sdk = [28])
    fun testGetActivityOptionsWithP() {
        // The stack id isn't changed from the default on Chrome OS with Android P
        val stackId = getActivityOptionsStackId(ActivityOptions.makeBasic())
        testGetActivityOptions(0, 5, 1, stackId)
    }

    @Test
    @Config(sdk = [27])
    fun testGetActivityOptionsWithPBelowVersion() {
        testGetActivityOptions(-1, 2, -1, -1)
    }

    private fun testGetActivityOptions(
        defaultStackId: Int,
        freeformStackId: Int,
        stackIdWithoutBrokenApi: Int,
        chromeOsStackId: Int
    ) {
        PowerMockito.spy(U::class.java)
        val hasBrokenSetLaunchBoundsApiAnswer = BooleanAnswer()
        val isChromeOsAnswer = BooleanAnswer()
        PowerMockito.`when`(U.hasBrokenSetLaunchBoundsApi())
                .thenAnswer(hasBrokenSetLaunchBoundsApiAnswer)
        PowerMockito.`when`(U.isChromeOs(context)).thenAnswer(isChromeOsAnswer)
        val originFreeformHackActive = FreeformHackHelper.getInstance().isFreeformHackActive
        checkActivityOptionsStackIdForNonContextMenu(
                context, null, false, defaultStackId
        )
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_PORTRAIT, false, 1
        )
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_PORTRAIT, true, freeformStackId
        )
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_LANDSCAPE, false, 1
        )
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_LANDSCAPE, true, freeformStackId
        )
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.APP_FULLSCREEN, false, 1
        )
        checkActivityOptionsStackIdForNonContextMenu(
                context, ApplicationType.FREEFORM_HACK, false, freeformStackId
        )
        FreeformHackHelper.getInstance().isFreeformHackActive = originFreeformHackActive
        hasBrokenSetLaunchBoundsApiAnswer.answer = true
        checkActivityOptionsStackIdForContextMenu(context, 1)
        hasBrokenSetLaunchBoundsApiAnswer.answer = false
        isChromeOsAnswer.answer = false
        checkActivityOptionsStackIdForContextMenu(context, stackIdWithoutBrokenApi)
        isChromeOsAnswer.answer = true
        checkActivityOptionsStackIdForContextMenu(context, chromeOsStackId)
    }

    private fun checkActivityOptionsStackIdForContextMenu(
        context: Context?,
        stackId: Int
    ) {
        val options = U.getActivityOptions(context, ApplicationType.CONTEXT_MENU, null)
        Assert.assertEquals(stackId.toLong(), getActivityOptionsStackId(options).toLong())
    }

    private fun checkActivityOptionsStackIdForNonContextMenu(
        context: Context?,
        applicationType: ApplicationType?,
        isFreeformHackActive: Boolean,
        stackId: Int
    ) {
        FreeformHackHelper.getInstance().isFreeformHackActive = isFreeformHackActive
        val options = U.getActivityOptions(context, applicationType, null)
        Assert.assertEquals(stackId.toLong(), getActivityOptionsStackId(options).toLong())
    }

    private fun getActivityOptionsStackId(options: ActivityOptions): Int {
        val methodName: String
        methodName = if (U.getCurrentApiVersion() >= 28.0f) {
            "getLaunchWindowingMode"
        } else {
            "getLaunchStackId"
        }
        return ReflectionHelpers.callInstanceMethod(options, methodName)
    }

    @Test
    fun testIsChromeOs() {
        val packageManager = context.packageManager
        val shadowPackageManager = Shadows.shadowOf(packageManager)
        shadowPackageManager.setSystemFeature("org.chromium.arc", true)
        Assert.assertTrue(U.isChromeOs(context))
        shadowPackageManager.setSystemFeature("org.chromium.arc", false)
        Assert.assertFalse(U.isChromeOs(context))
    }

    @Test
    @Config(qualifiers = "sw720dp")
    fun testGetBaseTaskbarSizeWithSW720dp() {
        var initialSize = context.resources.getDimension(R.dimen.tb_base_size_start_plus_divider)
        initialSize += context.resources.getDimension(R.dimen.tb_base_size_collapse_button)
        Assert.assertEquals(initialSize, U.getBaseTaskbarSize(context), 0f)
    }

    @Test
    fun testGetBaseTaskbarSizeWithNormalDimension() {
        var initialSize = context.resources.getDimension(R.dimen.tb_base_size_start_plus_divider)
        initialSize += context.resources.getDimension(R.dimen.tb_base_size_collapse_button)
        Assert.assertEquals(initialSize, U.getBaseTaskbarSize(context), 0f)
    }

    @Test
    fun testInitPrefsForNormalWithCanEnableFreeformAndHackOverrideTrueButNoSupport() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.canEnableFreeform(context)).thenReturn(true)
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK_OVERRIDE, true).apply()
        PowerMockito.`when`(U.hasFreeformSupport(context)).thenReturn(false)
        U.initPrefs(context)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_FREEFORM_HACK, false))
    }

    @Test
    fun testInitPrefsForNormalWithCantEnableFreeform() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.canEnableFreeform(context)).thenReturn(false)
        val prefs = U.getSharedPreferences(context)
        U.initPrefs(context)
        Assert.assertFalse(prefs.getBoolean(Constants.PREF_FREEFORM_HACK, false))
        prefs.edit()
                .putBoolean(Constants.PREF_FREEFORM_HACK, false)
                .putBoolean(Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false)
                .apply()
        U.initPrefs(context)
        Assert.assertFalse(prefs.getBoolean(
                Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false))
        prefs.edit()
                .putBoolean(Constants.PREF_FREEFORM_HACK, true)
                .putBoolean(Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false)
                .apply()
        U.initPrefs(context)
        Assert.assertTrue(prefs.getBoolean(
                Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false))
        prefs.edit()
                .putBoolean(Constants.PREF_FREEFORM_HACK, false)
                .putBoolean(Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, true)
                .apply()
        U.initPrefs(context)
        Assert.assertTrue(prefs.getBoolean(
                Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false))
        prefs.edit()
                .putBoolean(Constants.PREF_FREEFORM_HACK, true)
                .putBoolean(Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, true)
                .apply()
        U.initPrefs(context)
        Assert.assertTrue(prefs.getBoolean(
                Constants.PREF_SHOW_FREEFORM_DISABLED_MESSAGE, false))
    }

    @Test
    fun testIsOverridingFreeformHackForPAndAboveVersion() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.isChromeOs(context)).thenReturn(false)
        // Check preferences
        Assert.assertFalse(U.isOverridingFreeformHack(context, true))
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        Assert.assertTrue(U.isOverridingFreeformHack(context, true))
        prefs.edit().remove(Constants.PREF_FREEFORM_HACK).apply()

        // Don't check preferences
        Assert.assertTrue(U.isOverridingFreeformHack(context, false))
    }

    @Test
    @Config(sdk = [27])
    fun testIsOverridingFreeformHackForPBelowVersion() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.isChromeOs(context)).thenReturn(false)
        // Check preferences
        Assert.assertFalse(U.isOverridingFreeformHack(context, true))
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        Assert.assertFalse(U.isOverridingFreeformHack(context, true))
        prefs.edit().remove(Constants.PREF_FREEFORM_HACK).apply()

        // Don't check preferences
        Assert.assertFalse(U.isOverridingFreeformHack(context, false))
    }

    @Test
    fun testIsOverridingFreeformHackForChromeOS() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.isChromeOs(context)).thenReturn(true)
        Assert.assertFalse(U.isOverridingFreeformHack(context, true))
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        Assert.assertTrue(U.isOverridingFreeformHack(context, true))
        prefs.edit().remove(Constants.PREF_FREEFORM_HACK).apply()
    }

    @Test
    @Config(sdk = [28])
    fun testIsOverridingFreeformHackForChromeOSApi28() {
        PowerMockito.spy(U::class.java)
        PowerMockito.`when`(U.isChromeOs(context)).thenReturn(true)
        // Check preferences
        Assert.assertFalse(U.isOverridingFreeformHack(context, true))
        val prefs = U.getSharedPreferences(context)
        prefs.edit().putBoolean(Constants.PREF_FREEFORM_HACK, true).apply()
        // The default PREF_CHROME_OS_CONTEXT_MENU_FIX is true
        Assert.assertTrue(U.isOverridingFreeformHack(context, true))
        prefs.edit().putBoolean(Constants.PREF_CHROME_OS_CONTEXT_MENU_FIX, false).apply()
        Assert.assertFalse(U.isOverridingFreeformHack(context, true))
        prefs.edit().putBoolean(Constants.PREF_CHROME_OS_CONTEXT_MENU_FIX, true).apply()
        Assert.assertTrue(U.isOverridingFreeformHack(context, true))
        prefs.edit().remove(Constants.PREF_FREEFORM_HACK).apply()
        prefs.edit().remove(Constants.PREF_CHROME_OS_CONTEXT_MENU_FIX).apply()

        // Don't check preferences
        Assert.assertTrue(U.isOverridingFreeformHack(context, false))
        prefs.edit().putBoolean(Constants.PREF_CHROME_OS_CONTEXT_MENU_FIX, false).apply()
        Assert.assertFalse(U.isOverridingFreeformHack(context, false))
        prefs.edit().putBoolean(Constants.PREF_CHROME_OS_CONTEXT_MENU_FIX, true).apply()
        Assert.assertTrue(U.isOverridingFreeformHack(context, false))
        prefs.edit().remove(Constants.PREF_CHROME_OS_CONTEXT_MENU_FIX).apply()
    }

    @Test
    @Config(sdk = [25])
    fun testHasBrokenSetLaunchBoundsApiForApi25() {
        Assert.assertFalse(U.hasBrokenSetLaunchBoundsApi())
    }

    @Test
    @Config(sdk = [26])
    fun testHasBrokenSetLaunchBoundsApiForApi26() {
        Assert.assertTrue(U.hasBrokenSetLaunchBoundsApi())
    }

    @Test
    @Config(sdk = [27])
    fun testHasBrokenSetLaunchBoundsApiForApi27() {
        Assert.assertTrue(U.hasBrokenSetLaunchBoundsApi())
    }

    @Test
    @Config(sdk = [28])
    fun testHasBrokenSetLaunchBoundsApiForApi28() {
        Assert.assertFalse(U.hasBrokenSetLaunchBoundsApi())
    }

    @Test
    fun testWrapContext() {
        val newContext = U.wrapContext(context)
        val themeResource = ReflectionHelpers.getField<Int>(newContext, "mThemeResource")
        Assert.assertTrue(newContext is ContextThemeWrapper)
        Assert.assertNotNull(themeResource)
        Assert.assertEquals(R.style.Taskbar_Dark.toLong(), (themeResource as Int).toLong())
    }

    @Test
    fun testEnableFreeformModeShortcut() {
        PowerMockito.spy(U::class.java)
        val canEnableFreeformAnswer = BooleanAnswer()
        val isOverridingFreeformHackAnswer = BooleanAnswer()
        val isChromeOsAnswer = BooleanAnswer()
        PowerMockito.`when`(U.canEnableFreeform(context)).thenAnswer(canEnableFreeformAnswer)
        PowerMockito.`when`(U.isOverridingFreeformHack(context, false))
                .thenAnswer(isOverridingFreeformHackAnswer)
        PowerMockito.`when`(U.isChromeOs(context)).thenAnswer(isChromeOsAnswer)
        canEnableFreeformAnswer.answer = false
        isOverridingFreeformHackAnswer.answer = false
        isChromeOsAnswer.answer = false
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = false
        isOverridingFreeformHackAnswer.answer = false
        isChromeOsAnswer.answer = true
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = false
        isOverridingFreeformHackAnswer.answer = true
        isChromeOsAnswer.answer = false
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = false
        isOverridingFreeformHackAnswer.answer = true
        isChromeOsAnswer.answer = true
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = true
        isOverridingFreeformHackAnswer.answer = false
        isChromeOsAnswer.answer = false
        Assert.assertTrue(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = true
        isOverridingFreeformHackAnswer.answer = false
        isChromeOsAnswer.answer = true
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = true
        isOverridingFreeformHackAnswer.answer = true
        isChromeOsAnswer.answer = false
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
        canEnableFreeformAnswer.answer = true
        isOverridingFreeformHackAnswer.answer = true
        isChromeOsAnswer.answer = true
        Assert.assertFalse(U.enableFreeformModeShortcut(context))
    }

    @Test
    @Config(sdk = [26])
    fun testGetOverlayTypeForOAndAboveVersion() {
        Assert.assertEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY.toLong(),
                U.getOverlayType().toLong())
    }

    @Test
    @Config(sdk = [25])
    fun testGetOverlayTypeForOBelowVersion() {
        Assert.assertEquals(
                WindowManager.LayoutParams.TYPE_PHONE.toLong(),
                U.getOverlayType().toLong()
        )
    }

    @Test
    fun testIsDesktopIconEnabled() {
        Assert.assertFalse(U.isDesktopIconsEnabled(context))
    }

    @Test
    fun testApplyDisplayCutoutModeToWithPAndAboveVersion() {
        val layoutParams = WindowManager.LayoutParams()
        Assert.assertTrue(U.applyDisplayCutoutModeTo(layoutParams))
        Assert.assertEquals(
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES.toLong(),
                layoutParams.layoutInDisplayCutoutMode.toLong()
        )
    }

    @Test
    @Config(sdk = [27])
    fun testApplyDisplayCutoutModeToWithBelowVersion() {
        val layoutParams = WindowManager.LayoutParams()
        Assert.assertFalse(U.applyDisplayCutoutModeTo(layoutParams))
    }

    @Test
    fun testSendBroadcast() {
        val receiver = TestBroadcastReceiver()
        val filter = IntentFilter(TestBroadcastReceiver.ACTION)
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        U.sendBroadcast(context, TestBroadcastReceiver.ACTION)
        Assert.assertTrue(receiver.onReceived)
        receiver.onReceived = false
        U.sendBroadcast(context, Intent(TestBroadcastReceiver.ACTION))
        Assert.assertTrue(receiver.onReceived)
    }

    private class TestBroadcastReceiver : BroadcastReceiver() {
        var onReceived = false
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION != intent.action) {
                return
            }
            onReceived = true
        }

        companion object {
            const val ACTION = "test-broadcast-receiver-action"
        }
    }
}
