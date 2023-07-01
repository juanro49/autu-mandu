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

package org.juanro.autumandu.gui;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.text.format.DateFormat;
import android.util.SparseArray;

import java.util.Locale;

import org.juanro.autumandu.FuelConsumption;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.data.balancing.BalancedRefuelingCursor;
import org.juanro.autumandu.data.balancing.RefuelingBalancer;
import org.juanro.autumandu.provider.refueling.RefuelingSelection;

public class DataListRefuelingFragment extends AbstractDataListFragment {
    public static class RefuelingLoader extends CursorLoader {
        private final ForceLoadContentObserver mObserver;
        private final long mCarId;

        public RefuelingLoader(Context context, long carId) {
            super(context);
            mObserver = new ForceLoadContentObserver();
            mCarId = carId;
        }

        @Override
        public Cursor loadInBackground() {
            RefuelingBalancer balancer = new RefuelingBalancer(getContext());
            Cursor cursor = balancer.getBalancedRefuelings(mCarId, true);
            if (cursor != null) {
                cursor.getCount();
                cursor.registerContentObserver(mObserver);
            }

            return cursor;
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
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new RefuelingLoader(getActivity(), mCarId);
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
    protected SparseArray<String> getItemData(Cursor cursor) {
        BalancedRefuelingCursor refueling = (BalancedRefuelingCursor) cursor;
        int position = refueling.getPosition();
        int mileage = refueling.getMileage();
        float volume = refueling.getVolume();

        if (refueling.getGuessed()) {
            SparseArray<String> data = new SparseArray<>(1);
            data.put(R.id.title, getString(R.string.missing_refueling));
            return data;
        }

        SparseArray<String> data = new SparseArray<>(10);

        data.put(R.id.title, getString(R.string.edit_title_refueling));
        data.put(R.id.subtitle, refueling.getFuelTypeName());
        data.put(R.id.date, mDateFormat.format(refueling.getDate()));

        data.put(R.id.data1, String.format(Locale.getDefault(), "%d %s", mileage, mUnitDistance));

        int mileageDifference = mileage - refueling.getCarInitialMileage();
        if (moveToNextNotGuessedRefueling(refueling)) {
            mileageDifference = mileage - refueling.getMileage();
        }
        refueling.moveToPosition(position);
        data.put(R.id.data1_calculated, String.format(Locale.getDefault(), "+ %d %s",
                mileageDifference, mUnitDistance));

        if (refueling.getPrice() != 0.0f) {
            data.put(R.id.data2, String.format(Locale.getDefault(), "%.2f %s", refueling.getPrice(),
                    mUnitCurrency));
            data.put(R.id.data2_calculated, String.format(Locale.getDefault(), "%.3f %s/%s",
                    refueling.getPrice() / volume, mUnitCurrency, mUnitVolume));
        } else {
            data.put(R.id.data2, getString(R.string.notice_not_paid));
        }

        data.put(R.id.data3, String.format(Locale.getDefault(), "%.2f %s", volume, mUnitVolume));
        if (refueling.getPartial()) {
            data.put(R.id.data3_calculated, getString(R.string.label_partial));
        } else if (moveToNextNotGuessedRefuelingOfSameFuelTypeCategory(refueling)) {
            float diffVolume = volume;
            do {
                if (refueling.getPartial()) {
                    diffVolume += refueling.getVolume();
                } else {
                    int diffMileage = mileage - refueling.getMileage();
                    data.put(R.id.data3_calculated, String.format(Locale.getDefault(), "%.2f %s",
                            mFuelConsumption.computeFuelConsumption(diffVolume, diffMileage),
                            mFuelConsumption.getUnitLabel()));
                    break;
                }
            } while (moveToNextNotGuessedRefuelingOfSameFuelTypeCategory(refueling));
        }
        refueling.moveToPosition(position);

        data.put(R.id.data_invalid, refueling.getValid() ? "false" : "true");

        return data;
    }

    @Override
    protected boolean isMissingData(Cursor cursor) {
        BalancedRefuelingCursor refueling = (BalancedRefuelingCursor) cursor;

        return refueling.getGuessed();
    }

    @Override
    protected void deleteItem(long id) {
        new RefuelingSelection().id(id).delete(getActivity().getContentResolver());
    }

    private boolean moveToNextNotGuessedRefueling(BalancedRefuelingCursor refueling) {
        while (refueling.moveToNext()) {
            if (!refueling.getGuessed()) {
                return true;
            }
        }

        return false;
    }

    private boolean moveToNextNotGuessedRefuelingOfSameFuelTypeCategory(BalancedRefuelingCursor refueling) {
        String fuelTypeCategory = refueling.getFuelTypeCategory();
        while (moveToNextNotGuessedRefueling(refueling)) {
            if ((fuelTypeCategory == null &&
                    refueling.getFuelTypeCategory() == null) ||
                    (fuelTypeCategory != null &&
                            fuelTypeCategory.equals(refueling.getFuelTypeCategory()))) {
                return true;
            }
        }

        return false;
    }
}
