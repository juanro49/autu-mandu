/*
 * Copyright 2025 Juanro49
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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import android.text.format.DateFormat;
import android.util.SparseArray;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.model.dto.TireWithDetails;
import org.juanro.autumandu.gui.DataDetailActivity;

import java.util.List;
import java.util.Locale;

public class DataListTireFragment extends AbstractDataListFragment<TireWithDetails> {

    private java.text.DateFormat dateFormat;
    private String unitDistance;
    private String unitCurrency;

    private org.juanro.autumandu.viewmodel.TireListViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dateFormat = DateFormat.getDateFormat(requireActivity());

        var prefs = new Preferences(requireActivity());
        unitDistance = prefs.getUnitDistance();
        unitCurrency = prefs.getUnitCurrency();

        viewModel = new ViewModelProvider(this).get(org.juanro.autumandu.viewmodel.TireListViewModel.class);
        viewModel.setCarId(carId);
    }

    @Override
    protected LiveData<List<TireWithDetails>> getLiveData() {
        return viewModel.getTires();
    }

    @Override
    protected int getAlertDeleteManyMessage() {
        return R.string.alert_delete_tire_message;
    }

    @Override
    protected int getExtraEdit() {
        return DataDetailActivity.EXTRA_EDIT_TIRE;
    }

    @Override
    protected SparseArray<String> getItemData(TireWithDetails tireWithDetails) {
        var tire = tireWithDetails.tire();

        var data = new SparseArray<String>(7);

        data.put(R.id.title, tire.getManufacturer() + " " + tire.getModel());
        data.put(R.id.date, dateFormat.format(tire.getBuyDate()));
        data.put(R.id.data1, String.format(Locale.getDefault(), "%d %s", tireWithDetails.distance(), unitDistance));
        data.put(R.id.data2, String.format(Locale.getDefault(), "%.2f %s", tire.getPrice(), unitCurrency));
        data.put(R.id.data3_calculated, String.format(Locale.getDefault(), "x%d", tire.getQuantity()));

        if (tireWithDetails.isMounted()) {
            data.put(R.id.data3, getString(R.string.tire_state_mounted));
        } else if (tire.getTrashDate() != null) {
            data.put(R.id.data3, getString(R.string.tire_state_trashed));
        }

        return data;
    }

    @Override
    protected boolean isMissingData(TireWithDetails tire) {
        return false;
    }

    @Override
    protected long getItemId(TireWithDetails item) {
        var id = item.tire().getId();
        return id != null ? id : 0L;
    }

    @Override
    protected void deleteItem(long id) {
        viewModel.delete(id);
    }
}
