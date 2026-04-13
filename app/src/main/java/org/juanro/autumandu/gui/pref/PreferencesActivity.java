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

package org.juanro.autumandu.gui.pref;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.util.AbstractPreferenceActivity;

public class PreferencesActivity extends AbstractPreferenceActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    public static final String EXTRA_SHOW_FRAGMENT = "show_fragment";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = "show_fragment_title";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            String fragmentClassName = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);
            int titleRes = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0);
            Uri csvUri = null;
            Uri dbUri = null;

            // Check if we were started to view a file
            if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
                fragmentClassName = PreferencesBackupFragment.class.getName();
                titleRes = R.string.pref_title_backup;
                Uri dataUri = intent.getData();
                String path = dataUri.getPath();
                if (path != null && path.endsWith(".db")) {
                    dbUri = dataUri;
                } else {
                    csvUri = dataUri;
                }
            }

            Fragment fragment;
            if (fragmentClassName != null) {
                fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                        getClassLoader(), fragmentClassName);
                if (titleRes != 0) {
                    setTitle(titleRes);
                }
                if (csvUri != null) {
                    Bundle args = new Bundle();
                    args.putParcelable(PreferencesBackupFragment.EXTRA_IMPORT_CSV_URI, csvUri);
                    fragment.setArguments(args);
                } else if (dbUri != null) {
                    Bundle args = new Bundle();
                    args.putParcelable(PreferencesBackupFragment.EXTRA_RESTORE_DB_URI, dbUri);
                    fragment.setArguments(args);
                }
            } else {
                fragment = new PreferencesHeadersFragment();
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_wrapper, fragment)
                    .commit();
        }
    }

    @Override
    protected int getTitleResourceId() {
        return R.string.title_settings;
    }

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        final String fragmentClassName = pref.getFragment();
        if (fragmentClassName == null) {
            return false;
        }

        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                fragmentClassName);
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_wrapper, fragment)
                .addToBackStack(null)
                .commit();

        if (pref.getTitle() != null) {
            setTitle(pref.getTitle());
        }

        return true;
    }

    public static class PreferencesHeadersFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.preference_headers, rootKey);
        }
    }
}
