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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(U.relaunchActivityIfNeeded(this)) return;

        U.initPrefs(this);

        if(!U.isLibrary(this)) {
            U.setComponentEnabled(this, HomeActivity.class, true);
            U.setComponentEnabled(this, ShortcutActivity.class,
                    U.enableFreeformModeShortcut(this));
            U.setComponentEnabled(this, StartTaskbarActivity.class, true);
        }

        Intent intent = new Intent(ACTION_START);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
        finish();
    }
}
