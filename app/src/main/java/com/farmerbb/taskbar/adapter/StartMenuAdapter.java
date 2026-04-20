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

package com.farmerbb.taskbar.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.helper.FreeformHackHelper;
import com.farmerbb.taskbar.util.U;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.farmerbb.taskbar.util.Constants.*;

public class StartMenuAdapter extends ArrayAdapter<AppEntry> implements SectionIndexer {

    private final boolean isGrid;

    private final List<Character> sections = new ArrayList<>();
    private final SparseIntArray gpfsCache = new SparseIntArray();
    private final SparseIntArray gsfpCache = new SparseIntArray();

    private final List<Character> lowercase = Arrays.asList(
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
    );

    private final List<Character> uppercase = Arrays.asList(
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
    );

    public StartMenuAdapter(Context context, int layout, List<AppEntry> list) {
        super(context, layout, list);
        isGrid = layout == R.layout.tb_row_alt;

        updateList(list, true);
    }

    @Override
    public @NonNull View getView(int position, View convertView, final @NonNull ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(isGrid ? R.layout.tb_row_alt : R.layout.tb_row, parent, false);
        }

        final AppEntry entry = getItem(position);
        assert entry != null;

        TextView textView = convertView.findViewById(R.id.name);
        textView.setText(entry.getLabel());
        textView.setTypeface(null, android.graphics.Typeface.NORMAL);
        textView.setTextColor(ContextCompat.getColor(getContext(),
                U.isDarkTheme(getContext()) ? R.color.tb_text_color_dark : R.color.tb_text_color));

        ImageView imageView = convertView.findViewById(R.id.icon);
        imageView.setImageDrawable(entry.getIcon(getContext()));

        LinearLayout layout = convertView.findViewById(R.id.entry);
        layout.setOnClickListener(view -> {
            U.sendBroadcast(getContext(), ACTION_HIDE_START_MENU);
            U.launchApp(getContext(), entry, null, false, false, view);
        });

        layout.setOnLongClickListener(view -> {
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            openContextMenu(entry, location);
            return true;
        });

        layout.setOnGenericMotionListener((view, motionEvent) -> {
            if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                    && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                int[] location = new int[2];
                view.getLocationOnScreen(location);
                openContextMenu(entry, location);
            }

            return false;
        });

        return convertView;
    }

    private void openContextMenu(final AppEntry entry, final int[] location) {
        U.sendBroadcast(getContext(), ACTION_HIDE_START_MENU_NO_RESET);

        Bundle args = new Bundle();
        args.putSerializable("app_entry", entry);
        args.putBoolean("launched_from_start_menu", true);
        args.putInt("x", location[0]);
        args.putInt("y", location[1]);

        U.newHandler().postDelayed(() -> U.startContextMenuActivity(getContext(), args), shouldDelay() ? 100 : 0);
    }

    private boolean shouldDelay() {
        return U.hasFreeformSupport(getContext())
                && U.isFreeformModeEnabled(getContext())
                && !FreeformHackHelper.getInstance().isFreeformHackActive();
    }

    public void updateList(List<AppEntry> list) {
        updateList(list, false);
    }

    private void updateList(List<AppEntry> list, boolean firstUpdate) {
        if(!firstUpdate) {
            clear();

            sections.clear();
            gsfpCache.clear();
            gpfsCache.clear();

            addAll(list);
        }

        for(AppEntry entry : list) {
            char firstLetter = getSectionForAppEntry(entry);
            if(!sections.contains(firstLetter))
                sections.add(firstLetter);
        }
    }

    private char getSectionForAppEntry(AppEntry entry) {
        if(entry.getLabel().equals(""))
            return ' ';

        char origChar = entry.getLabel().charAt(0);
        if(uppercase.contains(origChar))
            return origChar;

        if(lowercase.contains(origChar))
            return uppercase.get(lowercase.indexOf(origChar));

        return '#';
    }

    @Override
    public int getPositionForSection(int section) {
        int cachedPos = gpfsCache.get(section, -1);
        if(cachedPos != -1)
            return cachedPos;

        for(int i = 0; i < getCount(); i++) {
            if(sections.get(section) == getSectionForAppEntry(getItem(i))) {
                gpfsCache.put(section, i);
                return i;
            }
        }

        gpfsCache.put(section, 0);
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        int cachedSection = gsfpCache.get(position, -1);
        if(cachedSection != -1)
            return cachedSection;

        for(int i = 0; i < sections.size(); i++) {
            if(sections.get(i) == getSectionForAppEntry(getItem(position))) {
                gsfpCache.put(position, i);
                return i;
            }
        }

        gsfpCache.put(position, 0);
        return 0;
    }

    @Override
    public Object[] getSections() {
        return sections.toArray();
    }
}
