/*
 * Copyright 2012 Jan Kühle
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

package me.kuehle.carreport.gui;

import java.util.List;

import me.kuehle.carreport.R;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class PreferencesActivity extends PreferenceActivity {
    private Toolbar mActionBar;
	private Fragment mCurrentPreferencesFragment;

	public void onAttachFragment(Fragment fragment) {
        if(fragment != null && isValidFragment(fragment.getClass().getName())) {
            mCurrentPreferencesFragment = fragment;
        }
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mActionBar.setTitle(getTitle());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.activity_preferences, new LinearLayout(this), false);

        mActionBar = (Toolbar) contentView.findViewById(R.id.action_bar);
        mActionBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ViewGroup contentWrapper = (ViewGroup) contentView.findViewById(R.id.content_wrapper);
        LayoutInflater.from(this).inflate(layoutResID, contentWrapper, true);

        getWindow().setContentView(contentView);
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (mCurrentPreferencesFragment != null) {
            mCurrentPreferencesFragment.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	protected boolean isValidFragment(String fragmentName) {
		return PreferencesGeneralFragment.class.getName().equals(fragmentName)
				|| PreferencesCarsFragment.class.getName().equals(fragmentName)
				|| PreferencesFuelTypesFragment.class.getName().equals(
						fragmentName)
				|| PreferencesReportOrderFragment.class.getName().equals(
						fragmentName)
				|| PreferencesBackupFragment.class.getName().equals(
						fragmentName)
				|| PreferencesAboutFragment.class.getName()
						.equals(fragmentName);
	}
}
