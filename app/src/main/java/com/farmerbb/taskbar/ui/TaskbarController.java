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
import android.bluetooth.BluetoothAdapter;
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

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
    private Button button;
    private Space space;
    private FrameLayout dashboardButton;
    private LinearLayout sysTrayLayout;
    private FrameLayout sysTrayParentLayout;
    private TextView time;
    private ImageView notificationCountCircle;
    private TextView notificationCountText;

    private Handler handler;
    private Handler handler2;
    private Thread thread;
    private Thread thread2;

    private boolean taskbarShownTemporarily = false;
    private boolean taskbarHiddenTemporarily = false;
    private boolean isFirstStart = true;

    private boolean startThread2 = false;
    private boolean stopThread2 = false;

    private int refreshInterval = -1;

    private int currentTaskbarPosition = 0;
    private boolean showHideAutomagically = false;
    private boolean positionIsVertical = false;
    private boolean dashboardEnabled = false;
    private boolean sysTrayEnabled = false;

    private int cellStrength = -1;
    private int notificationCount = 0;
    private int numOfSysTrayIcons = 0;

    private boolean matchParent;
    private Runnable updateParamsRunnable;

    private final Map<Integer, Boolean> sysTrayIconStates = new HashMap<>();

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

    private final BroadcastReceiver notificationCountReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            notificationCount = intent.getIntExtra(EXTRA_COUNT, 0);
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    private final PhoneStateListener listener = new PhoneStateListener() {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            try {
                cellStrength = signalStrength.getLevel();
            } catch (SecurityException e) {
                cellStrength = -1;
            }
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
        TaskbarPosition.setCachedRotation(windowManager.getDefaultDisplay().getRotation());

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
        boolean altButtonConfig = pref.getBoolean(PREF_ALT_BUTTON_CONFIG, false);

        layout = (LinearLayout) LayoutInflater.from(U.wrapContext(context)).inflate(layoutId, null);
        taskbar = layout.findViewById(R.id.taskbar);
        scrollView = layout.findViewById(R.id.taskbar_scrollview);

        int backgroundTint = U.getBackgroundTint(context);
        int accentColor = U.getAccentColor(context);

        if(altButtonConfig) {
            space = layout.findViewById(R.id.space_alt);
            layout.findViewById(R.id.space).setVisibility(View.GONE);
        } else {
            space = layout.findViewById(R.id.space);
            layout.findViewById(R.id.space_alt).setVisibility(View.GONE);
        }

        space.setOnClickListener(v -> toggleTaskbar(true));

        startButton = layout.findViewById(R.id.start_button);
        drawStartButton(context, startButton, pref);

        refreshInterval = 250;

        U.sendBroadcast(context, ACTION_HIDE_START_MENU);
        U.sendBroadcast(context, ACTION_UPDATE_HOME_SCREEN_MARGINS);

        if(altButtonConfig) {
            button = layout.findViewById(R.id.hide_taskbar_button_alt);
            layout.findViewById(R.id.hide_taskbar_button).setVisibility(View.GONE);
        } else {
            button = layout.findViewById(R.id.hide_taskbar_button);
            layout.findViewById(R.id.hide_taskbar_button_alt).setVisibility(View.GONE);
        }

        try {
            button.setTypeface(Typeface.createFromFile("/system/fonts/Roboto-Regular.ttf"));
        } catch (RuntimeException ignored) {}

        updateButton(false);
        button.setOnClickListener(v -> toggleTaskbar(true));

        LinearLayout buttonLayout = layout.findViewById(altButtonConfig
                ? R.id.hide_taskbar_button_layout_alt
                : R.id.hide_taskbar_button_layout);
        if(buttonLayout != null) buttonLayout.setOnClickListener(v -> toggleTaskbar(true));

        LinearLayout buttonLayoutToHide = layout.findViewById(altButtonConfig
                ? R.id.hide_taskbar_button_layout
                : R.id.hide_taskbar_button_layout_alt);
        if(buttonLayoutToHide != null) buttonLayoutToHide.setVisibility(View.GONE);

        dashboardButton = layout.findViewById(R.id.dashboard_button);
        dashboardEnabled = drawDashboardButton(context, layout, dashboardButton, accentColor);

        sysTrayEnabled = U.isSystemTrayEnabled(context);

        if(sysTrayEnabled) {
            drawSysTray(context, layoutId, layout);
        }

        if(scrollView != null)
            scrollView.setVisibility(View.GONE);

        layout.setBackgroundColor(backgroundTint);
        layout.findViewById(R.id.divider).setBackgroundColor(
                pref.getBoolean(PREF_CENTERED_ICONS, false) ? 0 : accentColor
        );
        button.setTextColor(accentColor);

        applyMarginFix(host, layout, params);

        if(isFirstStart && FreeformHackHelper.getInstance().isInFreeformWorkspace())
            showTaskbar(false);
        else if(!pref.getBoolean(PREF_COLLAPSED, false) && pref.getBoolean(PREF_TASKBAR_ACTIVE, false))
            toggleTaskbar(false);

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

        if(sysTrayEnabled) {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            manager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

            U.registerReceiver(context, notificationCountReceiver, ACTION_NOTIFICATION_COUNT_CHANGED);
            U.sendBroadcast(context, ACTION_REQUEST_NOTIFICATION_COUNT);
        }

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
        Drawable allAppsIcon = ContextCompat.getDrawable(context, R.drawable.tb_all_apps_button_icon);
        int padding = 0;

        switch(pref.getString(PREF_START_BUTTON_IMAGE, U.getDefaultStartButtonImage(context))) {
            case PREF_START_BUTTON_IMAGE_DEFAULT:
                startButton.setImageDrawable(allAppsIcon);
                padding = context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding);
                break;
            case PREF_START_BUTTON_IMAGE_APP_LOGO:
                Drawable drawable;

                if(U.isBlissOs(context)) {
                    drawable = ContextCompat.getDrawable(context, R.drawable.tb_bliss);
                } else if(U.isProjectSakura(context)) {
                    drawable = ContextCompat.getDrawable(context, R.drawable.tb_sakura);                         
                } else {
                    LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                    LauncherActivityInfo info = launcherApps.getActivityList(context.getPackageName(), Process.myUserHandle()).get(0);
                    drawable = IconCache.getInstance(context).getIcon(context, context.getPackageManager(), info);
                }

                startButton.setImageDrawable(drawable);
                padding = context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding_alt);
                break;
            case PREF_START_BUTTON_IMAGE_CUSTOM:
                U.applyCustomImage(context, "custom_image", startButton, allAppsIcon);
                padding = context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding);
                break;
        }

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

    @VisibleForTesting
    boolean drawDashboardButton(Context context,
                                LinearLayout layout,
                                FrameLayout dashboardButton,
                                int accentColor) {
        dashboardButton.setVisibility(View.GONE);
        return false;
    }

    @VisibleForTesting
    void drawSysTray(Context context, int layoutId, LinearLayout layout) {
        sysTrayLayout = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.tb_system_tray, null);

        FrameLayout.LayoutParams sysTrayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                context.getResources().getDimensionPixelSize(R.dimen.tb_icon_size)
        );

        if(layoutId == R.layout.tb_taskbar_right) {
            time = sysTrayLayout.findViewById(R.id.time_left);
            sysTrayParams.gravity = Gravity.START;
            sysTrayLayout.findViewById(R.id.space_right).setVisibility(View.VISIBLE);
        } else {
            time = sysTrayLayout.findViewById(R.id.time_right);
            sysTrayParams.gravity = Gravity.END;
            sysTrayLayout.findViewById(R.id.space_left).setVisibility(View.VISIBLE);
        }

        time.setVisibility(View.VISIBLE);
        sysTrayLayout.setLayoutParams(sysTrayParams);

        notificationCountCircle = sysTrayLayout.findViewById(R.id.notification_count_circle);
        notificationCountText = sysTrayLayout.findViewById(R.id.notification_count_text);

        sysTrayParentLayout = layout.findViewById(R.id.add_systray_here);
        sysTrayParentLayout.setVisibility(View.VISIBLE);
        sysTrayParentLayout.addView(sysTrayLayout);

        sysTrayIconStates.clear();
        sysTrayIconStates.put(R.id.cellular, false);
        sysTrayIconStates.put(R.id.bluetooth, false);
        sysTrayIconStates.put(R.id.wifi, false);
        sysTrayIconStates.put(R.id.battery, false);
        sysTrayIconStates.put(R.id.notification_count, false);
    }

    private void toggleTaskbar(boolean userInitiated) {
        if(userInitiated && Build.BRAND.equalsIgnoreCase("essential")) {
            SharedPreferences pref = U.getSharedPreferences(context);
            if(!pref.getBoolean(PREF_GRIP_REJECTION_TOAST_SHOWN, false)) {
                U.showToastLong(context, R.string.tb_essential_phone_grip_rejection);
                pref.edit().putBoolean(PREF_GRIP_REJECTION_TOAST_SHOWN, true).apply();
            }
        }

        if(startButton.getVisibility() == View.GONE)
            showTaskbar(true);
        else
            hideTaskbar(true);
    }

    private void showTaskbar(boolean clearVariables) {
        if(clearVariables) {
            taskbarShownTemporarily = false;
            taskbarHiddenTemporarily = false;
        }

        if(startButton.getVisibility() == View.GONE) {
            startButton.setVisibility(View.VISIBLE);
            space.setVisibility(View.VISIBLE);

            if(dashboardEnabled)
                dashboardButton.setVisibility(View.VISIBLE);

            if(sysTrayEnabled)
                sysTrayParentLayout.setVisibility(View.VISIBLE);

            SharedPreferences pref = U.getSharedPreferences(context);
            pref.edit().putBoolean(PREF_COLLAPSED, true).apply();

            updateButton(false);

            U.newHandler().post(() -> U.sendBroadcast(context, ACTION_SHOW_START_MENU_SPACE));
        }
    }

    private void hideTaskbar(boolean clearVariables) {
        if(clearVariables) {
            taskbarShownTemporarily = false;
            taskbarHiddenTemporarily = false;
        }

        if(startButton.getVisibility() == View.VISIBLE) {
            startButton.setVisibility(View.GONE);
            space.setVisibility(View.GONE);

            if(dashboardEnabled)
                dashboardButton.setVisibility(View.GONE);

            if(sysTrayEnabled)
                sysTrayParentLayout.setVisibility(View.GONE);

            SharedPreferences pref = U.getSharedPreferences(context);
            pref.edit().putBoolean(PREF_COLLAPSED, false).apply();

            updateButton(true);

            if(clearVariables) {
                U.sendBroadcast(context, ACTION_HIDE_START_MENU);
                U.sendBroadcast(context, ACTION_HIDE_DASHBOARD);
            }

            if(matchParent) {
                matchParent = false;
                U.newHandler().post(updateParamsRunnable);
            }

            U.newHandler().post(() -> U.sendBroadcast(context, ACTION_HIDE_START_MENU_SPACE));
        }
    }

    private void tempShowTaskbar() {
        if(!taskbarHiddenTemporarily) {
            SharedPreferences pref = U.getSharedPreferences(context);
            if(!pref.getBoolean(PREF_COLLAPSED, false)) taskbarShownTemporarily = true;
        }

        showTaskbar(false);

        if(taskbarHiddenTemporarily)
            taskbarHiddenTemporarily = false;
    }

    private void tempHideTaskbar(boolean monitorPositionChanges) {
        if(!taskbarShownTemporarily) {
            SharedPreferences pref = U.getSharedPreferences(context);
            if(pref.getBoolean(PREF_COLLAPSED, false)) taskbarHiddenTemporarily = true;
        }

        hideTaskbar(false);

        if(taskbarShownTemporarily)
            taskbarShownTemporarily = false;

        if(monitorPositionChanges && showHideAutomagically && !positionIsVertical) {
            if(thread2 != null) thread2.interrupt();

            handler2 = U.newHandler();
            thread2 = new Thread(() -> {
                stopThread2 = false;

                while(!stopThread2) {
                    SystemClock.sleep(refreshInterval);

                    handler2.post(() -> stopThread2 = checkPositionChange());
                }

                startThread2 = false;
            });

            thread2.start();
        }
    }

    private boolean checkPositionChange() {
        if(layout != null) {
            int[] location = new int[2];
            layout.getLocationOnScreen(location);

            if(location[1] == 0) {
                return true;
            } else {
                if(location[1] > currentTaskbarPosition) {
                    currentTaskbarPosition = location[1];
                    if(taskbarHiddenTemporarily) {
                        tempShowTaskbar();
                        return true;
                    }
                } else if(location[1] == currentTaskbarPosition && taskbarHiddenTemporarily) {
                    tempShowTaskbar();
                    return true;
                } else if(location[1] < currentTaskbarPosition
                        && currentTaskbarPosition - location[1] == getNavBarSize()) {
                    currentTaskbarPosition = location[1];
                }
            }
        }

        return false;
    }

    private int getNavBarSize() {
        Point size = new Point();
        Point realSize = new Point();

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getSize(size);
        display.getRealSize(realSize);

        return realSize.y - size.y;
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

        if(sysTrayEnabled) {
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            manager.listen(listener, PhoneStateListener.LISTEN_NONE);

            U.unregisterReceiver(context, notificationCountReceiver);
        }

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

    private void updateButton(boolean isCollapsed) {
        SharedPreferences pref = U.getSharedPreferences(context);
        boolean hide = pref.getBoolean(PREF_INVISIBLE_BUTTON, false);

        if(button != null) button.setText(context.getString(isCollapsed ? R.string.tb_right_arrow : R.string.tb_left_arrow));
        if(layout != null) layout.setAlpha(isCollapsed && hide ? 0 : 1);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRecreateHost(UIHost host) {
        if(layout != null) {
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException ignored) {}

            currentTaskbarPosition = 0;

            if(U.canDrawOverlays(context))
                drawTaskbar(host);
            else {
                SharedPreferences pref = U.getSharedPreferences(context);
                pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();

                host.terminate();
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateSystemTray() {
        if(!sysTrayEnabled) return;

        handler.post(() -> {
            Map<Integer, Drawable> drawables = new HashMap<>();
            drawables.put(R.id.battery, getBatteryDrawable());
            drawables.put(R.id.wifi, getWifiDrawable());
            drawables.put(R.id.bluetooth, getBluetoothDrawable());
            drawables.put(R.id.cellular, getCellularDrawable());

            for(Integer key : drawables.keySet()) {
                ImageView view = sysTrayLayout.findViewById(key);
                Drawable drawable = drawables.get(key);

                if(drawable != null) view.setImageDrawable(drawable);
                sysTrayIconStates.put(key, drawable != null);
            }

            if(notificationCount > 0) {
                int color = ColorUtils.setAlphaComponent(U.getBackgroundTint(context), 255);
                notificationCountText.setTextColor(color);

                Drawable drawable = ContextCompat.getDrawable(context, R.drawable.tb_circle);
                drawable.setTint(U.getAccentColor(context));

                notificationCountCircle.setImageDrawable(drawable);
                notificationCountText.setText(Integer.toString(notificationCount));
                sysTrayIconStates.put(R.id.notification_count, true);
            } else
                sysTrayIconStates.put(R.id.notification_count, false);

            time.setText(context.getString(R.string.tb_systray_clock,
                    DateFormat.getTimeFormat(context).format(new Date()),
                    DateFormat.getDateFormat(context).format(new Date())));
            time.setTextColor(U.getAccentColor(context));
        });
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Drawable getBatteryDrawable() {
        BatteryManager bm = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        if(batLevel == Integer.MIN_VALUE)
            return null;

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        String batDrawable;
        if(batLevel < 10 && !isCharging)
            batDrawable = "alert";
        else if(batLevel < 25)
            batDrawable = "20";
        else if(batLevel < 40)
            batDrawable = "30";
        else if(batLevel < 55)
            batDrawable = "50";
        else if(batLevel < 70)
            batDrawable = "60";
        else if(batLevel < 85)
            batDrawable = "80";
        else if(batLevel < 95)
            batDrawable = "90";
        else
            batDrawable = "full";

        String charging;
        if(isCharging)
            charging = "charging_";
        else
            charging = "";

        String batRes = "tb_battery_" + charging + batDrawable;
        int id = getResourceIdFor(batRes);

        return getDrawableForSysTray(id);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Drawable getWifiDrawable() {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo ethernet = manager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        if(ethernet != null && ethernet.isConnected())
            return getDrawableForSysTray(R.drawable.tb_settings_ethernet);

        NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(wifi == null || !wifi.isConnected())
            return null;

        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int numberOfLevels = 5;

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);

        String wifiRes = "tb_signal_wifi_" + level + "_bar";
        int id = getResourceIdFor(wifiRes);

        return getDrawableForSysTray(id);
    }

    private Drawable getBluetoothDrawable() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter != null && adapter.isEnabled())
            return getDrawableForSysTray(R.drawable.tb_bluetooth);

        return null;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Drawable getCellularDrawable() {
        if(Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0)
            return getDrawableForSysTray(R.drawable.tb_airplanemode_active);

        if(cellStrength == -1)
            return null;

        String cellRes = "tb_signal_cellular_" + cellStrength + "_bar";
        int id = getResourceIdFor(cellRes);

        return getDrawableForSysTray(id);
    }

    private Drawable getDrawableForSysTray(int id) {
        Drawable drawable = null;
        try {
            drawable = ContextCompat.getDrawable(context, id);
        } catch (Resources.NotFoundException ignored) {}

        if(drawable == null) return null;

        drawable.setTint(U.getAccentColor(context));
        return drawable;
    }

    private int getResourceIdFor(String name) {
        String packageName = context.getResources().getResourcePackageName(R.drawable.tb_dummy);
        return context.getResources().getIdentifier(name, "drawable", packageName);
    }
}
