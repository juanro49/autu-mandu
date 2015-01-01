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

import me.kuehle.carreport.FuelConsumption;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.balancing.RefuelingBalancer;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Refueling;

public class DataListRefuelingFragment extends AbstractDataListFragment<Refueling> {
    public static class RefuelingLoader extends AsyncTaskLoader<List<Refueling>> {
        private Car mCar;

        public RefuelingLoader(Context context, Car car) {
            super(context);
            mCar = car;
        }

        @Override
        public List<Refueling> loadInBackground() {
            RefuelingBalancer balancer = new RefuelingBalancer(getContext());
            List<Refueling> refuelings = balancer.getBalancedRefuelings(mCar);
            Collections.reverse(refuelings);
            return refuelings;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }
    }

    private java.text.DateFormat mDateFormat;
    private FuelConsumption mFuelConsumption;
    private String mUnitDistance;
    private String mUnitCurrency;
    private String mUnitVolume;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDateFormat = DateFormat.getDateFormat(getActivity());
        mFuelConsumption = new FuelConsumption(getActivity());

        Preferences prefs = new Preferences(getActivity());
        mUnitDistance = prefs.getUnitDistance();
        mUnitCurrency = prefs.getUnitCurrency();
        mUnitVolume = prefs.getUnitVolume();
    }

    @Override
    public Loader<List<Refueling>> onCreateLoader(int id, Bundle args) {
        return new RefuelingLoader(getActivity(), mCar);
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
    protected SparseArray<String> getItemData(List<Refueling> refuelings, int position) {
        Refueling refueling = refuelings.get(position);

        if (refueling.guessed) {
            SparseArray<String> data = new SparseArray<>(1);
            data.put(R.id.title, getString(R.string.missing_refueling));
            return data;
        }

        SparseArray<String> data = new SparseArray<>(10);

        data.put(R.id.title, getString(R.string.edit_title_refueling));
        data.put(R.id.subtitle, refueling.fuelType.name);
        data.put(R.id.date, mDateFormat.format(refueling.date));

        data.put(R.id.data1, String.format("%d %s", refueling.mileage, mUnitDistance));
        Refueling nextRefueling = getNextRefueling(refuelings, position);
        if (nextRefueling != null) {
            data.put(R.id.data1_calculated, String.format("+ %d %s",
                    refueling.mileage - nextRefueling.mileage, mUnitDistance));
        }

        data.put(R.id.data2, String.format("%.2f %s", refueling.price, mUnitCurrency));
        data.put(R.id.data2_calculated, String.format("%.3f %s/%s",
                refueling.price / refueling.volume, mUnitCurrency,
                mUnitVolume));

        data.put(R.id.data3, String.format("%.2f %s", refueling.volume, mUnitVolume));
        if (refueling.partial) {
            data.put(R.id.data3_calculated, getString(R.string.label_partial));
        } else if (nextRefueling != null) {
            float diffVolume = refueling.volume;
            for (int i = position + 1; nextRefueling != null; i++) {
                if (nextRefueling.partial) {
                    diffVolume += nextRefueling.volume;
                } else {
                    int diffMileage = refueling.mileage - nextRefueling.mileage;
                    data.put(R.id.data3_calculated, String.format("%.2f %s",
                            mFuelConsumption.computeFuelConsumption(diffVolume, diffMileage),
                            mFuelConsumption.getUnitLabel()));
                    break;
                }

                nextRefueling = getNextRefueling(refuelings, i);
            }
        }

        return data;
    }

    @Override
    protected boolean isMissingData(List<Refueling> refuelings, int position) {
        return refuelings.get(position).guessed;
    }

    @Override
    protected boolean isInvalidData(List<Refueling> refuelings, int position) {
        return !refuelings.get(position).valid;
    }

    private Refueling getNextRefueling(List<Refueling> refuelings, int position) {
        while (++position < refuelings.size()) {
            if (!refuelings.get(position).guessed) {
                return refuelings.get(position);
            }
        }

        return null;
    }
}