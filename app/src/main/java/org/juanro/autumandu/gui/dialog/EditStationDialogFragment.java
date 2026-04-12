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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Set;
import java.util.TreeSet;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.util.AbstractFormFieldValidator;
import org.juanro.autumandu.gui.util.FormFieldNotEmptyValidator;
import org.juanro.autumandu.gui.util.FormValidator;
import org.juanro.autumandu.model.dto.StationWithVolume;
import org.juanro.autumandu.model.entity.Station;
import org.juanro.autumandu.viewmodel.StationsViewModel;

public class EditStationDialogFragment extends DialogFragment {
    public static final String REQUEST_KEY = "org.juanro.autumandu.EDIT_STATION_REQUEST";
    public static final String RESULT_ACTION = "action";
    public static final String RESULT_REQUEST_CODE = "request_code";

    public static final int ACTION_POSITIVE = 1;
    public static final int ACTION_NEGATIVE = 2;

    private static final String ARG_STATION_ID = "station_id";
    private static final String ARG_REQUEST_CODE = "request_code";

    public static EditStationDialogFragment newInstance(int requestCode, long stationId) {
        EditStationDialogFragment f = new EditStationDialogFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_STATION_ID, stationId);
        args.putInt(ARG_REQUEST_CODE, requestCode);
        f.setArguments(args);

        return f;
    }

    private Set<String> mOtherStationNames;
    private Station mStation;
    private EditText mEdtName;
    private StationsViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long currentStationId = getArguments() != null ? getArguments().getLong(ARG_STATION_ID, 0) : 0;
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

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(requireActivity());
        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.dialog_station, null);
        dialog.setContentView(view);

        mEdtName = view.findViewById(R.id.edt_name);

        long currentStationId = getArguments() != null ? getArguments().getLong(ARG_STATION_ID, 0) : 0;
        dialog.setTitle(currentStationId == 0
                ? R.string.title_add_station
                : R.string.title_edit_station);

        if (savedInstanceState != null) {
            mEdtName.setText(savedInstanceState.getString("name"));
        }

        Button btnOk = view.findViewById(R.id.btn_ok);
        btnOk.setOnClickListener(v -> {
            if (save()) {
                sendResult(ACTION_POSITIVE);
                dialog.dismiss();
            }
        });

        Button btnCancel = view.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> {
            sendResult(ACTION_NEGATIVE);
            dialog.dismiss();
        });

        return dialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mEdtName != null) {
            outState.putString("name", mEdtName.getText().toString());
        }
    }

    private void sendResult(int action) {
        Bundle result = new Bundle();
        result.putInt(RESULT_ACTION, action);
        result.putInt(RESULT_REQUEST_CODE, getArguments() != null ? getArguments().getInt(ARG_REQUEST_CODE) : 0);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }

    private boolean save() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldNotEmptyValidator(mEdtName));
        validator.add(new AbstractFormFieldValidator(mEdtName) {
            @Override
            protected boolean isValid() {
                String name = mEdtName.getText().toString();
                return !mOtherStationNames.contains(name);
            }

            @Override
            protected int getMessage() {
                return R.string.validate_error_station_exists;
            }
        });

        if (validator.validate()) {
            if (mStation == null) {
                Station station = new Station(mEdtName.getText().toString());
                mViewModel.saveStation(station);
            } else {
                mStation.setName(mEdtName.getText().toString());
                mViewModel.saveStation(mStation);
            }

            return true;
        } else {
            return false;
        }
    }
}
