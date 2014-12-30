/*
 * Copyright 2014 Jan Kühle
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

package me.kuehle.carreport.gui.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import me.kuehle.carreport.R;

public class DrawerListAdapter extends BaseAdapter {
    private static final int VIEW_TYPE_TOP = 0;
    private static final int VIEW_TYPE_PRIMARY = 1;
    private static final int VIEW_TYPE_SECONDARY = 2;
    private static final int VIEW_TYPE_SEPARATOR = 3;

    private Context mContext;
    private DrawerListItem[] mItems;

    public DrawerListAdapter(Context context, DrawerListItem[] items) {
        mContext = context;
        mItems = items;
    }

    @Override
    public int getCount() {
        return mItems.length;
    }

    @Override
    public Object getItem(int position) {
        return mItems[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        DrawerListItem item = mItems[position];
        if (position == 0) {
            return VIEW_TYPE_TOP;
        } else if (item.isSeparator()) {
            return VIEW_TYPE_SEPARATOR;
        } else if (item.isPrimary()) {
            return VIEW_TYPE_PRIMARY;
        } else {
            return VIEW_TYPE_SECONDARY;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DrawerListItem item = mItems[position];
        int viewType = getItemViewType(position);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(getViewTypeLayout(viewType), parent, false);
        }

        TextView text = (TextView) convertView.findViewById(android.R.id.text1);
        if(text != null) {
            text.setText(item.getText());
        }

        ImageView icon = (ImageView) convertView.findViewById(android.R.id.icon1);
        if (icon != null) {
            icon.setImageResource(item.getIcon());
        }

        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) == VIEW_TYPE_PRIMARY ||
                getItemViewType(position) == VIEW_TYPE_SECONDARY;
    }

    public void setItems(DrawerListItem[] items) {
        mItems = items;
        notifyDataSetChanged();
    }

    private int getViewTypeLayout(int viewType) {
        if (viewType==VIEW_TYPE_TOP) {
            return R.layout.list_item_drawer_top;
        } else if (viewType == VIEW_TYPE_PRIMARY) {
            return R.layout.list_item_drawer_primary;
        } else if (viewType == VIEW_TYPE_SECONDARY) {
            return R.layout.list_item_drawer_secondary;
        } else {
            return R.layout.list_item_drawer_separator;
        }
    }
}
