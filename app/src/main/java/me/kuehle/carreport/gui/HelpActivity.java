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
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.util.Locale;

import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.util.AbstractPreferenceActivity;
import me.kuehle.carreport.util.Assets;

public class HelpActivity extends AbstractPreferenceActivity {
    @Override
    protected int getHeadersResourceId() {
        return R.xml.help_headers;
    }

    @Override
    protected Class[] getFragmentClasses() {
        return new Class[]{
                GettingStartedFragment.class,
                FuelTypesFragment.class,
                TipsFragment.class,
                CalculationsFragment.class,
                CSVFragment.class
        };
    }

    private static abstract class HelpFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_help_detail, container, false);
            TextView text = v.findViewById(android.R.id.text1);
            text.setMovementMethod(LinkMovementMethod.getInstance());

            String assetPath = String.format(
                    "%s/%s.html",
                    getLocalizedDirectory("help"),
                    getHelpId());
            Spanned html = Assets.getHtml(getActivity(), assetPath);

            text.setText(html);

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
