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
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;
import androidx.lifecycle.ViewModelProvider;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.dialog.EditFuelTypeDialogFragment;
import org.juanro.autumandu.gui.dialog.MessageDialogFragment;
import org.juanro.autumandu.gui.util.AbstractPreferenceActivity;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.viewmodel.FuelTypesViewModel;

import java.util.ArrayList;
import java.util.List;

public class PreferencesFuelTypesFragment extends ListFragment implements
        AbstractPreferenceActivity.OptionsMenuListener {

    private FuelTypesViewModel mViewModel;

    private class FuelTypeAdapter extends BaseAdapter {
        private List<FuelType> mFuelTypes = new ArrayList<>();

        @Override
        public int getCount() {
            return mFuelTypes.size();
        }

        @Override
        public FuelType getItem(int position) {
            return mFuelTypes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mFuelTypes.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext()).inflate(
                        android.R.layout.simple_list_item_activated_2, parent, false);
                holder = new ViewHolder();
                holder.text1 = convertView.findViewById(android.R.id.text1);
                holder.text2 = convertView.findViewById(android.R.id.text2);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FuelType fuelType = getItem(position);
            holder.text1.setText(fuelType.getName());
            holder.text2.setText(fuelType.getCategory());

            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public void setFuelTypes(List<FuelType> fuelTypes) {
            mFuelTypes = fuelTypes;
            notifyDataSetChanged();
        }

        private static class ViewHolder {
            TextView text1;
            TextView text2;
        }
    }

    private class FuelTypesMultiChoiceModeListener implements MultiChoiceModeListener {
        private ActionMode mActionMode;
        private long[] mSelectedIds;

        public void deleteSelectedFuelTypes() {
            if (mSelectedIds != null) {
                mViewModel.deleteFuelTypes(mSelectedIds);
                mSelectedIds = null;
            }
        }

        public void finishActionMode() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.menu_delete) {
                mSelectedIds = getListView().getCheckedItemIds();
                mViewModel.checkUsage(mSelectedIds, isUsed -> requireActivity().runOnUiThread(() -> {
                    if (isUsed) {
                        MessageDialogFragment.newInstance(0, R.string.alert_delete_title,
                                getString(R.string.alert_cannot_delete_fuel_type),
                                android.R.string.ok, null)
                                .show(getParentFragmentManager(), null);
                        mSelectedIds = null;
                    } else {
                        MessageDialogFragment.newInstance(REQUEST_DELETE,
                                R.string.alert_delete_title,
                                getString(R.string.alert_delete_fuel_types_message,
                                        getListView().getCheckedItemCount()),
                                android.R.string.yes, android.R.string.no)
                                .show(getParentFragmentManager(), null);
                    }
                }));
                return true;
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.edit_fuel_types_cab, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
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

    private FuelTypesMultiChoiceModeListener mMultiChoiceModeListener;
    private FuelTypeAdapter mListAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Registro de listener para MessageDialogFragment y EditFuelTypeDialogFragment
        getParentFragmentManager().setFragmentResultListener(MessageDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            if (bundle.getInt(MessageDialogFragment.RESULT_ACTION) == MessageDialogFragment.ACTION_POSITIVE) {
                onDialogPositiveClick(bundle.getInt(MessageDialogFragment.RESULT_REQUEST_CODE));
            }
        });

        getParentFragmentManager().setFragmentResultListener(EditFuelTypeDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            if (bundle.getInt(EditFuelTypeDialogFragment.RESULT_ACTION) == EditFuelTypeDialogFragment.ACTION_POSITIVE) {
                onDialogPositiveClick(bundle.getInt(EditFuelTypeDialogFragment.RESULT_REQUEST_CODE));
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(FuelTypesViewModel.class);

        mMultiChoiceModeListener = new FuelTypesMultiChoiceModeListener();
        getListView().setMultiChoiceModeListener(mMultiChoiceModeListener);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        mListAdapter = new FuelTypeAdapter();
        setListAdapter(mListAdapter);

        mViewModel.getFuelTypes().observe(getViewLifecycleOwner(), fuelTypes -> mListAdapter.setFuelTypes(fuelTypes));
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.edit_fuel_types, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_add_fuel_type) {
            EditFuelTypeDialogFragment.newInstance(REQUEST_ADD, 0)
                    .show(getParentFragmentManager(), null);
            return true;
        }
        return false;
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        EditFuelTypeDialogFragment.newInstance(REQUEST_EDIT, id)
                .show(getParentFragmentManager(), null);
    }

    private void onDialogPositiveClick(int requestCode) {
        if (requestCode == REQUEST_DELETE) {
            mMultiChoiceModeListener.deleteSelectedFuelTypes();
            mMultiChoiceModeListener.finishActionMode();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mMultiChoiceModeListener.finishActionMode();
    }
}
