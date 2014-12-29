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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.kuehle.carreport.R;
import me.kuehle.carreport.db.FuelType;
import me.kuehle.carreport.gui.dialog.EditFuelTypeDialogFragment;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment;

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
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class PreferencesFuelTypesFragment extends ListFragment implements
        MessageDialogFragment.MessageDialogFragmentListener,
        EditFuelTypeDialogFragment.EditFuelTypeDialogFragmentListener {
	private class FuelTypesMultiChoiceModeListener implements MultiChoiceModeListener {
		private ActionMode mode;

		public void deleteSelectedFuelTypes() {
			SparseBooleanArray selected = getListView().getCheckedItemPositions();
			for (int i = 0; i < mFuelTypes.size(); i++) {
				if (selected.get(i)) {
					mFuelTypes.get(i).delete();
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
				SparseBooleanArray selected = getListView().getCheckedItemPositions();
				for (int i = 0; i < mFuelTypes.size(); i++) {
					FuelType type = mFuelTypes.get(i);
					if (selected.get(i) && type.getRefuelings().size() > 0) {
						MessageDialogFragment.newInstance(null, 0, R.string.alert_delete_title,
                                getString(R.string.alert_cannot_delete_fuel_type),
                                android.R.string.ok, null)
                                .show(getFragmentManager(), null);
						return true;
					}
				}

				MessageDialogFragment.newInstance(PreferencesFuelTypesFragment.this, REQUEST_DELETE,
                        R.string.alert_delete_title,
                        getString(R.string.alert_delete_fuel_types_message,
                                getListView().getCheckedItemCount()),
                        android.R.string.yes, android.R.string.no)
                        .show(getFragmentManager(), null);
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
		public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                              boolean checked) {
			int count = getListView().getCheckedItemCount();
			mode.setTitle(String.format(getString(R.string.cab_title_selected), count));
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
	}

	private static final int REQUEST_DELETE = 1;
    private static final int REQUEST_ADD = 2;
    private static final int REQUEST_EDIT = 3;

	private List<FuelType> mFuelTypes;

	private OnItemClickListener onItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            EditFuelTypeDialogFragment.newInstance(PreferencesFuelTypesFragment.this, REQUEST_EDIT,
                    mFuelTypes.get(position)).show(getFragmentManager(), null);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_fuel_type:
                EditFuelTypeDialogFragment.newInstance(PreferencesFuelTypesFragment.this,
                        REQUEST_ADD, null).show(getFragmentManager(), null);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	@Override
	public void onDialogPositiveClick(int requestCode) {
		if (requestCode == REQUEST_DELETE) {
			multiChoiceModeListener.deleteSelectedFuelTypes();
			multiChoiceModeListener.finishActionMode();
			fillList();
		} else if (requestCode == REQUEST_ADD || requestCode == REQUEST_EDIT) {
            fillList();
        }
	}

    @Override
    public void onDialogNegativeClick(int requestCode) {
    }

	@Override
	public void onStop() {
		super.onStop();
		multiChoiceModeListener.finishActionMode();
	}

	private void fillList() {
		mFuelTypes = FuelType.getAll();

		List<Map<String, String>> data = new ArrayList<>();
		for (FuelType fuelType : mFuelTypes) {
            Map<String, String> item = new HashMap<>();
            item.put("name", fuelType.name);
            item.put("category", fuelType.category);
            data.add(item);
		}

        setListAdapter(new SimpleAdapter(getActivity(), data,
                android.R.layout.simple_list_item_activated_2,
                new String[] { "name", "category" },
                new int[] { android.R.id.text1, android.R.id.text2 }));
	}
}