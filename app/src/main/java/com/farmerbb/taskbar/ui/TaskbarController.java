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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.VisibleForTesting;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import android.widget.LinearLayout;
import android.widget.Space;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class TaskbarController extends UIController {

    private LinearLayout layout;
    private ImageView startButton;
    private FrameLayout scrollView;
    private Space space;
    private boolean isFirstStart = true;

    private final View.OnClickListener ocl = view -> {
        SharedPreferences pref = U.getSharedPreferences(context);
        boolean startMenuProcessRunning = U.isProcessRunning(context, context.getPackageName() + ":startmenu");
        boolean startMenuOpen = pref.getBoolean(PREF_START_MENU_OPEN, false);

        if(startMenuProcessRunning) {
            if(startMenuOpen)
                U.sendGlobalBroadcast(context, ACTION_KILL_START_MENU_PROCESS);
            else
                U.sendGlobalBroadcast(context, ACTION_SHOW_START_MENU);
        } else {
            Intent intent = new Intent(context, com.farmerbb.taskbar.activity.StartMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    };

    private final BroadcastReceiver showReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showTaskbar();
        }
    };

    private final BroadcastReceiver tempShowReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            tempShowTaskbar();
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
        final ViewParams params = new ViewParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                -1,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                getBottomMargin(context)
        );

        // Taskbar is fixed to the bottom-left corner.
        params.gravity = Gravity.BOTTOM | Gravity.LEFT;
        int layoutId = R.layout.tb_taskbar_left;
        // Initialize views
        SharedPreferences pref = U.getSharedPreferences(context);

        layout = (LinearLayout) LayoutInflater.from(U.wrapContext(context)).inflate(layoutId, null);
        scrollView = layout.findViewById(R.id.taskbar_scrollview);

        int backgroundTint = U.getBackgroundTint(context);
        int accentColor = U.getAccentColor(context);

        space = layout.findViewById(R.id.space);
        layout.findViewById(R.id.space_alt).setVisibility(View.GONE);

        space.setOnClickListener(v -> {});

        startButton = layout.findViewById(R.id.start_button);
        drawStartButton(context, startButton);

        if(scrollView != null)
            scrollView.setVisibility(View.VISIBLE);

        layout.setBackgroundColor(backgroundTint);
        layout.findViewById(R.id.divider).setBackgroundColor(
                pref.getBoolean(PREF_CENTERED_ICONS, false) ? 0 : accentColor
        );

        applyMarginFix(host, layout, params);

        if(isFirstStart && FreeformHackHelper.getInstance().isInFreeformWorkspace())
            showTaskbar();

        U.registerReceiver(context, showReceiver, ACTION_SHOW_TASKBAR);
        U.registerReceiver(context, tempShowReceiver, ACTION_TEMP_SHOW_TASKBAR);

        host.addView(layout, params);

        isFirstStart = false;
    }

    @VisibleForTesting
    void drawStartButton(Context context, ImageView startButton) {
        startButton.setImageResource(U.getStartButtonIcon());
        int padding = context.getResources().getDimensionPixelSize(R.dimen.tb_app_drawer_icon_padding);
        startButton.setPadding(padding, padding, padding, padding);
        startButton.setOnClickListener(ocl);
    }

    private void showTaskbar() {
        layout.setVisibility(View.VISIBLE);
        startButton.setVisibility(View.VISIBLE);
        space.setVisibility(View.VISIBLE);

        if(scrollView != null)
            scrollView.setVisibility(View.VISIBLE);

    }

    private void tempShowTaskbar() {
        showTaskbar();
    }

    @Override
    public void onDestroyHost(UIHost host) {
        if(layout != null)
            try {
                host.removeView(layout);
            } catch (IllegalArgumentException ignored) {}

        U.unregisterReceiver(context, showReceiver);
        U.unregisterReceiver(context, tempShowReceiver);

        isFirstStart = true;
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
