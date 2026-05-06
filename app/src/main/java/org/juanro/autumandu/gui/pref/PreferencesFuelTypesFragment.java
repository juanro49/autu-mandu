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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.dialog.EditFuelTypeDialogFragment;
import org.juanro.autumandu.gui.dialog.MessageDialogFragment;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.viewmodel.FuelTypesViewModel;

import java.util.ArrayList;
import java.util.List;

public class PreferencesFuelTypesFragment extends AbstractPreferencesListFragment {

    private FuelTypesViewModel mViewModel;

    private class FuelTypeAdapter extends BaseAdapter {
        private List<FuelType> mFuelTypes = new ArrayList<>();

        @Override
        public int getCount() { return mFuelTypes.size(); }
        @Override
        public FuelType getItem(int position) { return mFuelTypes.get(position); }
        @Override
        public long getItemId(int position) { return mFuelTypes.get(position).getId(); }

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
        public boolean hasStableIds() { return true; }

        public void setFuelTypes(List<FuelType> fuelTypes) {
            mFuelTypes = fuelTypes;
            notifyDataSetChanged();
        }

        private static class ViewHolder {
            TextView text1;
            TextView text2;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(FuelTypesViewModel.class);
        super.onViewCreated(view, savedInstanceState);

        FuelTypeAdapter listAdapter = new FuelTypeAdapter();
        setListAdapter(listAdapter);
        mViewModel.getFuelTypes().observe(getViewLifecycleOwner(), listAdapter::setFuelTypes);
    }

    @Override
    protected int getCabMenu() { return R.menu.edit_fuel_types_cab; }

    @Override
    protected String getEditRequestKey() { return EditFuelTypeDialogFragment.REQUEST_KEY; }

    @Override
    protected void checkUsageAndConfirmDelete(long[] ids) {
        mViewModel.checkUsage(ids, isUsed -> requireActivity().runOnUiThread(() -> {
            if (isUsed) {
                MessageDialogFragment.newInstance(0, R.string.alert_delete_title,
                        getString(R.string.alert_cannot_delete_fuel_type),
                        android.R.string.ok, null)
                        .show(getParentFragmentManager(), null);
            } else {
                MessageDialogFragment.newInstance(REQUEST_DELETE,
                        R.string.alert_delete_title,
                        getString(R.string.alert_delete_fuel_types_message, ids.length),
                        android.R.string.ok, android.R.string.cancel)
                        .show(getParentFragmentManager(), null);
            }
        }));
    }

    @Override
    protected void deleteSelected(long[] ids) {
        mViewModel.deleteFuelTypes(ids);
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
}
