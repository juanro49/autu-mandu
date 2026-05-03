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

package org.juanro.autumandu.gui.dialog;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import android.view.View;

import java.util.Set;
import java.util.TreeSet;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.util.AbstractFormFieldValidator;
import org.juanro.autumandu.gui.util.FormFieldNotEmptyValidator;
import org.juanro.autumandu.gui.util.FormValidator;
import org.juanro.autumandu.model.dto.StationWithVolume;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.viewmodel.StationsViewModel;

public class EditStationDialogFragment extends AbstractEditDialogFragment {
    public static final String REQUEST_KEY = "org.juanro.autumandu.EDIT_STATION_REQUEST";

    public static EditStationDialogFragment newInstance(int requestCode, long stationId) {
        EditStationDialogFragment f = new EditStationDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, stationId);
        args.putInt(ARG_REQUEST_CODE, requestCode);
        f.setArguments(args);
        return f;
    }

    private Set<String> mOtherStationNames;
    private Station mStation;
    private StationsViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long currentStationId = getArguments() != null ? getArguments().getLong(ARG_ID, 0) : 0;
        mOtherStationNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        mViewModel = new ViewModelProvider(this).get(StationsViewModel.class);
        mViewModel.getStations().observe(this, stations -> {
            mOtherStationNames.clear();
            for (StationWithVolume stationWithVolume : stations) {
                Station station = stationWithVolume.station();
                if (currentStationId == station.getId()) {
                    mStation = station;
                    if (mEdtName != null && mEdtName.getText().length() == 0) {
                        mEdtName.setText(mStation.getName());
                    }
                } else {
                    mOtherStationNames.add(station.getName());
                }
            }
        });
    }

    @Override
    protected int getLayout() { return R.layout.dialog_station; }

    @Override
    protected int getAddTitle() { return R.string.title_add_station; }

    @Override
    protected int getEditTitle() { return R.string.title_edit_station; }

    @Override
    protected void initFields(View view, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mEdtName.setText(savedInstanceState.getString("name"));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mEdtName != null) {
            outState.putString("name", mEdtName.getText().toString());
        }
    }

    @Override
    protected String getRequestKey() { return REQUEST_KEY; }

    @Override
    protected boolean save() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldNotEmptyValidator(mEdtName));
        validator.add(new AbstractFormFieldValidator(mEdtName) {
            @Override
            protected boolean isValid() {
                return !mOtherStationNames.contains(mEdtName.getText().toString());
            }

            @Override
            protected int getMessage() {
                return R.string.validate_error_station_exists;
            }
        });

        if (validator.validate()) {
            if (mStation == null) {
                mViewModel.saveStation(new Station(mEdtName.getText().toString()));
            } else {
                mStation.setName(mEdtName.getText().toString());
                mViewModel.saveStation(mStation);
            }
            return true;
        }
        return false;
    }
}
