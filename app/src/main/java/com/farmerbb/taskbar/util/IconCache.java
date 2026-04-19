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

package com.farmerbb.taskbar.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.util.DisplayMetrics;
import android.util.LruCache;

public class IconCache {

    private final LruCache<String, Drawable> drawables;

    private static IconCache theInstance;

    private IconCache(Context context) {
        final int cacheSize = 4 * 1024 * 1024; // 4MB hard-cap

        drawables = new LruCache<String, Drawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, Drawable value) {
                if(value instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) value).getBitmap();
                    if(bitmap != null) return bitmap.getByteCount();
                }

                return 1;
            }
        };
    }

    public static IconCache getInstance(Context context) {
        if(theInstance == null) theInstance = new IconCache(context.getApplicationContext());

        return theInstance;
    }

    public Drawable getIcon(Context context, LauncherActivityInfo appInfo) {
        return getIcon(context, context.getPackageManager(), appInfo);
    }

    public Drawable getIcon(Context context, PackageManager pm, LauncherActivityInfo appInfo) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        String name;

        try {
           name = appInfo.getComponentName().flattenToString() + ":" + userManager.getSerialNumberForUser(appInfo.getUser());
        } catch (NullPointerException e) {
            return pm.getDefaultActivityIcon();
        }

        Drawable drawable;

        synchronized (drawables) {
            drawable = drawables.get(name);
            if(drawable == null) {
                drawable = appInfo.getIcon(DisplayMetrics.DENSITY_MEDIUM);
                if (drawable == null) drawable = pm.getDefaultActivityIcon();
                drawables.put(name, drawable);
            }
        }

        return drawable;
    }

    public void clearCache() {
        drawables.evictAll();
    }
}
