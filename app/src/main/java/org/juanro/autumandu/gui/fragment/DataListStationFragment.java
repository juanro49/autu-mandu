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

package org.juanro.autumandu.gui.fragment;

import android.os.Bundle;
import android.util.SparseArray;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.DataDetailActivity;
import org.juanro.autumandu.model.dto.StationWithVolume;
import org.juanro.autumandu.model.entity.FuelCategory;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DataListStationFragment extends AbstractDataListFragment<StationWithVolume> {

    private org.juanro.autumandu.viewmodel.StationListViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(org.juanro.autumandu.viewmodel.StationListViewModel.class);
    }

    @Override
    protected LiveData<List<StationWithVolume>> getLiveData() {
        if (carId > 0) {
            return viewModel.getStationsWithVolume(carId);
        } else {
            return viewModel.getStationsWithVolume();
        }
    }

    @Override
    protected int getAlertDeleteManyMessage() {
        return R.string.alert_delete_station_message;
    }

    @Override
    protected int getExtraEdit() {
        return DataDetailActivity.EXTRA_EDIT_STATION;
    }

    @Override
    protected SparseArray<String> getItemData(StationWithVolume stationWithVolume) {
        var data = new SparseArray<String>(7);
        data.put(R.id.title, stationWithVolume.station().getName());
        data.put(R.id.data1, formatVolumesByCategory(stationWithVolume.volumesByCategory()));

        return data;
    }

    private String formatVolumesByCategory(Map<String, Double> volumesByCategory) {
        List<String> formatted = new ArrayList<>();
        for (Map.Entry<String, Double> entry : volumesByCategory.entrySet()) {
            if (entry.getValue() > 0) {
                FuelCategory category = FuelCategory.fromKey(entry.getKey());
                String unit = category.getVolumeUnit(requireContext());
                formatted.add(String.format(Locale.getDefault(), "%.2f %s", entry.getValue(), unit));
            }
        }
        return String.join(", ", formatted);
    }

    @Override
    protected boolean isMissingData(StationWithVolume item) {
        return false;
    }

    @Override
    protected long getItemId(StationWithVolume item) {
        return item.station().getId();
    }

    @Override
    protected void deleteItem(long id) {
        viewModel.delete(id);
    }
}
