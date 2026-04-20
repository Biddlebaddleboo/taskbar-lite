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

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.util.DisplayMetrics;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.adapter.StartMenuAdapter;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.util.U;
import com.farmerbb.taskbar.widget.StartMenuLayout;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.farmerbb.taskbar.util.Constants.*;

public class StartMenuController extends UIController {

    private StartMenuLayout layout;
    private GridView startMenu;
    private TextView textView;
    private StartMenuAdapter adapter;

    private Handler handler;
    private Thread thread;

    private List<String> currentStartMenuIds = new ArrayList<>();

    private final View.OnClickListener ocl = view -> toggleStartMenu();

    private final BroadcastReceiver hideReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideStartMenu(true);
        }
    };

    private final BroadcastReceiver hideReceiverNoReset = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            hideStartMenu(false);
        }
    };

    private final BroadcastReceiver showReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showStartMenu();
        }
    };

    private final Comparator<LauncherActivityInfo> comparator = (ai1, ai2) -> {
        String label1;
        String label2;

        try {
            label1 = ai1.getLabel().toString();
            label2 = ai2.getLabel().toString();
        } catch (OutOfMemoryError e) {
            System.gc();

            label1 = ai1.getApplicationInfo().packageName;
            label2 = ai2.getApplicationInfo().packageName;
        }

        return Collator.getInstance().compare(label1, label2);
    };

    public StartMenuController(Context context) {
        super(context);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreateHost(UIHost host) {
        init(context, host, () -> drawStartMenu(host));
    }

    public void drawStartMenu(UIHost host) {
        final SharedPreferences pref = U.getSharedPreferences(context);

        final ViewParams params = new ViewParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                -1,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                getBottomMargin(context)
        );

        // The taskbar is fixed to the bottom-left corner, so the start menu is too.
        int layoutId = R.layout.tb_start_menu_left;
        params.gravity = Gravity.BOTTOM | Gravity.LEFT;

        // Initialize views
        layout = (StartMenuLayout) LayoutInflater.from(U.wrapContext(context)).inflate(layoutId, null);
        layout.setVisibility(View.INVISIBLE);
        layout.setAlpha(0);
        layout.viewHandlesBackButton();

        startMenu = layout.findViewById(R.id.start_menu);

        if(pref.getBoolean(PREF_TRANSPARENT_START_MENU, false))
            startMenu.setBackgroundColor(0);

        int columns = context.getResources().getInteger(R.integer.tb_start_menu_columns);
        ViewGroup.LayoutParams startMenuParams = startMenu.getLayoutParams();
        startMenuParams.width = (int) (startMenuParams.width * (columns / 3f));
        startMenu.setLayoutParams(startMenuParams);

        int backgroundTint = U.getBackgroundTint(context);

        FrameLayout startMenuFrame = layout.findViewById(R.id.start_menu_frame);
        startMenuFrame.setBackgroundColor(backgroundTint);
        startMenu.setOnItemClickListener((viewParent, view, position, id) -> {
            hideStartMenu(true);

            AppEntry entry = (AppEntry) viewParent.getAdapter().getItem(position);
            U.launchApp(context, entry, null, false, false, view);
        });

        applyMarginFix(host, layout, params);

        textView = layout.findViewById(R.id.no_apps_found);

        U.registerReceiver(context, hideReceiver, ACTION_HIDE_START_MENU);
        U.registerReceiver(context, hideReceiverNoReset, ACTION_HIDE_START_MENU_NO_RESET);
        U.registerReceiver(context, showReceiver, ACTION_SHOW_START_MENU);

        handler = U.newHandler();
        refreshApps(true);

        host.addView(layout, params);
        showStartMenu();
    }

    private void refreshApps(boolean firstDraw) {
        if(thread != null) thread.interrupt();

        handler = U.newHandler();
        thread = new Thread(() -> {
            PackageManager pm = context.getPackageManager();
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);

            final List<UserHandle> userHandles = userManager.getUserProfiles();
            final List<LauncherActivityInfo> unfilteredList = new ArrayList<>();

            for(UserHandle handle : userHandles) {
                unfilteredList.addAll(launcherApps.getActivityList(null, handle));
            }

            final List<LauncherActivityInfo> list = new ArrayList<>(unfilteredList);
            Collections.sort(list, comparator);

            // Now that we've generated the list of apps,
            // we need to determine if we need to redraw the start menu or not
            boolean shouldRedrawStartMenu = false;
            List<String> finalApplicationIds = new ArrayList<>();

            for(LauncherActivityInfo appInfo : list) {
                finalApplicationIds.add(appInfo.getApplicationInfo().packageName);
            }

            if(!firstDraw) {
                if(finalApplicationIds.size() != currentStartMenuIds.size())
                    shouldRedrawStartMenu = true;
                else {
                    for(int i = 0; i < finalApplicationIds.size(); i++) {
                        if(!finalApplicationIds.get(i).equals(currentStartMenuIds.get(i))) {
                            shouldRedrawStartMenu = true;
                            break;
                        }
                    }
                }
            } else shouldRedrawStartMenu = true;

            if(shouldRedrawStartMenu) {
                currentStartMenuIds = finalApplicationIds;

                final List<AppEntry> entries =
                        generateAppEntries(context, userManager, pm, list);

                handler.post(() -> {
                    if(firstDraw) {
                        startMenu.setNumColumns(context.getResources().getInteger(R.integer.tb_start_menu_columns));
                        adapter = new StartMenuAdapter(context, R.layout.tb_row_alt, entries);
                        startMenu.setAdapter(adapter);
                    }

                    int position = startMenu.getFirstVisiblePosition();

                    if(!firstDraw && adapter != null)
                        adapter.updateList(entries);

                    startMenu.setSelection(position);

                    if(adapter != null && adapter.getCount() > 0)
                        textView.setText(null);
                    else
                        textView.setText(context.getString(R.string.tb_nothing_to_see_here));
                });
            }
        });

        thread.start();
    }

    @VisibleForTesting
    List<AppEntry> generateAppEntries(Context context,
                                      UserManager userManager,
                                      PackageManager pm,
                                      List<LauncherActivityInfo> queryList) {
        final List<AppEntry> entries = new ArrayList<>();
        Drawable defaultIcon = pm.getDefaultActivityIcon();
        for(LauncherActivityInfo appInfo : queryList) {
            // Attempt to work around frequently reported OutOfMemoryErrors
            String label;
            Drawable icon;

            try {
                label = appInfo.getLabel().toString();
                icon = appInfo.getIcon(DisplayMetrics.DENSITY_MEDIUM);
                if(icon == null)
                    icon = pm.getDefaultActivityIcon();
            } catch (OutOfMemoryError e) {
                System.gc();

                label = appInfo.getApplicationInfo().packageName;
                icon = defaultIcon;
            }

            String packageName = appInfo.getApplicationInfo().packageName;
            ComponentName componentName = new ComponentName(packageName, appInfo.getName());
            AppEntry newEntry =
                    new AppEntry(packageName, componentName.flattenToString(), label, icon);

            newEntry.setUserId(userManager.getSerialNumberForUser(appInfo.getUser()));
            entries.add(newEntry);
        }
        return entries;
    }

    public void toggleStartMenu() {
        if(layout.getVisibility() != View.VISIBLE)
            showStartMenu();
        else
            hideStartMenu(true);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void showStartMenu() {
        if(layout.getVisibility() != View.VISIBLE) {
            layout.setOnClickListener(ocl);
            layout.setVisibility(View.VISIBLE);
            U.getSharedPreferences(context).edit().putBoolean(PREF_START_MENU_OPEN, true).commit();

            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1)
                layout.setAlpha(1);

            U.sendBroadcast(context, ACTION_START_MENU_APPEARING);
            U.sendGlobalBroadcast(context, ACTION_START_MENU_APPEARING);

            refreshApps(false);

            U.newHandler().postDelayed(() -> {
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1)
                    layout.setAlpha(1);

                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(layout.getWindowToken(), 0);
            }, 100);
        }
    }

    private void hideStartMenu(boolean shouldReset) {
        if(layout.getVisibility() != View.VISIBLE) {
            U.getSharedPreferences(context).edit().putBoolean(PREF_START_MENU_OPEN, false).commit();
            U.sendBroadcast(context, ACTION_START_MENU_DISAPPEARING);
            U.sendGlobalBroadcast(context, ACTION_START_MENU_DISAPPEARING);
            return;
        }

        layout.setOnClickListener(null);
        layout.setAlpha(0);
        U.getSharedPreferences(context).edit().putBoolean(PREF_START_MENU_OPEN, false).commit();

        U.sendBroadcast(context, ACTION_START_MENU_DISAPPEARING);
        U.sendGlobalBroadcast(context, ACTION_START_MENU_DISAPPEARING);

        layout.postDelayed(() -> {
            layout.setVisibility(View.GONE);

            if(shouldReset) {
                startMenu.smoothScrollBy(0, 0);
                startMenu.setSelection(0);
            }

            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(layout.getWindowToken(), 0);
        }, 100);
    }

    @Override
    public void onDestroyHost(UIHost host) {
        if(layout != null)
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException ignored) {}

        U.unregisterReceiver(context, hideReceiver);
        U.unregisterReceiver(context, hideReceiverNoReset);
        U.unregisterReceiver(context, showReceiver);
        U.sendBroadcast(context, ACTION_START_MENU_DISAPPEARING);
        U.sendGlobalBroadcast(context, ACTION_START_MENU_DISAPPEARING);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRecreateHost(UIHost host) {
        if(layout != null) {
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException ignored) {}

            if(U.canDrawOverlays(context))
                drawStartMenu(host);
            else {
                SharedPreferences pref = U.getSharedPreferences(context);
                pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();

                host.terminate();
            }
        }
    }

}
