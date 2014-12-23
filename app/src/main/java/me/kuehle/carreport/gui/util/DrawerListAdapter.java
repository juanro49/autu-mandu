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
    private static final int VIEW_TYPE_PRIMARY = 0;
    private static final int VIEW_TYPE_SECONDARY = 1;
    private static final int VIEW_TYPE_SEPARATOR = 2;

    private Context mContext;
    private DrawerListItem[] mItems;

    public DrawerListAdapter(Context context, DrawerListItem[] items){
        mContext = context;
        mItems = items;
    }

    public DrawerListAdapter(Context context, String[] items, int[] relatedIcons){
        mContext = context;

        mItems = new DrawerListItem[items.length];
        for (int i = 0; i < items.length; i++) {
            if(items[i] == null || items[i].isEmpty()) {
                mItems[i] = new DrawerListItem();
            } else if (i >= relatedIcons.length) {
                mItems[i] = new DrawerListItem(items[i]);
            } else {
                mItems[i] = new DrawerListItem(items[i], relatedIcons[i]);
            }
        }
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
        if(item.isSeparator()) {
            return VIEW_TYPE_SEPARATOR;
        } else if(item.isPrimary()) {
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

        if (viewType == VIEW_TYPE_PRIMARY) {
            ImageView icon = (ImageView) convertView.findViewById(android.R.id.icon1);
            icon.setImageResource(item.getIcon());

            TextView text = (TextView) convertView.findViewById(android.R.id.text1);
            text.setText(item.getText());
        } else if (viewType == VIEW_TYPE_SECONDARY) {
            TextView text = (TextView) convertView.findViewById(android.R.id.text1);
            text.setText(item.getText());
        }

        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != VIEW_TYPE_SEPARATOR;
    }

    private int getViewTypeLayout(int viewType) {
        if (viewType == VIEW_TYPE_PRIMARY) {
            return R.layout.list_item_drawer_primary;
        } else if (viewType == VIEW_TYPE_SECONDARY) {
            return R.layout.list_item_drawer_secondary;
        } else {
            return R.layout.list_item_drawer_separator;
        }
    }
}
