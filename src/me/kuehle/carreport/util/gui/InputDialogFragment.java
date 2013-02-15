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

package me.kuehle.carreport.util.gui;

import me.kuehle.carreport.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class InputDialogFragment extends DialogFragment {
	public static interface InputDialogFragmentListener {
		public void onDialogNegativeClick(int requestCode);

		public void onDialogPositiveClick(int requestCode, String input);
	}

	public static InputDialogFragment newInstance(Fragment parent,
			int requestCode, Integer title, String input) {
		InputDialogFragment f = new InputDialogFragment();
		f.setTargetFragment(parent, requestCode);

		Bundle args = new Bundle();
		args.putString("input", input != null ? input : "");
		args.putInt("positive", android.R.string.ok);
		args.putInt("negative", android.R.string.cancel);
		if (title != null) {
			args.putInt("title", title);
		}

		f.setArguments(args);
		return f;
	}

	private EditText mEditText;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();

		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_input, null);
		mEditText = (EditText) view.findViewById(R.id.edt_input);
		if (savedInstanceState != null) {
			mEditText.setText(savedInstanceState.getString("input"));
		} else {
			mEditText.setText(args.getString("input"));
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view);
		builder.setPositiveButton(args.getInt("positive"),
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						getListener().onDialogPositiveClick(
								getTargetRequestCode(),
								mEditText.getText().toString());
					}
				});
		builder.setNegativeButton(args.getInt("negative"),
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						getListener().onDialogNegativeClick(
								getTargetRequestCode());
					}
				});
		if (args.containsKey("title")) {
			builder.setTitle(args.getInt("title"));
		}

		return builder.create();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("input", mEditText.getText().toString());
	}

	private InputDialogFragmentListener getListener() {
		return (InputDialogFragmentListener) getTargetFragment();
	}
}
