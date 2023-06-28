/*
 * Copyright 2013 Jan Kühle
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

package me.kuehle.carreport.gui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;

import android.app.DialogFragment;
import android.app.Fragment;

import android.widget.EditText;

import java.util.Set;
import java.util.TreeSet;

import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.util.AbstractFormFieldValidator;
import me.kuehle.carreport.gui.util.FormFieldNotEmptyValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.provider.station.StationContentValues;
import me.kuehle.carreport.provider.station.StationCursor;
import me.kuehle.carreport.provider.station.StationSelection;

public class EditStationDialogFragment extends DialogFragment {

    public interface EditStationDialogFragmentListener {
        void onDialogNegativeClick(int requestCode);

        void onDialogPositiveClick(int requestCode);
    }

    public static EditStationDialogFragment newInstance(Fragment parent, int requestCode, long stationId) {
        EditStationDialogFragment f = new EditStationDialogFragment();
        f.setTargetFragment(parent, requestCode);

        Bundle args = new Bundle();
        args.putLong("station_id", stationId);
        f.setArguments(args);

        return f;
    }

    private Set<String> mOtherStationNames;
    private StationCursor mStation;
    private EditText mEdtName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long currentStationId = getArguments().getLong("station_id", 0);
        int currentStationPos = -1;

        mOtherStationNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        StationCursor station = new StationSelection().query(getActivity().getContentResolver());
        while (station.moveToNext()) {
            if (currentStationId == station.getId()) {
                currentStationPos = station.getPosition();
            } else {
                mOtherStationNames.add(station.getName());
            }
        }

        if (currentStationPos > -1) {
            station.moveToPosition(currentStationPos);
            mStation = station;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_station);
        dialog.setTitle(mStation == null
            ? R.string.title_add_station
            : R.string.title_edit_station);

        mEdtName = dialog.findViewById(R.id.edt_name);

        if (savedInstanceState != null) {
            mEdtName.setText(savedInstanceState.getString("name"));
        } else if (mStation != null) {
            mEdtName.setText(mStation.getName());
        }

        dialog.findViewById(R.id.btn_ok).setOnClickListener(v -> {
            if (save()) {
                dialog.dismiss();
                getListener().onDialogPositiveClick(getTargetRequestCode());
            }
        });
        dialog.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            dialog.dismiss();
            getListener().onDialogNegativeClick(getTargetRequestCode());
        });

        return dialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("name", mEdtName.getText().toString());
    }

    private EditStationDialogFragmentListener getListener() {
        return (EditStationDialogFragmentListener) getTargetFragment();
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
            StationContentValues values = new StationContentValues();
            values.putName(mEdtName.getText().toString());

            if (mStation == null) {
                values.insert(getActivity().getContentResolver());
            } else {
                StationSelection where = new StationSelection().id(mStation.getId());
                values.update(getActivity().getContentResolver(), where);
            }

            return true;
        } else {
            return false;
        }
    }
}
