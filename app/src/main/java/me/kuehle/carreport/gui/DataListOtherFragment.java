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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.util.SparseArray;

import java.util.Collections;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.util.RecurrenceInterval;

public class DataListOtherFragment extends AbstractDataListFragment<OtherCost> {
    public static class OtherCostLoader extends AsyncTaskLoader<List<OtherCost>> {
        private Car mCar;
        private boolean mIsExpenditure;

        public OtherCostLoader(Context context, Car car, boolean isExpenditure) {
            super(context);
            mCar = car;
            mIsExpenditure = isExpenditure;
        }

        @Override
        public List<OtherCost> loadInBackground() {
            List<OtherCost> otherCosts = mIsExpenditure ?
                    mCar.getOtherExpenditures() :
                    mCar.getOtherIncomes();
            Collections.reverse(otherCosts);
            return otherCosts;
        }


        @Override
        protected void onStartLoading() {
            forceLoad();
        }
    }

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
    public Loader<List<OtherCost>> onCreateLoader(int id, Bundle args) {
        return new OtherCostLoader(getActivity(), mCar, mIsExpenditure);
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
    protected SparseArray<String> getItemData(List<OtherCost> otherCosts, int position) {
        OtherCost other = otherCosts.get(position);

        SparseArray<String> data = new SparseArray<>(7);
        data.put(R.id.title, other.title);
        data.put(R.id.date, mDateFormat.format(other.date));
        if (other.mileage > -1) {
            data.put(R.id.data1, String.format("%d %s", other.mileage, mUnitDistance));
        }

        float price = mIsExpenditure ? other.price : -other.price;
        data.put(R.id.data2, String.format("%.2f %s", price, mUnitCurrency));

        data.put(R.id.data3, mRepeatIntervals[other.recurrence.getInterval().getValue()]);
        if (!other.recurrence.getInterval().equals(RecurrenceInterval.ONCE)) {
            int recurrences;
            if (other.endDate == null) {
                recurrences = other.recurrence.getRecurrencesSince(other.date);
            } else {
                recurrences = other.recurrence.getRecurrencesBetween(other.date, other.endDate);
            }

            data.put(R.id.data2_calculated, String.format("%.2f %s", other.price * recurrences,
                    mUnitCurrency));
            data.put(R.id.data3_calculated, String.format("x%d", recurrences));
        }

        return data;
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