/*
 * Copyright 2026 Juanro49
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.juanro.autumandu.gui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.dto.RefuelingWithDetails;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RefuelingArrayAdapter extends ArrayAdapter<RefuelingWithDetails> {
    private final DateFormat mDateFormat;
    private final String mUnitDistance;
    private final String mUnitVolume;
    private final List<RefuelingWithDetails> mItems;
    private final LayoutInflater mInflater;

    public RefuelingArrayAdapter(Context context, List<RefuelingWithDetails> items) {
        super(context, android.R.layout.simple_spinner_dropdown_item);
        mItems = new ArrayList<>();
        // Add a "None" item at the beginning
        mItems.add(null);
        mItems.addAll(items);

        mInflater = LayoutInflater.from(context);
        mDateFormat = android.text.format.DateFormat.getDateFormat(context);
        Preferences prefs = new Preferences(context);
        mUnitDistance = prefs.getUnitDistance();
        mUnitVolume = prefs.getUnitVolume();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Nullable
    @Override
    public RefuelingWithDetails getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        RefuelingWithDetails item = getItem(position);
        return item != null ? item.id() : -1;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView != null ? convertView : mInflater.inflate(android.R.layout.simple_spinner_item, parent, false);
        TextView v = (TextView) view;
        v.setText(getLabel(position));
        return view;
    }

    @NonNull
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView != null ? convertView : mInflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
        TextView v = (TextView) view;
        v.setText(getLabel(position));
        return view;
    }

    private String getLabel(int position) {
        RefuelingWithDetails item = getItem(position);
        if (item == null) {
            return getContext().getString(R.string.edit_message_no_entry_selected);
        }

        return String.format(Locale.getDefault(), "%s: %d %s, %.2f %s",
                mDateFormat.format(item.date()),
                item.mileage(), mUnitDistance,
                item.volume(), mUnitVolume);
    }
}
