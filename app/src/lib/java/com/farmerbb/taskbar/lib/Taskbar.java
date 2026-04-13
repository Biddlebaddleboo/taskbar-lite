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

package com.farmerbb.taskbar.lib;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

@Keep public class Taskbar {

    private Taskbar() {}

    /**
     * Starts Taskbar.
     * @param context Context used to start the activity
     */
    @Keep public static void openSettings(@NonNull Context context) {
        openSettings(context, null, -1);
    }

    /**
     * Starts Taskbar, using the specified title for compatibility.
     * @param context Context used to start the activity
     * @param title Ignored compatibility parameter.
     */
    @Keep public static void openSettings(@NonNull Context context, @Nullable String title) {
        openSettings(context, title, -1);
    }

    /**
     * Starts Taskbar, using the specified theme for compatibility.
     * @param context Context used to start the activity
     * @param theme Ignored compatibility parameter.
     */
    @Keep public static void openSettings(@NonNull Context context, @StyleRes int theme) {
        openSettings(context, null, theme);
    }

    /**
     * Starts Taskbar, using the specified title and theme for compatibility.
     * @param context Context used to start the activity
     * @param title Ignored compatibility parameter.
     * @param theme Ignored compatibility parameter.
     */
    @Keep public static void openSettings(@NonNull Context context, @Nullable String title, @StyleRes int theme) {
        Intent intent = new Intent(ACTION_START);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    /**
     * Compatibility no-op kept for callers that still invoke the old toggle API.
     * Home replacement is always enabled in this trimmed build.
     */
    @Keep public static void setEnabled(@NonNull Context context, boolean enabled) {
        // Intentionally left blank.
    }
}
