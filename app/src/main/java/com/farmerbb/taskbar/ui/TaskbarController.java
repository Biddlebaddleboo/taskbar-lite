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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.graphics.Rect;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.util.TaskbarFramebuffer;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class TaskbarController extends UIController {

    private View layout;
    private boolean isFirstStart = true;

    private final View.OnClickListener ocl = view -> {
        SharedPreferences pref = U.getSharedPreferences(context);
        boolean startMenuProcessRunning = U.isProcessRunning(context, context.getPackageName() + ":startmenu");
        boolean startMenuOpen = pref.getBoolean(PREF_START_MENU_OPEN, false);

        if(startMenuOpen) {
            pref.edit().putBoolean(PREF_START_MENU_OPEN, false).commit();
            U.sendGlobalBroadcast(context, ACTION_KILL_START_MENU_PROCESS);
        } else if(startMenuProcessRunning) {
            pref.edit().putBoolean(PREF_START_MENU_OPEN, false).commit();
            U.sendGlobalBroadcast(context, ACTION_KILL_START_MENU_PROCESS);
            U.newHandler().postDelayed(this::openStartMenu, 100);
        } else {
            openStartMenu();
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

    private final BroadcastReceiver startMenuAppearingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            U.getSharedPreferences(context).edit().putBoolean(PREF_START_MENU_OPEN, true).commit();
        }
    };

    private final BroadcastReceiver startMenuDisappearingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            U.getSharedPreferences(context).edit().putBoolean(PREF_START_MENU_OPEN, false).commit();
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
        layout = createTaskbarPlaceholder();

        applyMarginFix(host, layout, params);

        if(isFirstStart && FreeformHackHelper.getInstance().isInFreeformWorkspace())
            showTaskbar();

        U.registerReceiver(context, showReceiver, ACTION_SHOW_TASKBAR);
        U.registerReceiver(context, tempShowReceiver, ACTION_TEMP_SHOW_TASKBAR);
        ContextCompat.registerReceiver(context,
                startMenuAppearingReceiver,
                new IntentFilter(ACTION_START_MENU_APPEARING),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(context,
                startMenuDisappearingReceiver,
                new IntentFilter(ACTION_START_MENU_DISAPPEARING),
                ContextCompat.RECEIVER_NOT_EXPORTED);

        host.addView(layout, params);

        isFirstStart = false;
    }

    @VisibleForTesting
    View createTaskbarPlaceholder() {
        return new TaskbarFramebufferView(U.wrapContext(context));
    }

    private int dpToPx(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                context.getResources().getDisplayMetrics()
        ));
    }

    private final class TaskbarFramebufferView extends View {
        private final Bitmap bitmap;
        private final Rect bitmapBounds = new Rect();

        TaskbarFramebufferView(Context context) {
            super(context);
            setClickable(true);
            setOnClickListener(ocl);
            bitmap = createBitmap();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int desiredWidth = dpToPx(TaskbarFramebuffer.WIDTH);
            int desiredHeight = dpToPx(TaskbarFramebuffer.HEIGHT);

            int width = resolveSize(desiredWidth, widthMeasureSpec);
            int height = resolveSize(desiredHeight, heightMeasureSpec);

            setMeasuredDimension(width, height);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            bitmapBounds.set(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(bitmap, null, bitmapBounds, null);
        }

        private Bitmap createBitmap() {
            Bitmap bitmap = Bitmap.createBitmap(
                    TaskbarFramebuffer.WIDTH,
                    TaskbarFramebuffer.HEIGHT,
                    Bitmap.Config.RGB_565
            );

            ByteBuffer buffer = ByteBuffer.allocate(TaskbarFramebuffer.FRAMEBUFFER.length * 2)
                    .order(ByteOrder.nativeOrder());

            for(int pixel : TaskbarFramebuffer.FRAMEBUFFER)
                buffer.putShort((short) pixel);

            buffer.rewind();
            bitmap.copyPixelsFromBuffer(buffer);

            return bitmap;
        }
    }

    private void openStartMenu() {
        Intent intent = new Intent(context, com.farmerbb.taskbar.activity.StartMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private void showTaskbar() {
        if(layout != null)
            layout.setVisibility(View.VISIBLE);
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
        try {
            context.unregisterReceiver(startMenuAppearingReceiver);
        } catch (IllegalArgumentException ignored) {}
        try {
            context.unregisterReceiver(startMenuDisappearingReceiver);
        } catch (IllegalArgumentException ignored) {}

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
