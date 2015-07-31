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
import android.app.DialogFragment;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;

import java.util.Set;
import java.util.TreeSet;

import me.kuehle.carreport.R;
import me.kuehle.carreport.data.query.FuelTypeQueries;
import me.kuehle.carreport.gui.util.AbstractFormFieldValidator;
import me.kuehle.carreport.gui.util.FormFieldNotEmptyValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.provider.fueltype.FuelTypeContentValues;
import me.kuehle.carreport.provider.fueltype.FuelTypeCursor;
import me.kuehle.carreport.provider.fueltype.FuelTypeSelection;

public class EditFuelTypeDialogFragment extends DialogFragment {
    public interface EditFuelTypeDialogFragmentListener {
        void onDialogNegativeClick(int requestCode);

        void onDialogPositiveClick(int requestCode);
    }

    public static EditFuelTypeDialogFragment newInstance(Fragment parent, int requestCode, long fuelTypeId) {
        EditFuelTypeDialogFragment f = new EditFuelTypeDialogFragment();
        f.setTargetFragment(parent, requestCode);

        Bundle args = new Bundle();
        args.putLong("fuel_type_id", fuelTypeId);
        f.setArguments(args);

        return f;
    }

    private Set<String> mOtherFuelTypeNames;
    private FuelTypeCursor mFuelType;
    private EditText mEdtName;
    private AutoCompleteTextView mEdtCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long currentFuelTypeId = getArguments().getLong("fuel_type_id", 0);
        int currentFuelTypePos = -1;

        mOtherFuelTypeNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        FuelTypeCursor fuelType = new FuelTypeSelection().query(getActivity().getContentResolver());
        while (fuelType.moveToNext()) {
            if (currentFuelTypeId == fuelType.getId()) {
                currentFuelTypePos = fuelType.getPosition();
            } else {
                mOtherFuelTypeNames.add(fuelType.getName());
            }
        }

        if (currentFuelTypePos > -1) {
            fuelType.moveToPosition(currentFuelTypePos);
            mFuelType = fuelType;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.setContentView(R.layout.dialog_fuel_type);
        dialog.setTitle(mFuelType == null ? R.string.title_add_fuel_type :
                R.string.title_edit_fuel_type);

        mEdtName = (EditText) dialog.findViewById(R.id.edt_name);
        mEdtCategory = (AutoCompleteTextView) dialog.findViewById(R.id.edt_category);
        mEdtCategory.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                FuelTypeQueries.getAllCategories(getActivity())));

        if (savedInstanceState != null) {
            mEdtName.setText(savedInstanceState.getString("name"));
            mEdtCategory.setText(savedInstanceState.getString("category"));
        } else if (mFuelType != null) {
            mEdtName.setText(mFuelType.getName());
            mEdtCategory.setText(mFuelType.getCategory());
        }

        dialog.findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (save()) {
                    dialog.dismiss();
                    getListener().onDialogPositiveClick(getTargetRequestCode());
                }
            }
        });
        dialog.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                getListener().onDialogNegativeClick(getTargetRequestCode());
            }
        });

        return dialog;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("name", mEdtName.getText().toString());
        outState.putString("category", mEdtCategory.getText().toString());
    }

    private EditFuelTypeDialogFragmentListener getListener() {
        return (EditFuelTypeDialogFragmentListener) getTargetFragment();
    }

    private boolean save() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldNotEmptyValidator(mEdtName));
        validator.add(new AbstractFormFieldValidator(mEdtName) {
            @Override
            protected boolean isValid() {
                String name = mEdtName.getText().toString();
                return !mOtherFuelTypeNames.contains(name);
            }

            @Override
            protected int getMessage() {
                return R.string.validate_error_fuel_type_exists;
            }
        });
        validator.add(new FormFieldNotEmptyValidator(mEdtCategory));

        if (validator.validate()) {
            FuelTypeContentValues values = new FuelTypeContentValues();
            values.putName(mEdtName.getText().toString());
            values.putCategory(mEdtCategory.getText().toString());

            if (mFuelType == null) {
                values.insert(getActivity().getContentResolver());
            } else {
                FuelTypeSelection where = new FuelTypeSelection().id(mFuelType.getId());
                values.update(getActivity().getContentResolver(), where);
            }

            return true;
        } else {
            return false;
        }
    }
}
