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

package com.farmerbb.taskbar.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.farmerbb.taskbar.activity.MainActivity;
import com.farmerbb.taskbar.service.TaskbarService;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class StartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref = U.getSharedPreferences(context);

        boolean taskbarNotActive = !U.isServiceRunning(context, TaskbarService.class);

        if(!U.canDrawOverlays(context)) {
            U.newHandler().postDelayed(() -> {
                Intent intent2 = new Intent(context, MainActivity.class);
                intent2.putExtra(EXTRA_SHOW_PERMISSION_DIALOG, true);
                intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent2);
            }, 250);
        } else if(taskbarNotActive) {
            U.initPrefs(context);

            SharedPreferences.Editor editor = pref.edit();

            if(pref.getBoolean(PREF_FIRST_RUN, true)) {
                editor.putBoolean(PREF_FIRST_RUN, false);
            }

            editor.putBoolean(PREF_TASKBAR_ACTIVE, true);
            editor.putLong(PREF_TIME_OF_SERVICE_START, System.currentTimeMillis());
            editor.apply();

            if(U.hasFreeformSupport(context)) {
                U.startFreeformHack(context, true);
            }

            U.startForegroundService(context, new Intent(context, TaskbarService.class));
        }
    }
}
