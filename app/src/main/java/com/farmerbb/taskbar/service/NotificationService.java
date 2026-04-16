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

package com.farmerbb.taskbar.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.farmerbb.taskbar.activity.MainActivity;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class NotificationService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getBooleanExtra(EXTRA_START_SERVICES, false)) {
            startService(new Intent(this, TaskbarService.class));
            startService(new Intent(this, StartMenuService.class));
        }

        return START_STICKY;
    }

    BroadcastReceiver userForegroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startService(new Intent(context, TaskbarService.class));
            startService(new Intent(context, StartMenuService.class));
        }
    };

    BroadcastReceiver userBackgroundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopService(new Intent(context, TaskbarService.class));
            stopService(new Intent(context, StartMenuService.class));

            U.clearCaches(context);
        }
    };

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        super.onCreate();
        U.setServiceRunning(getClass(), true);

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean(PREF_TASKBAR_ACTIVE, false)) {
            if(U.canDrawOverlays(this)) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                String id = "taskbar_notification_channel";

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    CharSequence name = getString(R.string.tb_app_name);
                    int importance = NotificationManager.IMPORTANCE_MIN;

                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.createNotificationChannel(new NotificationChannel(id, name, importance));
                }

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, id)
                        .setSmallIcon(U.getStartButtonIcon())
                        .setContentIntent(contentIntent)
                        .setContentTitle(getString(R.string.tb_taskbar_is_active))
                        .setContentText(getString(R.string.tb_click_to_open_taskbar))
                        .setColor(ContextCompat.getColor(this, R.color.tb_colorPrimary))
                        .setPriority(Notification.PRIORITY_MIN)
                        .setShowWhen(false)
                        .setOngoing(true);

                startForeground(8675309, mBuilder.build());

                registerReceiver(userForegroundReceiver, new IntentFilter(Intent.ACTION_USER_FOREGROUND));
                registerReceiver(userBackgroundReceiver, new IntentFilter(Intent.ACTION_USER_BACKGROUND));
            } else {
                pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();

                stopSelf();
            }
        } else stopSelf();
    }

    @Override
    public void onDestroy() {
        U.setServiceRunning(getClass(), false);

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean(PREF_IS_RESTARTING, false))
            pref.edit().remove(PREF_IS_RESTARTING).apply();
        else {
            if(U.isChromeOs(this))
                U.stopFreeformHack(this);
        }

        super.onDestroy();

        unregisterReceiver(userForegroundReceiver);
        unregisterReceiver(userBackgroundReceiver);
    }
}
