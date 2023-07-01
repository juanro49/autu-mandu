/*
 * Copyright 2017 Jan Kühle
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
package org.juanro.autumandu.gui.util;

import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import androidx.appcompat.widget.Toolbar;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.juanro.autumandu.R;

public abstract class AbstractPreferenceActivity extends PreferenceActivity implements
        Toolbar.OnMenuItemClickListener {
    public interface OptionsMenuListener {
        int getOptionsMenuResourceId();
    }

    private Toolbar mActionBar;
    private Fragment mCurrentMenuListener;
    private List<PreferenceFragment> mAttachedPrefFragments = new ArrayList<>(getFragmentClasses().length);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionBar.setTitle(getTitle());
        mActionBar.setOnMenuItemClickListener(this);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(getHeadersResourceId(), target);

        // Fix text color of breadcrumbs.
        // https://stackoverflow.com/a/27078485/1798215
        View breadcrumb = findViewById(android.R.id.title);
        if (breadcrumb != null) {
            try {
                Field titleColor = breadcrumb.getClass().getDeclaredField("mTextColor");
                titleColor.setAccessible(true);
                titleColor.setInt(breadcrumb, getResources().getColor(R.color.primary_text));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.activity_preference_base, new LinearLayout(this), false);

        mActionBar = contentView.findViewById(R.id.action_bar);
        mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        inflateMenu();

        ViewGroup contentWrapper = contentView.findViewById(R.id.content_wrapper);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);

        getWindow().setContentView(contentView);
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        mActionBar.setTitle(title);

        // In single pane mode, this seems to be the only way to detect, that the view switched
        // from a preference panel back to the header list.
        if (title.equals(getString(getTitleResourceId()))) {
            clearMenuAndListener();
        }
    }

    @Override
    public void switchToHeader(String fragmentName, Bundle args) {
        clearMenuAndListener();
        super.switchToHeader(fragmentName, args);
    }

    @Override
    public void switchToHeader(Header header) {
        clearMenuAndListener();
        super.switchToHeader(header);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        if (fragment instanceof PreferenceFragment && !mAttachedPrefFragments.contains(fragment)) {
            mAttachedPrefFragments.add((PreferenceFragment) fragment);
        }
        if (fragment instanceof OptionsMenuListener) {
            mCurrentMenuListener = fragment;
            inflateMenu();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return mCurrentMenuListener != null && mCurrentMenuListener.onOptionsItemSelected(item);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        Class[] classes = getFragmentClasses();
        for (Class clazz : classes) {
            if (clazz.getName().equals(fragmentName)) {
                return true;
            }
        }

        return false;
    }

    private void inflateMenu() {
        if (mActionBar != null && mCurrentMenuListener != null) {
            int menuRes = ((OptionsMenuListener) mCurrentMenuListener).getOptionsMenuResourceId();
            mActionBar.inflateMenu(menuRes);
        }
    }

    private void clearMenuAndListener() {
        mActionBar.getMenu().clear();
        mCurrentMenuListener = null;
    }

    protected List<PreferenceFragment> getAttachedFragments() {
        return new ArrayList<>(mAttachedPrefFragments);
    }

    protected abstract int getTitleResourceId();

    protected abstract int getHeadersResourceId();

    protected abstract Class[] getFragmentClasses();
}
