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

package org.juanro.autumandu.gui;

import android.os.Bundle;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.util.AbstractPreferenceActivity;
import org.juanro.autumandu.util.Assets;

public class HelpActivity extends AbstractPreferenceActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_wrapper, new HelpHeadersFragment())
                    .commit();
        }
    }

    @Override
    protected int getTitleResourceId() {
        return R.string.title_help;
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

    public static class HelpHeadersFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            setPreferencesFromResource(R.xml.help_preferences, rootKey);
        }
    }

    public abstract static class HelpFragment extends Fragment {
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_help_detail, container, false);
            TextView text = v.findViewById(android.R.id.text1);
            text.setMovementMethod(LinkMovementMethod.getInstance());

            String assetPath = String.format(
                    "%s/%s.html",
                    getString(R.string.help_folder_name),
                    getHelpId());
            Spanned html = Assets.getHtml(requireContext(), assetPath);

            text.setText(html);

            return v;
        }

        protected abstract String getHelpId();
    }

    public static class GettingStartedFragment extends HelpFragment {
        @Override
        protected String getHelpId() {
            return "getting_started";
        }
    }

    public static class TipsFragment extends HelpFragment {
        @Override
        protected String getHelpId() {
            return "tips";
        }
    }

    public static class CalculationsFragment extends HelpFragment {
        @Override
        protected String getHelpId() {
            return "calculations";
        }
    }

    public static class CSVFragment extends HelpFragment {
        @Override
        protected String getHelpId() {
            return "csv";
        }
    }

    public static class FuelTypesFragment extends HelpFragment {
        @Override
        protected String getHelpId() {
            return "fuel_types";
        }
    }
}
