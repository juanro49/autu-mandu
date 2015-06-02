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

package me.kuehle.carreport.gui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ListDialogFragment extends DialogFragment {
	public interface ListDialogFragmentListener {
		void onDialogNegativeClick(int requestCode);

		void onDialogPositiveClick(int requestCode, int selectedPosition);
	}

	public static ListDialogFragment newInstance(Fragment parent,
			int requestCode, Integer title, String[] items, int[] icons,
			Integer negative) {
		ListDialogFragment f = new ListDialogFragment();
		f.setTargetFragment(parent, requestCode);

		Bundle args = new Bundle();
		args.putStringArray("items", items);
		args.putIntArray("icons", icons);
		if (title != null) {
			args.putInt("title", title);
		}
		if (negative != null) {
			args.putInt("negative", negative);
		}

		f.setArguments(args);
		return f;
	}

	private class DialogListAdapter extends BaseAdapter {
		private String[] items;
		private int[] icons;

		public DialogListAdapter(String[] items, int[] icons) {
			this.items = items;
			this.icons = icons;
		}

		@Override
		public int getCount() {
			return items.length;
		}

		@Override
		public String getItem(int position) {
			return items[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(
						android.R.layout.simple_list_item_1, parent, false);
			}

			TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
			text1.setText(items[position]);
			if (icons != null) {
				text1.setCompoundDrawablesWithIntrinsicBounds(icons[position], 0, 0, 0);
				text1.setCompoundDrawablePadding(16);
			}

			return convertView;
		}
	}

	private ListDialogFragmentListener mDefaultListener = new ListDialogFragmentListener() {
		@Override
		public void onDialogNegativeClick(int requestCode) {
		}

		@Override
		public void onDialogPositiveClick(int requestCode, int selectedPosition) {
		}
	};

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setAdapter(new DialogListAdapter(args.getStringArray("items"),
				args.getIntArray("icons")), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				getListener().onDialogPositiveClick(getTargetRequestCode(), which);
			}
		});

		if (args.containsKey("title")) {
			builder.setTitle(args.getInt("title"));
		}

		if (args.containsKey("negative")) {
			builder.setNegativeButton(args.getInt("negative"), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getListener().onDialogNegativeClick(getTargetRequestCode());
                }
            });
		}

		return builder.create();
	}

	private ListDialogFragmentListener getListener() {
		Fragment f = getTargetFragment();
		return f != null ? (ListDialogFragmentListener) f : mDefaultListener;
	}
}
