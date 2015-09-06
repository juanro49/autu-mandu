/*
 * Copyright 2013 Jan Kühle
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

import me.kuehle.carreport.BuildConfig;
import me.kuehle.carreport.R;

public class PreferencesAboutFragment extends Fragment {
    public static class LicenseDialogFragment extends DialogFragment {
        private static final String TAG = "LicenseDialogFragment";

        public static LicenseDialogFragment newInstance() {
            return new LicenseDialogFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ScrollView v = new ScrollView(getActivity());
            v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            v.setPadding(16, 16, 16, 16);

            TextView text = new TextView(getActivity());
            text.setMovementMethod(LinkMovementMethod.getInstance());
            try {
                InputStream in = getActivity().getAssets().open("licenses.html");
                byte[] buffer = new byte[in.available()];
                in.read(buffer);
                in.close();
                text.setText(Html.fromHtml(new String(buffer)));
            } catch (IOException e) {
                Log.e(TAG, "Error loading license html file.", e);
            }

            v.addView(text, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.alert_about_licenses_title)
                    .setView(v)
                    .setPositiveButton(android.R.string.ok, null)
                    .create();
        }
    }

    private View.OnClickListener licensesOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            LicenseDialogFragment.newInstance().show(getFragmentManager(), null);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_prefs_about, container, false);

        String strVersion = getString(R.string.about_version, BuildConfig.VERSION_NAME);
        ((TextView) root.findViewById(R.id.txt_version)).setText(strVersion);
        root.findViewById(R.id.btn_licenses).setOnClickListener(licensesOnClickListener);

        return root;
    }
}