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

package com.farmerbb.taskbar.fragment;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class GeneralFragment extends SettingsFragment {

    @Override
    protected void loadPrefs() {
        addPreferencesFromResource(R.xml.tb_pref_general);

        if(U.canEnableFreeform(getActivity())
                && !U.isChromeOs(getActivity())
                && !U.isOverridingFreeformHack(getActivity(), false)) {
            findPreference(PREF_HIDE_TASKBAR).setSummary(R.string.tb_hide_taskbar_disclaimer);
        }

        if(!U.isChromeOs(getActivity()))
            getPreferenceScreen().removePreference(findPreference(PREF_CHROME_OS_CONTEXT_MENU_FIX));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setTitle(R.string.tb_pref_header_general);
        ActionBar actionBar = activity.getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }
}
