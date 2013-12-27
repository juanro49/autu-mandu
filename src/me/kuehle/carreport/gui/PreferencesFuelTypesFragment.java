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

import java.util.List;

import me.kuehle.carreport.R;
import me.kuehle.carreport.db.FuelType;
import me.kuehle.carreport.gui.dialog.InputDialogFragment;
import me.kuehle.carreport.gui.dialog.InputDialogFragment.InputDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment.MessageDialogFragmentListener;
import android.app.ListFragment;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class PreferencesFuelTypesFragment extends ListFragment implements
		MessageDialogFragmentListener, InputDialogFragmentListener {
	private class FuelTypesMultiChoiceModeListener implements
			MultiChoiceModeListener {
		private ActionMode mode;

		public void deleteSelectedFuelTypes() {
			SparseBooleanArray selected = getListView()
					.getCheckedItemPositions();
			for (int i = 0; i < fuelTypes.size(); i++) {
				if (selected.get(i)) {
					fuelTypes.get(i).delete();
				}
			}
		}

		public void finishActionMode() {
			if (mode != null) {
				mode.finish();
			}
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.menu_delete:
				SparseBooleanArray selected = getListView()
						.getCheckedItemPositions();
				for (int i = 0; i < fuelTypes.size(); i++) {
					FuelType type = fuelTypes.get(i);
					if (selected.get(i)
							&& (type.refuelings().size() > 0 || type
									.fuelTanks().size() > 0)) {
						MessageDialogFragment
								.newInstance(
										null,
										0,
										R.string.alert_delete_title,
										getString(R.string.alert_cannot_delete_fuel_type),
										android.R.string.ok, null).show(
										getFragmentManager(), null);
						return true;
					}
				}

				MessageDialogFragment.newInstance(
						PreferencesFuelTypesFragment.this,
						REQUEST_DELETE,
						R.string.alert_delete_title,
						getString(R.string.alert_delete_fuel_types_message,
								getListView().getCheckedItemCount()),
						android.R.string.yes, android.R.string.no).show(
						getFragmentManager(), null);
				return true;
			default:
				return false;
			}
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			this.mode = mode;
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.edit_fuel_types_cab, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position,
				long id, boolean checked) {
			int count = getListView().getCheckedItemCount();
			mode.setTitle(String.format(getString(R.string.cab_title_selected),
					count));
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
	}

	private static final int REQUEST_DELETE = 1;
	private static final int REQUEST_ADD = 2;
	private static final int REQUEST_EDIT = 3;

	private static final String STATE_CURRENTLY_EDITED_FUEL_TYPE = "currently_edited_fuel_type";

	private List<FuelType> fuelTypes;
	private FuelType currentlyEditedFuelType = null;

	private OnItemClickListener onItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			currentlyEditedFuelType = fuelTypes.get(position);
			InputDialogFragment.newInstance(PreferencesFuelTypesFragment.this,
					REQUEST_EDIT, R.string.title_edit_fuel_type,
					currentlyEditedFuelType.name).show(getFragmentManager(),
					null);
		}
	};

	private FuelTypesMultiChoiceModeListener multiChoiceModeListener = new FuelTypesMultiChoiceModeListener();

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getListView().setOnItemClickListener(onItemClickListener);
		getListView().setMultiChoiceModeListener(multiChoiceModeListener);
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

		fillList();

		if (savedInstanceState != null) {
			long id = savedInstanceState.getLong(
					STATE_CURRENTLY_EDITED_FUEL_TYPE, 0);
			if (id != 0) {
				currentlyEditedFuelType = FuelType.load(FuelType.class, id);
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.edit_fuel_types, menu);
	}

	@Override
	public void onDialogNegativeClick(int requestCode) {
		currentlyEditedFuelType = null;
	}

	@Override
	public void onDialogPositiveClick(int requestCode) {
		if (requestCode == REQUEST_DELETE) {
			multiChoiceModeListener.deleteSelectedFuelTypes();
			multiChoiceModeListener.finishActionMode();
			fillList();
		}
	}

	@Override
	public void onDialogPositiveClick(int requestCode, String input) {
		if (input.isEmpty()) {
			return;
		}

		// Check, if a fuel type with the same name does already exist.
		if (requestCode == REQUEST_ADD || requestCode == REQUEST_EDIT) {
			for (FuelType fuelType : fuelTypes) {
				if (fuelType.name.equals(input)
						&& !(requestCode == REQUEST_EDIT && fuelType
								.equals(currentlyEditedFuelType))) {
					MessageDialogFragment.newInstance(null, 0, null,
							getString(R.string.alert_fuel_type_exists_message),
							android.R.string.ok, null).show(
							getFragmentManager(), null);
					return;
				}
			}
		}

		// Save fuel type.
		if (requestCode == REQUEST_ADD) {
			new FuelType(input).save();
			fillList();
		} else if (requestCode == REQUEST_EDIT) {
			currentlyEditedFuelType.name = input;
			currentlyEditedFuelType.save();
			currentlyEditedFuelType = null;
			fillList();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_fuel_type:
			InputDialogFragment.newInstance(PreferencesFuelTypesFragment.this,
					REQUEST_ADD, R.string.title_add_fuel_type, null).show(
					getFragmentManager(), null);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (currentlyEditedFuelType != null) {
			outState.putLong(STATE_CURRENTLY_EDITED_FUEL_TYPE,
					currentlyEditedFuelType.id);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		multiChoiceModeListener.finishActionMode();
	}

	private void fillList() {
		fuelTypes = FuelType.getAll();
		String[] names = new String[fuelTypes.size()];
		for (int i = 0; i < fuelTypes.size(); i++) {
			names[i] = fuelTypes.get(i).name;
		}

		setListAdapter(new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_activated_1,
				android.R.id.text1, names));
	}
}