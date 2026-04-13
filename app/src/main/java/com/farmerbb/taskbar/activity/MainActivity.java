/* Copyright 2016 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.taskbar.activity;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.fragment.AboutFragment;
import com.farmerbb.taskbar.fragment.SettingsFragment;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.StartMenuService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.helper.GlobalHelper;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.util.CompatUtils;
import com.farmerbb.taskbar.util.DependencyUtils;
import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.util.U;
import com.google.android.material.snackbar.Snackbar;

import java.util.Arrays;
import java.util.Collections;

import static com.farmerbb.taskbar.util.Constants.*;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat theSwitch;
    private ImageView helpButton;

    private final BroadcastReceiver switchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSwitch();
        }
    };

    private boolean hasCaption = false;

    private final int latestChangelogVersion = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(U.relaunchActivityIfNeeded(this)) return;

        U.registerReceiver(this, switchReceiver, ACTION_UPDATE_SWITCH);

        final SharedPreferences pref = U.getSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();

        if(!U.isLibrary(this)) {
            boolean isRelaunched = getIntent().hasExtra("is_relaunched");
            int lightTheme = isRelaunched ? R.style.Taskbar_Floating : R.style.Taskbar;
            int darkTheme = isRelaunched ? R.style.Taskbar_Dark_Floating : R.style.Taskbar_Dark;

            setTheme(U.isDarkTheme(this) ? darkTheme : lightTheme);
        } else {
            int theme = getIntent().getIntExtra("theme", -1);
            if(theme != -1)
                setTheme(theme);
        }

        if(pref.getBoolean(PREF_TASKBAR_ACTIVE, false) && !U.isServiceRunning(this, NotificationService.class))
            editor.putBoolean(PREF_TASKBAR_ACTIVE, false);

        boolean launcherEnabled = (pref.getBoolean(PREF_LAUNCHER, true) && U.canDrawOverlays(this))
                || U.isLauncherPermanentlyEnabled(this);

        if(pref.contains(PREF_LAUNCHER) || launcherEnabled)
            editor.putBoolean(PREF_LAUNCHER, launcherEnabled);
        editor.apply();

        boolean isLibrary = U.isLibrary(this);
        if(!isLibrary) {
            U.setComponentEnabled(this, HomeActivity.class, true);

            U.setComponentEnabled(this, ShortcutActivity.class,
                    U.enableFreeformModeShortcut(this));

            U.setComponentEnabled(this, StartTaskbarActivity.class, true);
        }

        if(!launcherEnabled && !isLibrary) {
            U.sendBroadcast(this, ACTION_KILL_HOME_ACTIVITY);
        }

        // Update caption state
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && U.isChromeOs(this)) {
            getWindow().setRestrictedCaptionAreaListener(rect -> hasCaption = true);

            U.newHandler().postDelayed(() -> pref.edit().putBoolean(PREF_HAS_CAPTION, hasCaption).apply(), 500);
        }

        proceedWithAppLaunch(savedInstanceState);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void proceedWithAppLaunch(Bundle savedInstanceState) {
        try {
            setContentView(R.layout.tb_main);
        } catch (IllegalStateException e) {
            setTheme(com.google.android.material.R.style.Theme_AppCompat_Light);
            setContentView(R.layout.tb_main);
        }

        if(!U.isLibrary(this)) {
            setSupportActionBar(findViewById(R.id.toolbar));

            ActionBar actionBar = getSupportActionBar();
            if(actionBar != null) {
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
            }
        }

        theSwitch = findViewById(R.id.the_switch);
        helpButton = findViewById(R.id.help_button);

        if(theSwitch != null) {
            final SharedPreferences pref = U.getSharedPreferences(this);
            theSwitch.setChecked(pref.getBoolean(PREF_TASKBAR_ACTIVE, false));

            theSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
                if(b) {
                    if(U.canDrawOverlays(this)) {
                        startTaskbarService();
                    } else {
                        U.showPermissionDialog(this);
                        compoundButton.setChecked(false);
                    }
                } else
                    stopTaskbarService();
            });
        }

        if(savedInstanceState == null) {
            U.initPrefs(this);
            CompatUtils.grantNotificationPermissionIfNeeded(this);

            navigateTo(new AboutFragment());
        } else try {
            Fragment oldFragment = getFragmentManager().findFragmentById(R.id.fragmentContainer);
            navigateTo(oldFragment.getClass().newInstance());
        } catch (IllegalAccessException | InstantiationException ignored) {}

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && !U.isLibrary(this)) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

            if(shortcutManager.getDynamicShortcuts().size() == 0) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClass(this, StartTaskbarActivity.class);
                intent.putExtra(EXTRA_IS_LAUNCHING_SHORTCUT, true);

                ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "start_taskbar")
                        .setShortLabel(getString(R.string.tb_start_taskbar))
                        .setIcon(Icon.createWithResource(this, R.drawable.tb_shortcut_icon_start))
                        .setIntent(intent)
                        .build();

                if(U.enableFreeformModeShortcut(this)) {
                    Intent intent2 = new Intent(Intent.ACTION_MAIN);
                    intent2.setClass(this, ShortcutActivity.class);
                    intent2.putExtra(EXTRA_IS_LAUNCHING_SHORTCUT, true);

                    ShortcutInfo shortcut2 = new ShortcutInfo.Builder(this, "freeform_mode")
                            .setShortLabel(getString(R.string.tb_pref_header_freeform))
                            .setIcon(Icon.createWithResource(this, R.drawable.tb_shortcut_icon_freeform))
                            .setIntent(intent2)
                            .build();

                    shortcutManager.setDynamicShortcuts(Arrays.asList(shortcut, shortcut2));
                } else
                    shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
            }
        }

        SharedPreferences pref = U.getSharedPreferences(this);

        if(pref.getInt("show_changelog", 0) < latestChangelogVersion
                && U.isConsumerBuild(this)) {
            Snackbar snackbar = Snackbar.make(
                    findViewById(R.id.main_activity_layout),
                    R.string.tb_see_whats_new,
                    Snackbar.LENGTH_INDEFINITE
            );

            snackbar.setAction(R.string.tb_action_view, v -> {
                pref.edit().putInt("show_changelog", latestChangelogVersion).apply();

                Uri uri = Uri.parse("https://github.com/farmerbb/Taskbar/blob/" + BuildConfig.VERSION_CODE + "/CHANGELOG.md");
                try {
                    DependencyUtils.openChromeCustomTab(this, uri);
                } catch (ActivityNotFoundException | IllegalArgumentException ignored) {}
            });

            snackbar.show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        updateSwitch();
    }

    @Override
    protected void onStart() {
        super.onStart();

        GlobalHelper.getInstance().setOnMainActivity(true);
    }

    @Override
    protected void onStop() {
        super.onStop();

        GlobalHelper.getInstance().setOnMainActivity(false);
    }

    @Override
    protected void onDestroy() {
        U.unregisterReceiver(this, switchReceiver);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && U.isChromeOs(this))
            getWindow().setRestrictedCaptionAreaListener(null);

        if(isFinishing() && U.isConsumerBuild(this)) {
            SharedPreferences pref = U.getSharedPreferences(this);
            pref.edit().putInt("show_changelog", latestChangelogVersion).apply();
        }

        super.onDestroy();
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void startTaskbarService() {
        SharedPreferences pref = U.getSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();

        editor.putBoolean(PREF_IS_HIDDEN, false);

        if(pref.getBoolean(PREF_FIRST_RUN, true)) {
            editor.putBoolean(PREF_FIRST_RUN, false);
        }

        editor.putBoolean(PREF_TASKBAR_ACTIVE, true);
        editor.putLong(PREF_TIME_OF_SERVICE_START, System.currentTimeMillis());
        editor.apply();

        if(U.hasFreeformSupport(this)
                && U.isFreeformModeEnabled(this)
                && !FreeformHackHelper.getInstance().isFreeformHackActive()
                && U.needsInvisibleActivityHacks()) {
            U.startFreeformHack(this, true);
        }

        startService(new Intent(this, TaskbarService.class));
        startService(new Intent(this, StartMenuService.class));
        startService(new Intent(this, NotificationService.class));
    }

    private void stopTaskbarService() {
        SharedPreferences pref = U.getSharedPreferences(this);
        pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();

        if(!LauncherHelper.getInstance().isOnHomeScreen(this)) {
            stopService(new Intent(this, TaskbarService.class));
            stopService(new Intent(this, StartMenuService.class));

            U.clearCaches(this);
            U.sendBroadcast(this, ACTION_START_MENU_DISAPPEARING);
        }

        stopService(new Intent(this, NotificationService.class));
    }

    private void updateSwitch() {
        if(theSwitch != null) {
            SharedPreferences pref = U.getSharedPreferences(this);
            theSwitch.setChecked(pref.getBoolean(PREF_TASKBAR_ACTIVE, false));
        }
    }

    @Override
    public void onBackPressed() {
        Fragment oldFragment = getFragmentManager().findFragmentById(R.id.fragmentContainer);

        if(oldFragment instanceof AboutFragment)
            super.onBackPressed();
        else {
            Fragment newFragment;
            newFragment = new AboutFragment();

            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, newFragment, newFragment.getClass().getSimpleName())
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                    .commit();
        }
    }

    public String getAboutFragmentTitle() {
        if(!U.isLibrary(this))
            return getString(R.string.tb_app_name);

        String title = getIntent().getStringExtra("title");
        return title != null ? title : getString(R.string.tb_settings);
    }

    public boolean getAboutFragmentBackArrow() {
        if(!U.isLibrary(this))
            return false;

        return getIntent().getBooleanExtra("back_arrow", false);
    }

    public void updateHelpButton(SettingsFragment fragment) {
        if(helpButton == null) return;

        helpButton.setVisibility(View.INVISIBLE);
        helpButton.setOnClickListener(null);
    }

    private void navigateTo(Fragment fragment) {
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment, fragment.getClass().getSimpleName())
                .commit();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        // MainActivity manually handles its own state restoration
    }
}
