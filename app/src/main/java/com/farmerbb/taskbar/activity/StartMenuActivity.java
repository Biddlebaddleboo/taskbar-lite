package com.farmerbb.taskbar.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.ui.StartMenuController;
import com.farmerbb.taskbar.ui.UIHost;
import com.farmerbb.taskbar.ui.ViewParams;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;

import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class StartMenuActivity extends Activity implements UIHost {
    private static final long SELF_DESTRUCT_DELAY_MS = 5000L;

    private StartMenuController controller;
    private final Handler selfDestructHandler = U.newHandler();
    private final Runnable selfDestructRunnable = () -> Process.killProcess(Process.myPid());

    private final BroadcastReceiver openedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            cancelSelfDestruct();
        }
    };
    private final BroadcastReceiver closedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scheduleSelfDestruct();
        }
    };
    private final BroadcastReceiver killReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            scheduleSelfDestruct();
        }
    };
    private final BroadcastReceiver toggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(controller != null)
                controller.toggleStartMenu();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int startMenuSpaceHeight = getIntent().getIntExtra(
                EXTRA_START_MENU_SPACE_HEIGHT,
                getResources().getDimensionPixelSize(R.dimen.tb_icon_size)
        );

        ContextCompat.registerReceiver(
                this,
                openedReceiver,
                new IntentFilter(ACTION_START_MENU_PROCESS_OPENED),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        ContextCompat.registerReceiver(
                this,
                closedReceiver,
                new IntentFilter(ACTION_START_MENU_PROCESS_CLOSED),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        ContextCompat.registerReceiver(
                this,
                killReceiver,
                new IntentFilter(ACTION_KILL_START_MENU_PROCESS),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
        ContextCompat.registerReceiver(
                this,
                toggleReceiver,
                new IntentFilter(ACTION_TOGGLE_START_MENU_PROCESS),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );

        controller = new StartMenuController(this, startMenuSpaceHeight);
        controller.drawStartMenu(this);
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(openedReceiver);
        } catch (IllegalArgumentException ignored) {}

        try {
            unregisterReceiver(closedReceiver);
        } catch (IllegalArgumentException ignored) {}

        try {
            unregisterReceiver(killReceiver);
        } catch (IllegalArgumentException ignored) {}

        try {
            unregisterReceiver(toggleReceiver);
        } catch (IllegalArgumentException ignored) {}

        cancelSelfDestruct();
        controller.onDestroyHost(this);
        super.onDestroy();
    }

    @Override
    public void addView(View view, ViewParams params) {
        setContentView(view);

        Window window = getWindow();
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.width = params.width;
        attrs.height = params.height;

        if(params.gravity > -1)
            attrs.gravity = params.gravity;

        if(params.bottomMargin > -1)
            attrs.y = params.bottomMargin;

        attrs.x = 0;
        attrs.flags |= params.flags;
        window.setAttributes(attrs);
    }

    @Override
    public void removeView(View view) {
        finish();
    }

    @Override
    public void updateViewLayout(View view, ViewParams params) {
        // Not needed for activity-based start menu
    }

    @Override
    public void terminate() {
        finish();
    }

    private void scheduleSelfDestruct() {
        cancelSelfDestruct();

        selfDestructHandler.postDelayed(selfDestructRunnable, SELF_DESTRUCT_DELAY_MS);
    }

    private void cancelSelfDestruct() {
        selfDestructHandler.removeCallbacks(selfDestructRunnable);
    }
}
