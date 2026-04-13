/* Copyright 2020 Braden Farmer
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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class PersistentShortcutSelectAppActivity extends AbstractSelectAppActivity {

    private AppEntry selectedEntry;

    @Override
    public void selectApp(AppEntry entry) {
        selectedEntry = entry;

        boolean windowSizeOptions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && U.hasFreeformSupport(this);

        if(!windowSizeOptions) {
            createShortcut(null);
            return;
        }

        LinearLayout layout = (LinearLayout) View.inflate(this, R.layout.tb_shortcut_options, null);
        final Spinner spinner = layout.findViewById(R.id.spinner);

        String[] windowSizes = getResources().getStringArray(R.array.tb_pref_window_size_list_values);

        layout.findViewById(R.id.window_size_options).setVisibility(View.VISIBLE);
        String defaultWindowSize = "standard";
        for(int i = 0; i < windowSizes.length; i++) {
            if(windowSizes[i].equals(defaultWindowSize)) {
                spinner.setSelection(i);
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(selectedEntry.getLabel())
                .setView(layout)
                .setPositiveButton(R.string.tb_action_ok, (dialog, which) -> {
                    createShortcut(windowSizes[spinner.getSelectedItemPosition()]);
                })
                .setNegativeButton(R.string.tb_action_cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createShortcut(String windowSize) {
        createHomeScreenShortcut(windowSize);
        finish();
    }

    private void createHomeScreenShortcut(String windowSize) {
        try {
            Context packageContext = createPackageContext(selectedEntry.getPackageName(), Context.CONTEXT_IGNORE_SECURITY);
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(selectedEntry.getPackageName(), PackageManager.GET_META_DATA);

            Intent shortcutIntent = new Intent(this, PersistentShortcutLaunchActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            shortcutIntent.putExtra("package_name", selectedEntry.getPackageName());
            shortcutIntent.putExtra("component_name", selectedEntry.getComponentName());
            shortcutIntent.putExtra("user_id", selectedEntry.getUserId(this));

            if(windowSize != null) shortcutIntent.putExtra("window_size", windowSize);

            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(packageContext, applicationInfo.icon));
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, selectedEntry.getLabel());

            setResult(RESULT_OK, intent);
        } catch (PackageManager.NameNotFoundException ignored) {}
    }
}
