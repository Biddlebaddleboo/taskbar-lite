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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.core.content.ContextCompat;
import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.helper.DisplayHelper;
import com.farmerbb.taskbar.helper.GlobalHelper;
import com.farmerbb.taskbar.util.Callbacks;
import com.farmerbb.taskbar.service.NotificationService;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.ui.UIHost;
import com.farmerbb.taskbar.ui.ViewParams;
import com.farmerbb.taskbar.ui.StartMenuController;
import com.farmerbb.taskbar.ui.TaskbarController;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.util.U;

import java.io.File;
import java.util.List;

import static com.farmerbb.taskbar.util.Constants.*;

public class HomeActivityDelegate extends Activity implements UIHost {
    private FrameLayout layout;
    private boolean forceTaskbarStart = false;
    private AlertDialog dialog;

    private boolean shouldDelayFreeformHack;
    private int hits;

    private boolean waitingForPermission;

    private final BroadcastReceiver killReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            killHomeActivity();
        }
    };

    private final BroadcastReceiver forceTaskbarStartReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            forceTaskbarStart = true;
        }
    };

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences pref = U.getSharedPreferences(this);

        shouldDelayFreeformHack = true;
        hits = 0;

        WindowManager.LayoutParams params = getWindow().getAttributes();
        if(U.applyDisplayCutoutModeTo(params))
            getWindow().setAttributes(params);

        layout = new FrameLayout(this) {
            @Override
            protected void onAttachedToWindow() {
                super.onAttachedToWindow();

                boolean shouldStartFreeformHack = shouldDelayFreeformHack && hits > 0;
                shouldDelayFreeformHack = false;

                if(shouldStartFreeformHack)
                    startFreeformHack();
            }
        };

        layout.setFitsSystemWindows(true);

        setContentView(layout);

        updateWindowFlags();

        U.registerReceiver(this, killReceiver, ACTION_KILL_HOME_ACTIVITY);
        U.registerReceiver(this, forceTaskbarStartReceiver, ACTION_FORCE_TASKBAR_RESTART);

        U.initPrefs(this);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    protected void onResume() {
        super.onResume();

        if(U.canBootToFreeform(this))
            startFreeformHack();
        else
            performOnResumeLogic();
    }

    @Override
    protected void onStart() {
        super.onStart();

        U.sendBroadcast(this, ACTION_HIDE_START_MENU);
        init();
    }

    private void init() {
        if(U.canDrawOverlays(this)) {
            if(!U.canBootToFreeform(this)) {
                setOnHomeScreen(true);

                if(forceTaskbarStart) {
                    forceTaskbarStart = false;
                    U.newHandler().postDelayed(() -> {
                        setOnHomeScreen(true);
                        startTaskbar();
                    }, 250);
                } else
                    startTaskbar();
            } else
                startFreeformHack();
        } else if(!waitingForPermission)
            dialog = U.showPermissionDialog(U.wrapContext(this), new Callbacks(
                    () -> dialog = U.showErrorDialog(U.wrapContext(this), "SYSTEM_ALERT_WINDOW"),
                    () -> waitingForPermission = true));
    }

    private void startTaskbar() {
        // Ensure that the freeform hack is started whenever Taskbar starts
        if(U.hasFreeformSupport(this)
                && U.isFreeformModeEnabled(this)
                && !FreeformHackHelper.getInstance().isFreeformHackActive()) {
            U.startFreeformHack(this, true);
        }

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean(PREF_FIRST_RUN, true)) {
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean(PREF_FIRST_RUN, false);
            editor.apply();
        }

        // We always start the Taskbar and Start Menu services, even if the app isn't normally running
        try {
            startService(new Intent(this, TaskbarService.class));
            
        } catch (IllegalStateException ignored) {}

        if(pref.getBoolean(PREF_TASKBAR_ACTIVE, false) && !U.isServiceRunning(this, NotificationService.class))
            pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();

        // Show the Taskbar temporarily, as nothing else will be visible on screen
        U.newHandler().postDelayed(() ->
                U.sendBroadcast(this, ACTION_TEMP_SHOW_TASKBAR), 100);
    }

    private void startFreeformHack() {
        if(shouldDelayFreeformHack)
            hits++;
        else
            U.startFreeformHack(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences pref = U.getSharedPreferences(this);
        if(!U.canBootToFreeform(this)) {
            if(isChangingConfigurations())
                setOnHomeScreen(false);
            else
                U.newHandler().post(() -> setOnHomeScreen(false));
        }

        if(dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        U.unregisterReceiver(this, killReceiver);
        U.unregisterReceiver(this, forceTaskbarStartReceiver);
    }

    @Override
    public void onBackPressed() {
        U.sendBroadcast(this, ACTION_HIDE_START_MENU);
    }

    private void killHomeActivity() {
        U.newHandler().post(() -> {
            setOnHomeScreen(false);
            finish();
        });
    }

    private void updateWindowFlags() {
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        getWindow().clearFlags(flags);
    }

    @Override
    public void addView(View view, ViewParams params) {
        // no-op
    }

    @Override
    public void removeView(View view) {
        // no-op
    }

    @Override
    public void terminate() {
        // no-op
    }

    @Override
    public void updateViewLayout(View view, ViewParams params) {
        // no-op
    }

    private void setOnHomeScreen(boolean value) {
        LauncherHelper helper = LauncherHelper.getInstance();
        helper.setOnPrimaryHomeScreen(value);
    }

    @Override
    public void onTopResumedActivityChanged(boolean isTopResumedActivity) {
        if(isTopResumedActivity)
            performOnResumeLogic();
    }

    private void performOnResumeLogic() {
        if(waitingForPermission) {
            waitingForPermission = false;
            init();
        }

        overridePendingTransition(0, R.anim.close_anim);
        U.sendBroadcast(this, ACTION_TEMP_SHOW_TASKBAR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != RESULT_OK)
            return;

    }
}
