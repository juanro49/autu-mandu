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

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.MITLicense;
import de.psdev.licensesdialog.model.Notice;
import me.kuehle.carreport.BuildConfig;
import me.kuehle.carreport.R;

public class PreferencesAboutFragment extends Fragment {
    private View.OnClickListener licensesOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new LicensesDialog.Builder(getActivity())
                    .setTitle(R.string.about_licenses)
                    .setNoticesCssStyle(R.string.about_licenses_styles)
                    .setCloseText(android.R.string.ok)
                    .setNotices(R.raw.licenses)
                    .setIncludeOwnLicense(true)
                    .build()
                    .show();
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