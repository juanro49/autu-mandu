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

package me.kuehle.carreport.gui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class SupportMessageDialogFragment extends DialogFragment {
	public interface SupportMessageDialogFragmentListener {
		void onDialogNegativeClick(int requestCode);

		void onDialogPositiveClick(int requestCode);
	}

	public static SupportMessageDialogFragment newInstance(Fragment parent,
			int requestCode, Integer title, String message, int positive,
			Integer negative) {
		SupportMessageDialogFragment f = new SupportMessageDialogFragment();
		f.setTargetFragment(parent, requestCode);

		Bundle args = new Bundle();
		args.putString("message", message);
		args.putInt("positive", positive);
		if (title != null) {
			args.putInt("title", title);
		}
		if (negative != null) {
			args.putInt("negative", negative);
		}

		f.setArguments(args);
		return f;
	}

	private SupportMessageDialogFragmentListener mDefaultListener = new SupportMessageDialogFragmentListener() {
		@Override
		public void onDialogPositiveClick(int requestCode) {
		}

		@Override
		public void onDialogNegativeClick(int requestCode) {
		}
	};

	@NonNull
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(args.getString("message"));
		builder.setPositiveButton(args.getInt("positive"),
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						getListener().onDialogPositiveClick(
								getTargetRequestCode());
					}
				});
		if (args.containsKey("title")) {
			builder.setTitle(args.getInt("title"));
		}
		if (args.containsKey("negative")) {
			builder.setNegativeButton(args.getInt("negative"),
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							getListener().onDialogNegativeClick(
									getTargetRequestCode());
						}
					});
		}

		return builder.create();
	}

	private SupportMessageDialogFragmentListener getListener() {
		Fragment f = getTargetFragment();
		return f != null ? (SupportMessageDialogFragmentListener) f
				: mDefaultListener;
	}
}
