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

import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import me.kuehle.carreport.R;

public class HelpActivity extends PreferenceActivity {
    private static final String TAG = "HelpActivity";

    private Toolbar mActionBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionBar.setTitle(getTitle());
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.help_headers, target);
    }

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(
                R.layout.activity_help, new LinearLayout(this), false);

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
    protected boolean isValidFragment(String fragmentName) {
        return GettingStartedFragment.class.getName().equals(fragmentName)
                || FuelTypesFragment.class.getName().equals(fragmentName)
                || TipsFragment.class.getName().equals(fragmentName)
                || CalculationsFragment.class.getName().equals(fragmentName)
                || CSVFragment.class.getName().equals(fragmentName);
    }

    private static abstract class HelpFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_help_detail, container, false);
            TextView text = (TextView) v.findViewById(android.R.id.text1);
            text.setMovementMethod(LinkMovementMethod.getInstance());

            try {
                InputStream in = getActivity().getAssets().open(
                        String.format("%s/%s.html", getLocalizedDirectory("help"), getHelpId()));
                byte[] buffer = new byte[in.available()];
                in.read(buffer);
                in.close();

                text.setText(Html.fromHtml(new String(buffer)));
            } catch (IOException e) {
                Log.e(TAG, "Error reading help html file.", e);
            }

            return v;
        }

        protected abstract String getHelpId();

        private String getLocalizedDirectory(String directory) {
            String locale = Locale.getDefault().getLanguage().substring(0, 2)
                    .toLowerCase(Locale.US);
            String localizedDirectory = directory + "-" + locale;

            try {
                if (getActivity().getAssets().list(localizedDirectory).length > 0) {
                    return localizedDirectory;
                } else {
                    return directory;
                }
            } catch (IOException e) {
                return directory;
            }
        }
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
