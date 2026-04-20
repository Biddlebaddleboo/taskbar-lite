/* Copyright 2019 Braden Farmer
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

package com.farmerbb.taskbar.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.ui.TaskbarController;
import com.farmerbb.taskbar.ui.UIController;
import com.farmerbb.taskbar.ui.UIHostService;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class TaskbarService extends UIHostService {
    private static final String CHANNEL_ID = "taskbar_notification_channel";

    private boolean foregroundStarted = false;

    private final BroadcastReceiver userForegroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startService(new Intent(context, TaskbarService.class));
        }
    };

    private final BroadcastReceiver userBackgroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopService(new Intent(context, TaskbarService.class));
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundIfNeeded();
        return START_STICKY;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundIfNeeded();
    }

    @Override
    public void onDestroy() {
        if(foregroundStarted) {
            try {
                unregisterReceiver(userForegroundReceiver);
            } catch (IllegalArgumentException ignored) {}

            try {
                unregisterReceiver(userBackgroundReceiver);
            } catch (IllegalArgumentException ignored) {}

            foregroundStarted = false;
        }

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean(PREF_IS_RESTARTING, false))
            pref.edit().remove(PREF_IS_RESTARTING).apply();
        else if(U.isChromeOs(this))
            U.stopFreeformHack(this);

        super.onDestroy();
    }

    @Override
    public UIController newController() {
        return new TaskbarController(this);
    }

    private void startForegroundIfNeeded() {
        if(foregroundStarted)
            return;

        SharedPreferences pref = U.getSharedPreferences(this);
        if(!U.canDrawOverlays(this)) {
            pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();
            stopSelf();
            return;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.tb_app_name);
            int importance = NotificationManager.IMPORTANCE_MIN;

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if(mNotificationManager != null)
                mNotificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, name, importance));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentTitle("T")
                .setContentText("T")
                .setPriority(Notification.PRIORITY_MIN)
                .setShowWhen(false)
                .setOngoing(true);

        startForeground(8675309, builder.build());

        registerReceiver(userForegroundReceiver, new IntentFilter(Intent.ACTION_USER_FOREGROUND));
        registerReceiver(userBackgroundReceiver, new IntentFilter(Intent.ACTION_USER_BACKGROUND));
        foregroundStarted = true;
    }
}
