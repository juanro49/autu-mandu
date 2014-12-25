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

import android.content.Intent;
import android.support.v4.app.Fragment;

public class DrawerListItem {
    private CharSequence mText;
    private int mIcon;
    private boolean mIsSeparator;
    private boolean mIsPrimary;
    private Fragment mFragment;
    private Intent mIntent;

    /**
     * Creates a separator item.
     */
    public DrawerListItem() {
        mIsSeparator = true;
    }

    /**
     * Creates a secondary item with the specified title.
     * @param text
     */
    public DrawerListItem(CharSequence text, Intent intent) {
        mText = text;
        mIntent = intent;
    }

    /**
     * Creates a primary item with the specified title and icon.
     * @param text
     * @param icon
     */
    public DrawerListItem(CharSequence text, int icon, Fragment fragment) {
        mText = text;
        mIcon = icon;
        mIsPrimary = true;
        mFragment = fragment;
    }

    public CharSequence getText() {
        return mText;
    }

    public void setText(CharSequence text) {
        mText = text;
    }

    public int getIcon() {
        return mIcon;
    }

    public void setIcon(int icon) {
        mIcon = icon;
    }

    public boolean isSeparator() {
        return mIsSeparator;
    }

    public void setSeparator(boolean isSeparator) {
        mIsSeparator = isSeparator;
    }

    public boolean isPrimary() {
        return mIsPrimary;
    }

    public void setPrimary(boolean isPrimary) {
        mIsPrimary = isPrimary;
    }

    public Fragment getFragment() {
        return mFragment;
    }

    public void setFragment(Fragment fragment) {
        mFragment = fragment;
    }

    public Intent getIntent() {
        return mIntent;
    }

    public void setIntent(Intent intent) {
        mIntent = intent;
    }
}
