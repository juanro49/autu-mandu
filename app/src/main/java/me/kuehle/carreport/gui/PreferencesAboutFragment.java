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

import java.io.IOException;
import java.io.InputStream;

import me.kuehle.carreport.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class PreferencesAboutFragment extends Fragment {
	public static class LicenseDialogFragment extends DialogFragment {
		public static LicenseDialogFragment newInstance() {
			return new LicenseDialogFragment();
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			TextView view = new TextView(getActivity());
			view.setMovementMethod(LinkMovementMethod.getInstance());
			view.setPadding(16, 16, 16, 16);
			try {
				InputStream in = getActivity().getAssets().open(
						"licenses.html");
				byte[] buffer = new byte[in.available()];
				in.read(buffer);
				in.close();
				view.setText(Html.fromHtml(new String(buffer)));
			} catch (IOException e) {
			}

			return new AlertDialog.Builder(getActivity())
					.setTitle(R.string.alert_about_licenses_title)
					.setView(view)
					.setPositiveButton(android.R.string.ok, null).create();
		}
	}

	private View.OnClickListener licensesOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			LicenseDialogFragment.newInstance().show(getFragmentManager(),
					null);
		}
	};

	public String getVersion() {
		try {
			return getActivity().getPackageManager().getPackageInfo(
					getActivity().getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			return "";
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.fragment_prefs_about,
				container, false);

		String strVersion = getString(R.string.about_version, getVersion());
		((TextView) root.findViewById(R.id.txt_version))
				.setText(strVersion);
		((Button) root.findViewById(R.id.btn_licenses))
				.setOnClickListener(licensesOnClickListener);

		return root;
	}
}