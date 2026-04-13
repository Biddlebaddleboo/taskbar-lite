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

package com.farmerbb.taskbar.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import android.telephony.PhoneStateListener;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.widget.LinearLayout;
import android.widget.Space;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.activity.HomeActivityDelegate;
import com.farmerbb.taskbar.activity.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.HomeActivity;
import com.farmerbb.taskbar.activity.InvisibleActivityFreeform;
import com.farmerbb.taskbar.util.TaskbarPosition;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.DisplayInfo;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.helper.MenuHelper;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class TaskbarController extends UIController {

    private LinearLayout layout;
    private ImageView startButton;
    private LinearLayout taskbar;
    private FrameLayout scrollView;
    private Space space;

    private boolean taskbarShownTemporarily = false;
    private boolean taskbarHiddenTemporarily = false;
    private boolean isFirstStart = true;

    private boolean positionIsVertical = false;

    private boolean matchParent;
    private Runnable updateParamsRunnable;

    private final View.OnClickListener ocl = view ->
            U.sendBroadcast(context, ACTION_TOGGLE_START_MENU);

    private final BroadcastReceiver showReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showTaskbar(true);
        }
    };

    private final BroadcastReceiver hideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideTaskbar(true);
        }
    };

    private final BroadcastReceiver tempShowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tempShowTaskbar();
        }
    };

    private final BroadcastReceiver tempHideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tempHideTaskbar(false);
        }
    };

    private final BroadcastReceiver startMenuAppearReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(startButton.getVisibility() == View.GONE
                    && (!LauncherHelper.getInstance().isOnHomeScreen(context) || FreeformHackHelper.getInstance().isInFreeformWorkspace()))
                layout.setVisibility(View.GONE);
        }
    };

    private final BroadcastReceiver startMenuDisappearReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(startButton.getVisibility() == View.GONE)
                layout.setVisibility(View.VISIBLE);
        }
    };

    public TaskbarController(Context context) {
        super(context);
    }

    @Override
    public void onCreateHost(UIHost host) {
        init(context, host, () -> drawTaskbar(host));
    }

    private void drawTaskbar(UIHost host) {
        IconCache.getInstance(context).clearCache();

        // Initialize layout params
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        final ViewParams params = new ViewParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                -1,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                getBottomMargin(context)
        );

        // Determine where to show the taskbar on screen
        String taskbarPosition = TaskbarPosition.getTaskbarPosition(context);
        params.gravity = getTaskbarGravity(taskbarPosition);
        int layoutId = getTaskbarLayoutId(taskbarPosition);
        positionIsVertical = TaskbarPosition.isVertical(taskbarPosition);

        // Initialize views
        SharedPreferences pref = U.getSharedPreferences(context);

        layout = (LinearLayout) LayoutInflater.from(U.wrapContext(context)).inflate(layoutId, null);
        taskbar = layout.findViewById(R.id.taskbar);
        scrollView = layout.findViewById(R.id.taskbar_scrollview);

        int backgroundTint = U.getBackgroundTint(context);
        int accentColor = U.getAccentColor(context);

        space = layout.findViewById(R.id.space);
        layout.findViewById(R.id.space_alt).setVisibility(View.GONE);

        space.setOnClickListener(v -> {});

        startButton = layout.findViewById(R.id.start_button);
        drawStartButton(context, startButton, pref);

        U.sendBroadcast(context, ACTION_HIDE_START_MENU);
        U.sendBroadcast(context, ACTION_UPDATE_HOME_SCREEN_MARGINS);

        View hideTaskbarButton = layout.findViewById(R.id.hide_taskbar_button);
        if(hideTaskbarButton != null)
            hideTaskbarButton.setVisibility(View.GONE);

        View hideTaskbarButtonAlt = layout.findViewById(R.id.hide_taskbar_button_alt);
        if(hideTaskbarButtonAlt != null)
            hideTaskbarButtonAlt.setVisibility(View.GONE);

        View hideTaskbarButtonLayout = layout.findViewById(R.id.hide_taskbar_button_layout);
        if(hideTaskbarButtonLayout != null)
            hideTaskbarButtonLayout.setVisibility(View.GONE);

        View hideTaskbarButtonLayoutAlt = layout.findViewById(R.id.hide_taskbar_button_layout_alt);
        if(hideTaskbarButtonLayoutAlt != null)
            hideTaskbarButtonLayoutAlt.setVisibility(View.GONE);

        if(scrollView != null)
            scrollView.setVisibility(View.VISIBLE);

        layout.setBackgroundColor(backgroundTint);
        layout.findViewById(R.id.divider).setBackgroundColor(
                pref.getBoolean(PREF_CENTERED_ICONS, false) ? 0 : accentColor
        );

        applyMarginFix(host, layout, params);

        if(isFirstStart && FreeformHackHelper.getInstance().isInFreeformWorkspace())
            showTaskbar(false);

        if(pref.getBoolean(PREF_AUTO_HIDE_NAVBAR, false))
            U.showHideNavigationBar(context, false);

        if(FreeformHackHelper.getInstance().isTouchAbsorberActive()) {
            U.sendBroadcast(context, ACTION_FINISH_FREEFORM_ACTIVITY);

            U.newHandler().postDelayed(() -> U.startTouchAbsorberActivity(context), 500);
        }

        U.registerReceiver(context, showReceiver, ACTION_SHOW_TASKBAR);
        U.registerReceiver(context, hideReceiver, ACTION_HIDE_TASKBAR);
        U.registerReceiver(context, tempShowReceiver, ACTION_TEMP_SHOW_TASKBAR);
        U.registerReceiver(context, tempHideReceiver, ACTION_TEMP_HIDE_TASKBAR);
        U.registerReceiver(context, startMenuAppearReceiver, ACTION_START_MENU_APPEARING);
        U.registerReceiver(context, startMenuDisappearReceiver, ACTION_START_MENU_DISAPPEARING);

        matchParent = false;
        updateParamsRunnable = () -> {
            ViewParams newParams;
            if(TaskbarPosition.isVertical(context)) {
                newParams = matchParent
                        ? params.updateHeight(WindowManager.LayoutParams.MATCH_PARENT)
                        : params.updateHeight(WindowManager.LayoutParams.WRAP_CONTENT);
            } else {
                newParams = matchParent
                        ? params.updateWidth(WindowManager.LayoutParams.MATCH_PARENT)
                        : params.updateWidth(WindowManager.LayoutParams.WRAP_CONTENT);
            }

            try {
                host.updateViewLayout(layout, newParams);
            } catch (IllegalArgumentException ignored) {}
        };

        host.addView(layout, params);

        isFirstStart = false;
    }

    @SuppressLint("RtlHardcoded")
    @VisibleForTesting
    int getTaskbarGravity(String taskbarPosition) {
        int gravity = Gravity.BOTTOM | Gravity.LEFT;
        switch(taskbarPosition) {
            case POSITION_BOTTOM_LEFT:
            case POSITION_BOTTOM_VERTICAL_LEFT:
                gravity = Gravity.BOTTOM | Gravity.LEFT;
                break;
            case POSITION_BOTTOM_RIGHT:
            case POSITION_BOTTOM_VERTICAL_RIGHT:
                gravity = Gravity.BOTTOM | Gravity.RIGHT;
                break;
            case POSITION_TOP_LEFT:
            case POSITION_TOP_VERTICAL_LEFT:
                gravity = Gravity.TOP | Gravity.LEFT;
                break;
            case POSITION_TOP_RIGHT:
            case POSITION_TOP_VERTICAL_RIGHT:
                gravity = Gravity.TOP | Gravity.RIGHT;
                break;
        }
        return gravity;
    }

    @VisibleForTesting
    int getTaskbarLayoutId(String taskbarPosition) {
        int layoutId = R.layout.tb_taskbar_left;
        switch(taskbarPosition) {
            case POSITION_BOTTOM_LEFT:
            case POSITION_TOP_LEFT:
                layoutId = R.layout.tb_taskbar_left;
                break;
            case POSITION_BOTTOM_VERTICAL_LEFT:
            case POSITION_BOTTOM_VERTICAL_RIGHT:
                layoutId = R.layout.tb_taskbar_vertical;
                break;
            case POSITION_BOTTOM_RIGHT:
            case POSITION_TOP_RIGHT:
                layoutId = R.layout.tb_taskbar_right;
                break;
            case POSITION_TOP_VERTICAL_LEFT:
            case POSITION_TOP_VERTICAL_RIGHT:
                layoutId = R.layout.tb_taskbar_top_vertical;
                break;
        }
        return layoutId;
    }

    @VisibleForTesting
    void drawStartButton(Context context, ImageView startButton, SharedPreferences pref) {
        startButton.setImageResource(U.getStartButtonIcon());
        int padding = context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding);
        startButton.setPadding(padding, padding, padding, padding);
        startButton.setOnClickListener(ocl);
        startButton.setOnLongClickListener(view -> {
            openContextMenu();
            return true;
        });

        startButton.setOnGenericMotionListener((view, motionEvent) -> {
            if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                    && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY)
                openContextMenu();

            return false;
        });
    }

    private void showTaskbar(boolean clearVariables) {
        if(clearVariables) {
            taskbarShownTemporarily = false;
            taskbarHiddenTemporarily = false;
        }

        if(startButton.getVisibility() == View.GONE) {
            layout.setVisibility(View.VISIBLE);
            startButton.setVisibility(View.VISIBLE);
            space.setVisibility(View.VISIBLE);

            if(scrollView != null)
                scrollView.setVisibility(View.VISIBLE);

            new Handler().post(() ->
                    U.sendBroadcast(context, ACTION_SHOW_START_MENU_SPACE));
        }
    }

    private void hideTaskbar(boolean clearVariables) {
        if(clearVariables) {
            taskbarShownTemporarily = false;
            taskbarHiddenTemporarily = false;
        }

        if(startButton.getVisibility() == View.GONE)
            return;

        startButton.setVisibility(View.GONE);
        space.setVisibility(View.GONE);

        if(scrollView != null)
            scrollView.setVisibility(View.GONE);

        if(clearVariables)
            U.sendBroadcast(context, ACTION_HIDE_START_MENU);

        new Handler().post(() ->
                U.sendBroadcast(context, ACTION_HIDE_START_MENU_SPACE));
    }

    private void tempShowTaskbar() {
        if(!taskbarHiddenTemporarily)
            taskbarShownTemporarily = true;

        showTaskbar(false);

        if(taskbarHiddenTemporarily)
            taskbarHiddenTemporarily = false;
    }

    private void tempHideTaskbar(boolean monitorPositionChanges) {
        if(!taskbarShownTemporarily)
            taskbarHiddenTemporarily = true;

        hideTaskbar(false);

        if(taskbarShownTemporarily)
            taskbarShownTemporarily = false;
    }

    @Override
    public void onDestroyHost(UIHost host) {
        if(layout != null)
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException ignored) {}

        SharedPreferences pref = U.getSharedPreferences(context);
        if(pref.getBoolean(PREF_SKIP_AUTO_HIDE_NAVBAR, false)) {
            pref.edit().remove(PREF_SKIP_AUTO_HIDE_NAVBAR).apply();
        } else if(pref.getBoolean(PREF_AUTO_HIDE_NAVBAR, false))
            U.showHideNavigationBar(context, true);

        U.unregisterReceiver(context, showReceiver);
        U.unregisterReceiver(context, hideReceiver);
        U.unregisterReceiver(context, tempShowReceiver);
        U.unregisterReceiver(context, tempHideReceiver);
        U.unregisterReceiver(context, startMenuAppearReceiver);
        U.unregisterReceiver(context, startMenuDisappearReceiver);

        isFirstStart = true;
    }

    private void openContextMenu() {
        SharedPreferences pref = U.getSharedPreferences(context);

        Bundle args = new Bundle();
        args.putBoolean("dont_show_quit",
                LauncherHelper.getInstance().isOnHomeScreen(context)
                        && !pref.getBoolean(PREF_TASKBAR_ACTIVE, false));
        args.putBoolean("is_start_button", true);

        U.startContextMenuActivity(context, args);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRecreateHost(UIHost host) {
        if(layout != null) {
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException ignored) {}

            if(U.canDrawOverlays(context))
                drawTaskbar(host);
            else {
                SharedPreferences pref = U.getSharedPreferences(context);
                pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();

                host.terminate();
            }
        }
    }

}
