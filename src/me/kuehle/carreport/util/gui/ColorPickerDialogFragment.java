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

import com.larswerkman.colorpicker.ColorPicker;
import com.larswerkman.colorpicker.OpacityBar;
import com.larswerkman.colorpicker.SVBar;

public class ColorPickerDialogFragment extends DialogFragment {
	public static interface ColorPickerDialogFragmentListener {
		public void onDialogNegativeClick(int requestCode);

		public void onDialogPositiveClick(int requestCode, int color);
	}

	public static ColorPickerDialogFragment newInstance(Fragment parent,
			int requestCode, Integer title, int color) {
		ColorPickerDialogFragment f = new ColorPickerDialogFragment();
		f.setTargetFragment(parent, requestCode);

		Bundle args = new Bundle();
		args.putInt("color", color);
		args.putInt("positive", android.R.string.ok);
		args.putInt("negative", android.R.string.cancel);
		if (title != null) {
			args.putInt("title", title);
		}

		f.setArguments(args);
		return f;
	}

	private ColorPicker mColorPicker;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();

		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_color_picker, null);
		mColorPicker = (ColorPicker) view.findViewById(R.id.picker);
		mColorPicker.addSVBar((SVBar) view.findViewById(R.id.svbar));
		mColorPicker.addOpacityBar((OpacityBar) view
				.findViewById(R.id.opacitybar));

		mColorPicker.setOldCenterColor(args.getInt("color"));
		if (savedInstanceState != null) {
			mColorPicker.setColor(savedInstanceState.getInt("color"));
		} else {
			mColorPicker.setColor(args.getInt("color"));
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(view);
		builder.setPositiveButton(args.getInt("positive"),
				new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						getListener()
								.onDialogPositiveClick(getTargetRequestCode(),
										mColorPicker.getColor());
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
		outState.putInt("color", mColorPicker.getColor());
	}

	private ColorPickerDialogFragmentListener getListener() {
		return (ColorPickerDialogFragmentListener) getTargetFragment();
	}
}
