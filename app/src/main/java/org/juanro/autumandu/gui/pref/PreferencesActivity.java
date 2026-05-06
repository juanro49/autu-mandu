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
import androidx.core.content.IntentCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
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

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            int count = getSupportFragmentManager().getBackStackEntryCount();
            if (count == 0) {
                Intent intent = getIntent();
                if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
                    setTitle(R.string.pref_title_backup);
                } else {
                    int titleRes = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0);
                    if (titleRes != 0) {
                        setTitle(titleRes);
                    } else {
                        setTitle(getTitleResourceId());
                    }
                }
            } else {
                FragmentManager.BackStackEntry entry = getSupportFragmentManager().getBackStackEntryAt(count - 1);
                setTitle(entry.getName());
            }
        });

        if (savedInstanceState == null) {
            setupInitialFragment();
        }
    }

    private void setupInitialFragment() {
        Intent intent = getIntent();
        String fragmentClassName = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);
        int titleRes = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE, 0);

        Fragment fragment;
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            fragment = createFragmentFromViewAction(intent);
        } else if (fragmentClassName != null) {
            fragment = createFragmentByClassName(fragmentClassName, titleRes, intent);
        } else {
            fragment = new PreferencesHeadersFragment();
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_wrapper, fragment)
                .commit();
    }

    private Fragment createFragmentFromViewAction(Intent intent) {
        setTitle(R.string.pref_title_backup);
        Fragment fragment = new PreferencesBackupFragment();

        Uri dataUri = intent.getData();
        if (dataUri != null) {
            String path = dataUri.getPath();
            Bundle args = new Bundle();
            if (path != null && path.endsWith(".db")) {
                args.putParcelable(PreferencesBackupFragment.EXTRA_RESTORE_DB_URI, dataUri);
            } else {
                args.putParcelable(PreferencesBackupFragment.EXTRA_IMPORT_CSV_URI, dataUri);
            }
            fragment.setArguments(args);
        }

        return fragment;
    }

    private Fragment createFragmentByClassName(String className, int titleRes, Intent intent) {
        Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(), className);
        if (titleRes != 0) {
            setTitle(titleRes);
        }

        // Check for specific URIs in extras
        Uri csvUri = IntentCompat.getParcelableExtra(intent,
                PreferencesBackupFragment.EXTRA_IMPORT_CSV_URI, Uri.class);
        Uri dbUri = IntentCompat.getParcelableExtra(intent,
                PreferencesBackupFragment.EXTRA_RESTORE_DB_URI, Uri.class);
        if (csvUri != null || dbUri != null) {
            Bundle args = new Bundle();
            if (csvUri != null) args.putParcelable(PreferencesBackupFragment.EXTRA_IMPORT_CSV_URI, csvUri);
            if (dbUri != null) args.putParcelable(PreferencesBackupFragment.EXTRA_RESTORE_DB_URI, dbUri);
            fragment.setArguments(args);
        }

        return fragment;
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
                .addToBackStack(pref.getTitle() != null ? pref.getTitle().toString() : null)
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
