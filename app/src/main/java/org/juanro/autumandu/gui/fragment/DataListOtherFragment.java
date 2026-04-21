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

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.DataDetailActivity;
import org.juanro.autumandu.model.entity.OtherCost;
import org.juanro.autumandu.model.entity.helper.RecurrenceInterval;
import org.juanro.autumandu.util.Recurrences;

public class DataListOtherFragment extends AbstractDataListFragment<OtherCost> {
    public static final String EXTRA_OTHER_TYPE = "other_type";
    public static final int EXTRA_OTHER_TYPE_EXPENDITURE = 0;
    public static final int EXTRA_OTHER_TYPE_INCOME = 1;

    private boolean isExpenditure;
    private java.text.DateFormat dateFormat;
    private String[] repeatIntervals;
    private String unitDistance;
    private String unitCurrency;

    private org.juanro.autumandu.viewmodel.OtherListViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var args = getArguments();
        isExpenditure = args == null || args.getInt(EXTRA_OTHER_TYPE, EXTRA_OTHER_TYPE_EXPENDITURE) ==
                EXTRA_OTHER_TYPE_EXPENDITURE;
        dateFormat = DateFormat.getDateFormat(requireActivity());
        repeatIntervals = getResources().getStringArray(R.array.repeat_intervals);

        var prefs = new Preferences(requireActivity());
        unitDistance = prefs.getUnitDistance();
        unitCurrency = prefs.getUnitCurrency();

        viewModel = new ViewModelProvider(this).get(org.juanro.autumandu.viewmodel.OtherListViewModel.class);
        viewModel.setCarId(carId);
        viewModel.setExpenditure(isExpenditure);
    }

    @Override
    protected LiveData<List<OtherCost>> getLiveData() {
        return viewModel.getOtherCosts();
    }

    @Override
    protected int getAlertDeleteManyMessage() {
        if (isExpenditure) {
            return R.string.alert_delete_other_expenditures_message;
        } else {
            return R.string.alert_delete_other_incomes_message;
        }
    }

    @Override
    protected int getExtraEdit() {
        return DataDetailActivity.EXTRA_EDIT_OTHER;
    }

    @Override
    protected SparseArray<String> getItemData(OtherCost otherCost) {
        var data = new SparseArray<String>(7);
        data.put(R.id.title, otherCost.getTitle());
        data.put(R.id.date, dateFormat.format(otherCost.getDate()));
        if (otherCost.getMileage() != null && otherCost.getMileage() > -1) {
            data.put(R.id.data1, String.format(Locale.getDefault(), "%d %s", otherCost.getMileage(),
                    unitDistance));
        }

        float price = isExpenditure ? otherCost.getPrice() : -otherCost.getPrice();
        data.put(R.id.data2, String.format(Locale.getDefault(), "%.2f %s", price, unitCurrency));

        data.put(R.id.data3, repeatIntervals[otherCost.getRecurrenceInterval().ordinal()]);
        if (!otherCost.getRecurrenceInterval().equals(RecurrenceInterval.ONCE)) {
            int recurrences;
            if (otherCost.getEndDate() == null || otherCost.getEndDate().after(new Date())) {
                recurrences = Recurrences.getRecurrencesSince(otherCost.getRecurrenceInterval(),
                        otherCost.getRecurrenceMultiplier(), otherCost.getDate());
            } else {
                recurrences = Recurrences.getRecurrencesBetween(otherCost.getRecurrenceInterval(),
                        otherCost.getRecurrenceMultiplier(), otherCost.getDate(), otherCost.getEndDate());
            }

            data.put(R.id.data2_calculated, String.format(Locale.getDefault(), "%.2f %s",
                    otherCost.getPrice() * recurrences, unitCurrency));
            data.put(R.id.data3_calculated, String.format(Locale.getDefault(), "x%d", recurrences));
        }

        return data;
    }

    @Override
    protected long getItemId(OtherCost item) {
        return item.getId();
    }

    @Override
    protected boolean isMissingData(OtherCost otherCost) {
        return false;
    }

    @Override
    protected void deleteItem(long id) {
        viewModel.delete(id);
    }
}
