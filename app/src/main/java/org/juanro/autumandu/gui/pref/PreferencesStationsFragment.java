/*
 * Copyright 2023 Juanro49
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
import org.juanro.autumandu.gui.dialog.EditStationDialogFragment;
import org.juanro.autumandu.gui.dialog.MessageDialogFragment;
import org.juanro.autumandu.model.dto.StationWithVolume;
import org.juanro.autumandu.viewmodel.StationsViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PreferencesStationsFragment extends AbstractPreferencesListFragment {

    private StationsViewModel mViewModel;

    private class StationAdapter extends BaseAdapter {
        private List<StationWithVolume> mStations = new ArrayList<>();

        @Override
        public int getCount() { return mStations.size(); }
        @Override
        public StationWithVolume getItem(int position) { return mStations.get(position); }
        @Override
        public long getItemId(int position) { return mStations.get(position).station().getId(); }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(requireContext()).inflate(
                        android.R.layout.simple_list_item_activated_2, parent, false);
            }

            StationWithVolume stationWithVolume = getItem(position);
            TextView text1 = view.findViewById(android.R.id.text1);
            TextView text2 = view.findViewById(android.R.id.text2);
            text1.setText(stationWithVolume.station().getName());
            text2.setText(String.format(Locale.getDefault(), "%.2f l", stationWithVolume.totalVolume()));
            return view;
        }

        @Override
        public boolean hasStableIds() { return true; }

        public void setStations(List<StationWithVolume> stations) {
            mStations = stations;
            notifyDataSetChanged();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mViewModel = new ViewModelProvider(this).get(StationsViewModel.class);
        super.onViewCreated(view, savedInstanceState);

        StationAdapter listAdapter = new StationAdapter();
        setListAdapter(listAdapter);
        mViewModel.getStations().observe(getViewLifecycleOwner(), listAdapter::setStations);
    }

    @Override
    protected int getCabMenu() { return R.menu.edit_stations_cab; }

    @Override
    protected String getEditRequestKey() { return EditStationDialogFragment.REQUEST_KEY; }

    @Override
    protected void checkUsageAndConfirmDelete(long[] ids) {
        mViewModel.checkUsage(ids, isUsed -> requireActivity().runOnUiThread(() -> {
            if (isUsed) {
                MessageDialogFragment.newInstance(0, R.string.alert_delete_title,
                        getString(R.string.alert_cannot_delete_station),
                        android.R.string.ok, null)
                        .show(getParentFragmentManager(), null);
            } else {
                MessageDialogFragment.newInstance(REQUEST_DELETE,
                        R.string.alert_delete_title,
                        getString(R.string.alert_delete_station_message, ids.length),
                        android.R.string.ok, android.R.string.cancel)
                        .show(getParentFragmentManager(), null);
            }
        }));
    }

    @Override
    protected void deleteSelected(long[] ids) {
        mViewModel.deleteStations(ids);
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.edit_stations, menu);
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_add_station) {
            EditStationDialogFragment.newInstance(REQUEST_ADD, 0)
                    .show(getParentFragmentManager(), null);
            return true;
        }
        return false;
    }

    @Override
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        EditStationDialogFragment.newInstance(REQUEST_EDIT, id)
                .show(getParentFragmentManager(), null);
    }
}
