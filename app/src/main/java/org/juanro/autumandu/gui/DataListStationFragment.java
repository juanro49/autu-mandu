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

package org.juanro.autumandu.gui;

import android.database.Cursor;
import android.os.Bundle;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.text.format.DateFormat;
import android.util.SparseArray;

import java.util.Locale;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.presentation.StationPresenter;
import org.juanro.autumandu.provider.DataProvider;
import org.juanro.autumandu.provider.station.StationColumns;
import org.juanro.autumandu.provider.station.StationCursor;
import org.juanro.autumandu.provider.station.StationSelection;

public class DataListStationFragment extends AbstractDataListFragment {

    private java.text.DateFormat mDateFormat;
    private String mUnitVolume;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDateFormat = DateFormat.getDateFormat(getActivity());

        Preferences prefs = new Preferences(getActivity());
        mUnitVolume = prefs.getUnitVolume();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        StationSelection where = new StationSelection();

        return new CursorLoader(getActivity(), DataProvider.groupBy(where.uri(), StationColumns.NAME), null, where.sel(), where.args(),
            "sum(" + StationColumns.VOLUME + ") DESC");
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
    protected SparseArray<String> getItemData(Cursor cursor) {
        StationCursor station = new StationCursor(cursor);
        StationPresenter mStation = StationPresenter.getInstance(getActivity());

        SparseArray<String> data = new SparseArray<>(7);
        data.put(R.id.title, station.getName());
        data.put(R.id.data1, String.format(Locale.getDefault(), "%.2f %s", mStation.getVolumeForStation(station.getId()), mUnitVolume));

        return data;
    }

    @Override
    protected boolean isMissingData(Cursor cursor) {
        return false;
    }

    @Override
    protected void deleteItem(long id) {
            new StationSelection().id(id).delete(getActivity().getContentResolver());
    }
}
