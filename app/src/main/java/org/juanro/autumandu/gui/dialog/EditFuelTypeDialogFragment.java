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

package org.juanro.autumandu.gui.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.util.AbstractFormFieldValidator;
import org.juanro.autumandu.gui.util.FormFieldNotEmptyValidator;
import org.juanro.autumandu.gui.util.FormValidator;
import org.juanro.autumandu.model.entity.FuelType;
import org.juanro.autumandu.viewmodel.FuelTypesViewModel;

public class EditFuelTypeDialogFragment extends DialogFragment {
    public static final String REQUEST_KEY = "org.juanro.autumandu.EDIT_FUEL_TYPE_REQUEST";
    public static final String RESULT_ACTION = "action";
    public static final String RESULT_REQUEST_CODE = "request_code";

    public static final int ACTION_POSITIVE = 1;
    public static final int ACTION_NEGATIVE = 2;

    private static final String ARG_FUEL_TYPE_ID = "fuel_type_id";
    private static final String ARG_REQUEST_CODE = "request_code";

    public static EditFuelTypeDialogFragment newInstance(int requestCode, long fuelTypeId) {
        EditFuelTypeDialogFragment f = new EditFuelTypeDialogFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_FUEL_TYPE_ID, fuelTypeId);
        args.putInt(ARG_REQUEST_CODE, requestCode);
        f.setArguments(args);

        return f;
    }

    private Set<String> mOtherFuelTypeNames;
    private FuelType mFuelType;
    private EditText mEdtName;
    private AutoCompleteTextView mEdtCategory;
    private FuelTypesViewModel mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long currentFuelTypeId = getArguments() != null ? getArguments().getLong(ARG_FUEL_TYPE_ID, 0) : 0;
        mOtherFuelTypeNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        mViewModel = new ViewModelProvider(this).get(FuelTypesViewModel.class);

        mViewModel.getFuelTypes().observe(this, fuelTypes -> {
            mOtherFuelTypeNames.clear();
            Set<String> categories = new HashSet<>();
            for (FuelType fuelType : fuelTypes) {
                if (currentFuelTypeId == fuelType.getId()) {
                    mFuelType = fuelType;
                    if (mEdtName != null && mEdtName.getText().length() == 0) {
                        mEdtName.setText(mFuelType.getName());
                    }
                    if (mEdtCategory != null && mEdtCategory.getText().length() == 0) {
                        mEdtCategory.setText(mFuelType.getCategory());
                    }
                } else {
                    mOtherFuelTypeNames.add(fuelType.getName());
                }
                categories.add(fuelType.getCategory());
            }

            if (mEdtCategory != null) {
                String[] categoryArray = categories.toArray(new String[0]);
                mEdtCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        categoryArray));
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(requireActivity());
        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(R.layout.dialog_fuel_type, null);
        dialog.setContentView(view);

        mEdtName = view.findViewById(R.id.edt_name);
        mEdtCategory = view.findViewById(R.id.edt_category);

        long currentFuelTypeId = getArguments() != null ? getArguments().getLong(ARG_FUEL_TYPE_ID, 0) : 0;
        dialog.setTitle(currentFuelTypeId == 0
                ? R.string.title_add_fuel_type
                : R.string.title_edit_fuel_type);

        if (savedInstanceState != null) {
            mEdtName.setText(savedInstanceState.getString("name"));
            mEdtCategory.setText(savedInstanceState.getString("category"));
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
        if (mEdtCategory != null) {
            outState.putString("category", mEdtCategory.getText().toString());
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
                return !mOtherFuelTypeNames.contains(name);
            }

            @Override
            protected int getMessage() {
                return R.string.validate_error_fuel_type_exists;
            }
        });
        validator.add(new FormFieldNotEmptyValidator(mEdtCategory));

        if (validator.validate()) {
            if (mFuelType == null) {
                FuelType fuelType = new FuelType(
                        mEdtName.getText().toString(),
                        mEdtCategory.getText().toString()
                );
                mViewModel.saveFuelType(fuelType);
            } else {
                mFuelType.setName(mEdtName.getText().toString());
                mFuelType.setCategory(mEdtCategory.getText().toString());
                mViewModel.saveFuelType(mFuelType);
            }
            return true;
        } else {
            return false;
        }
    }
}
