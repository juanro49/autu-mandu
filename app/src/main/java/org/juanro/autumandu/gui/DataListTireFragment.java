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

package org.juanro.autumandu.gui;

import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.SparseArray;

import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.presentation.TirePresenter;
import org.juanro.autumandu.provider.tirelist.TireListColumns;
import org.juanro.autumandu.provider.tirelist.TireListCursor;
import org.juanro.autumandu.provider.tirelist.TireListSelection;

import java.util.Locale;

public class DataListTireFragment extends AbstractDataListFragment {

    private java.text.DateFormat mDateFormat;
    private String mUnitDistance;
    private String mUnitCurrency;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDateFormat = DateFormat.getDateFormat(getActivity());

        Preferences prefs = new Preferences(getActivity());
        mUnitDistance = prefs.getUnitDistance();
        mUnitCurrency = prefs.getUnitCurrency();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        TireListSelection where = new TireListSelection().carId(mCarId);

        return new CursorLoader(getActivity(), where.uri(), null, where.sel(), where.args(),
            TireListColumns.BUY_DATE + " DESC");
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
    protected SparseArray<String> getItemData(Cursor cursor) {
        TireListCursor tireList = new TireListCursor(cursor);
        TirePresenter mTire = TirePresenter.getInstance(getActivity());
        SparseArray<String> data = new SparseArray<>(7);

        data.put(R.id.title, tireList.getManufacturer() + " " + tireList.getModel());
        data.put(R.id.date, mDateFormat.format(tireList.getBuyDate()));
        data.put(R.id.data1, String.format(Locale.getDefault(), "%d %s", mTire.getTireDistance(tireList.getId()), mUnitDistance));
        data.put(R.id.data2, String.format(Locale.getDefault(), "%.2f %s", tireList.getPrice(), mUnitCurrency));
        data.put(R.id.data3_calculated, String.format(Locale.getDefault(), "x%d", tireList.getQuantity()));

        int state = mTire.getState(tireList.getId());

        if (state == 1) // 1: mounted, 2: trashed
        {
            data.put(R.id.data3, getString(R.string.tire_state_mounted));
        }
        else if (state == 2)
        {
            data.put(R.id.data3, getString(R.string.tire_state_trashed));
        }

        return data;
    }

    @Override
    protected boolean isMissingData(Cursor cursor) {
        return false;
    }

    @Override
    protected void deleteItem(long id) {
        new TireListSelection().id(id).delete(getActivity().getContentResolver());
    }
}
