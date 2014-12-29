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

package me.kuehle.carreport.gui;

import android.text.format.DateFormat;
import android.util.SparseArray;

import java.util.Collections;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.util.RecurrenceInterval;

public class DataListOtherFragment extends AbstractDataListFragment<OtherCost> {
    @Override
    protected int getAlertDeleteManyMessage() {
        return R.string.alert_delete_others_message;
    }

    @Override
    protected int getExtraEdit() {
        return DataDetailActivity.EXTRA_EDIT_OTHER;
    }

    @Override
    protected SparseArray<String> getItemData(List<OtherCost> otherCosts, int position) {
        Preferences prefs = new Preferences(getActivity());
        java.text.DateFormat dateFmt = DateFormat.getDateFormat(getActivity());
        String[] repIntervals = getResources().getStringArray(
                R.array.repeat_intervals);
        OtherCost other = otherCosts.get(position);

        SparseArray<String> data = new SparseArray<>();
        data.put(R.id.title, other.title);
        data.put(R.id.date, dateFmt.format(other.date));
        if (other.mileage > -1) {
            data.put(R.id.data1, String.format("%d %s", other.mileage, prefs.getUnitDistance()));
        }
        data.put(R.id.data2, String.format("%.2f %s", other.price, prefs.getUnitCurrency()));
        data.put(R.id.data3, repIntervals[other.recurrence.getInterval().getValue()]);
        if (!other.recurrence.getInterval().equals(RecurrenceInterval.ONCE)) {
            int recurrences;
            if (other.endDate == null) {
                recurrences = other.recurrence.getRecurrencesSince(other.date);
            } else {
                recurrences = other.recurrence.getRecurrencesBetween(other.date, other.endDate);
            }

            data.put(R.id.data2_calculated, String.format("%.2f %s", other.price * recurrences,
                    prefs.getUnitCurrency()));
            data.put(R.id.data3_calculated, String.format("x%d", recurrences));
        }

        return data;
    }

    @Override
    protected List<OtherCost> getItems() {
        List<OtherCost> otherCosts = mCar.getOtherCosts();
        Collections.reverse(otherCosts);
        return otherCosts;
    }

    @Override
    protected boolean isMissingData(List<OtherCost> otherCosts, int position) {
        return false;
    }

    @Override
    protected boolean isInvalidData(List<OtherCost> otherCosts, int position) {
        return false;
    }
}