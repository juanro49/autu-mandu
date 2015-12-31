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

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.util.SparseArray;

import org.joda.time.DateTime;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.provider.othercost.OtherCostColumns;
import me.kuehle.carreport.provider.othercost.OtherCostCursor;
import me.kuehle.carreport.provider.othercost.OtherCostSelection;
import me.kuehle.carreport.provider.othercost.RecurrenceInterval;
import me.kuehle.carreport.util.Recurrences;

public class DataListOtherFragment extends AbstractDataListFragment {
    public static final String EXTRA_OTHER_TYPE = "other_type";
    public static final int EXTRA_OTHER_TYPE_EXPENDITURE = 0;
    public static final int EXTRA_OTHER_TYPE_INCOME = 1;

    private boolean mIsExpenditure;
    private java.text.DateFormat mDateFormat;
    private String[] mRepeatIntervals;
    private String mUnitDistance;
    private String mUnitCurrency;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsExpenditure = getArguments().getInt(EXTRA_OTHER_TYPE, EXTRA_OTHER_TYPE_EXPENDITURE) ==
                EXTRA_OTHER_TYPE_EXPENDITURE;
        mDateFormat = DateFormat.getDateFormat(getActivity());
        mRepeatIntervals = getResources().getStringArray(R.array.repeat_intervals);

        Preferences prefs = new Preferences(getActivity());
        mUnitDistance = prefs.getUnitDistance();
        mUnitCurrency = prefs.getUnitCurrency();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        OtherCostSelection where = new OtherCostSelection().carId(mCarId);
        if (mIsExpenditure) {
            where.and().priceGt(0);
        } else {
            where.and().priceLt(0);
        }

        return new CursorLoader(getActivity(), where.uri(), null, where.sel(), where.args(),
                OtherCostColumns.DATE + " DESC");
    }

    @Override
    protected int getAlertDeleteManyMessage() {
        if (mIsExpenditure) {
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
    protected SparseArray<String> getItemData(Cursor cursor) {
        OtherCostCursor otherCost = new OtherCostCursor(cursor);

        SparseArray<String> data = new SparseArray<>(7);
        data.put(R.id.title, otherCost.getTitle());
        data.put(R.id.date, mDateFormat.format(otherCost.getDate()));
        if (otherCost.getMileage() != null && otherCost.getMileage() > -1) {
            data.put(R.id.data1, String.format("%d %s", otherCost.getMileage(), mUnitDistance));
        }

        float price = mIsExpenditure ? otherCost.getPrice() : -otherCost.getPrice();
        data.put(R.id.data2, String.format("%.2f %s", price, mUnitCurrency));

        data.put(R.id.data3, mRepeatIntervals[otherCost.getRecurrenceInterval().ordinal()]);
        if (!otherCost.getRecurrenceInterval().equals(RecurrenceInterval.ONCE)) {
            int recurrences;
            if (otherCost.getEndDate() == null || new DateTime(otherCost.getEndDate()).isAfterNow()) {
                recurrences = Recurrences.getRecurrencesSince(otherCost.getRecurrenceInterval(),
                        otherCost.getRecurrenceMultiplier(), otherCost.getDate());
            } else {
                recurrences = Recurrences.getRecurrencesBetween(otherCost.getRecurrenceInterval(),
                        otherCost.getRecurrenceMultiplier(), otherCost.getDate(), otherCost.getEndDate());
            }

            data.put(R.id.data2_calculated, String.format("%.2f %s", otherCost.getPrice() * recurrences,
                    mUnitCurrency));
            data.put(R.id.data3_calculated, String.format("x%d", recurrences));
        }

        return data;
    }

    @Override
    protected boolean isMissingData(Cursor cursor) {
        return false;
    }

    @Override
    protected void deleteItem(long id) {
        new OtherCostSelection().id(id).delete(getActivity().getContentResolver());
    }
}