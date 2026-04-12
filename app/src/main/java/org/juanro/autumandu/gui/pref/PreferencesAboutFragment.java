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

package org.juanro.autumandu.gui.pref;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.psdev.licensesdialog.LicensesDialog;
import org.juanro.autumandu.BuildConfig;
import org.juanro.autumandu.R;
import org.juanro.autumandu.util.Assets;

public class PreferencesAboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_prefs_about, container, false);

        ((TextView) root.findViewById(R.id.txt_version))
                .setText(getString(R.string.about_version, BuildConfig.VERSION_NAME));

        ((TextView) root.findViewById(R.id.txt_thanks))
                .setText(Assets.getHtml(requireContext(), "thanks.html"));

        root.findViewById(R.id.btn_licenses).setOnClickListener(v ->
                new LicensesDialog.Builder(requireContext())
                        .setTitle(R.string.about_licenses)
                        .setNoticesCssStyle(R.string.about_licenses_styles)
                        .setCloseText(android.R.string.ok)
                        .setNotices(R.raw.licenses)
                        .setIncludeOwnLicense(true)
                        .build()
                        .show());

        return root;
    }
}
