/*
 * Copyright 2014 Jan Kühle
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

import java.util.List;
import java.util.Locale;

import org.juanro.autumandu.FuelConsumption;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.DataDetailActivity;
import org.juanro.autumandu.model.dto.BalancedRefueling;

public class DataListRefuelingFragment extends AbstractDataListFragment<BalancedRefueling> {

    private java.text.DateFormat dateFormat;
    private FuelConsumption fuelConsumption;
    private String unitDistance;
    private String unitCurrency;
    private String unitVolume;

    private org.juanro.autumandu.viewmodel.RefuelingListViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var context = requireContext();
        dateFormat = DateFormat.getDateFormat(context);
        fuelConsumption = new FuelConsumption(context);

        var prefs = new Preferences(context);
        unitDistance = prefs.getUnitDistance();
        unitCurrency = prefs.getUnitCurrency();
        unitVolume = prefs.getUnitVolume();

        viewModel = new ViewModelProvider(this).get(org.juanro.autumandu.viewmodel.RefuelingListViewModel.class);
        viewModel.setCarId(carId);
    }

    @Override
    protected LiveData<List<BalancedRefueling>> getLiveData() {
        return viewModel.getBalancedRefuelings();
    }

    @Override
    protected int getAlertDeleteManyMessage() {
        return R.string.alert_delete_refuelings_message;
    }

    @Override
    protected int getExtraEdit() {
        return DataDetailActivity.EXTRA_EDIT_REFUELING;
    }

    @Override
    protected SparseArray<String> getItemData(BalancedRefueling refueling) {
        var mileage = refueling.getMileage();
        var volume = refueling.getVolume();

        if (refueling.isGuessed()) {
            var data = new SparseArray<String>(1);
            data.put(R.id.title, getString(R.string.missing_refueling));
            return data;
        }

        var data = new SparseArray<String>(10);

        data.put(R.id.title, getString(R.string.edit_title_refueling));
        data.put(R.id.subtitle, refueling.getFuelTypeName());
        data.put(R.id.date, dateFormat.format(refueling.getDate()));

        data.put(R.id.data1, String.format(Locale.getDefault(), "%d %s", mileage, unitDistance));

        if (refueling.getMileageDifference() != null) {
            data.put(R.id.data1_calculated, String.format(Locale.getDefault(), "+%d %s",
                    refueling.getMileageDifference(), unitDistance));
        }

        if (refueling.getConsumption() != null) {
            data.put(R.id.data3_calculated, String.format(Locale.getDefault(), "%.2f %s",
                    refueling.getConsumption(), fuelConsumption.getUnitLabel()));
        }

        if (refueling.getPrice() != 0.0f) {
            data.put(R.id.data2, String.format(Locale.getDefault(), "%.2f %s", refueling.getPrice(),
                    unitCurrency));
            data.put(R.id.data2_calculated, String.format(Locale.getDefault(), "%.3f %s/%s",
                    refueling.getPrice() / volume, unitCurrency, unitVolume));
        } else {
            data.put(R.id.data2, getString(R.string.notice_not_paid));
        }

        data.put(R.id.data3, String.format(Locale.getDefault(), "%.2f %s", volume, unitVolume));
        if (refueling.isPartial()) {
            var label = getString(R.string.label_partial);
            var consumption = data.get(R.id.data3_calculated);
            if (consumption != null) {
                label += " / " + consumption;
            }
            data.put(R.id.data3_calculated, label);
        }

        data.put(R.id.data_invalid, refueling.isValid() ? "false" : "true");

        return data;
    }

    @Override
    protected boolean isMissingData(BalancedRefueling refueling) {
        return refueling.isGuessed();
    }

    @Override
    protected long getItemId(BalancedRefueling item) {
        return item.getId();
    }

    @Override
    protected void deleteItem(long id) {
        viewModel.delete(id);
    }
}
