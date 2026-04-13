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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.view.View;

import com.farmerbb.taskbar.util.Callbacks;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class DummyActivity extends Activity {

    boolean shouldFinish = false;
    boolean finishOnPause = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(U.relaunchActivityIfNeeded(this)) return;

        setContentView(new View(this));

        if(getIntent().hasExtra("finish_on_pause"))
            finishOnPause = true;
    }

    @SuppressLint("RestrictedApi")
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    protected void onResume() {
        super.onResume();
        if(shouldFinish)
            finish();
        else {
            shouldFinish = true;

            if(getIntent().hasExtra("uninstall")) {
                UserManager userManager = (UserManager) getSystemService(USER_SERVICE);

                Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + getIntent().getStringExtra("uninstall")));
                intent.putExtra(Intent.EXTRA_USER, userManager.getUserForSerialNumber(getIntent().getLongExtra("user_id", 0)));

                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException ignored) {}
            } else if(getIntent().hasExtra(EXTRA_START_FREEFORM_HACK)) {
                if(U.hasFreeformSupport(this)
                        && U.isFreeformModeEnabled(this)
                        && !FreeformHackHelper.getInstance().isFreeformHackActive()) {
                    U.startFreeformHack(this, true);
                }

                finish();
            } else if(getIntent().hasExtra(EXTRA_SHOW_PERMISSION_DIALOG))
                U.showPermissionDialog(U.wrapContext(this), new Callbacks(null, this::finish));
            else if(!finishOnPause)
                finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(finishOnPause)
            finish();
    }

    @Override
    public void onTopResumedActivityChanged(boolean isTopResumedActivity) {
        if(isTopResumedActivity && finishOnPause) {
            finish();
        }
    }
}
