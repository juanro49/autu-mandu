/*
 * Copyright 2026 Juanro49
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

package org.juanro.autumandu.gui.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.dialog.DatePickerDialogFragment;
import org.juanro.autumandu.gui.dialog.TimePickerDialogFragment;
import org.juanro.autumandu.gui.util.DateTimeInput;
import org.juanro.autumandu.gui.util.FormFieldGreaterZeroValidator;
import org.juanro.autumandu.gui.util.FormValidator;
import org.juanro.autumandu.gui.util.SimpleAnimator;
import org.juanro.autumandu.model.entity.TireList;
import org.juanro.autumandu.model.entity.Car;
import org.juanro.autumandu.model.entity.TireUsage;
import org.juanro.autumandu.util.reminder.ReminderWorker;
import org.juanro.autumandu.viewmodel.TireDetailViewModel;

import java.util.Date;

/**
 * Fragment to edit tire details.
 */
public class DataDetailTireFragment extends AbstractDataDetailFragment {
    private static final int PICK_DATE_REQUEST_CODE = 0;
    private static final int PICK_TRASH_DATE_REQUEST_CODE = 1;
    private static final int PICK_MOUNT_DATE_REQUEST_CODE = 2;
    private static final int PICK_UMOUNT_DATE_REQUEST_CODE = 3;
    private static final int PICK_TIME_REQUEST_CODE = 4;

    public static DataDetailTireFragment newInstance(long id) {
        var f = new DataDetailTireFragment();
        var args = new Bundle();
        args.putLong(AbstractDataDetailFragment.EXTRA_ID, id);
        f.setArguments(args);
        return f;
    }

    private EditText edtManufacturer;
    private EditText edtModel;
    private DateTimeInput edtDate;
    private DateTimeInput edtTime;
    private EditText edtQuantity;
    private EditText edtPrice;
    private EditText edtNote;
    private Spinner spnCar;
    private CheckBox chkMount;
    private EditText edtMountDistance;
    private DateTimeInput edtMountDate;
    private TextInputLayout edtMountDistanceInputLayout;
    private TextInputLayout edtMountDateInputLayout;
    private SimpleAnimator edtMountDistanceAnimator;
    private SimpleAnimator edtMountDateAnimator;
    private SimpleAnimator chkMountAnimator;
    private CheckBox chkUmount;
    private EditText edtUmountDistance;
    private DateTimeInput edtUmountDate;
    private TextInputLayout edtUmountDistanceInputLayout;
    private TextInputLayout edtUmountDateInputLayout;
    private SimpleAnimator edtUmountDistanceAnimator;
    private SimpleAnimator edtUmountDateAnimator;
    private SimpleAnimator chkUmountAnimator;
    private CheckBox chkTrashDate;
    private DateTimeInput edtTrashDate;
    private TextInputLayout edtTrashDateInputLayout;
    private SimpleAnimator edtTrashDateAnimator;
    private SimpleAnimator chkTrashDateAnimator;

    private int carNumTires;
    private int numTiresMounted;
    private long mInitialCarId = -1;

    private TireDetailViewModel viewModel;
    private TireList tire;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TireDetailViewModel.class);
        viewModel.setTireId(mId);

        getParentFragmentManager().setFragmentResultListener(DatePickerDialogFragment.REQUEST_KEY, this, (requestKey, result) -> {
            int requestCode = result.getInt(DatePickerDialogFragment.RESULT_REQUEST_CODE);
            var date = new Date(result.getLong(DatePickerDialogFragment.RESULT_DATE));
            switch (requestCode) {
                case PICK_DATE_REQUEST_CODE -> edtDate.setDate(date);
                case PICK_TRASH_DATE_REQUEST_CODE -> edtTrashDate.setDate(date);
                case PICK_MOUNT_DATE_REQUEST_CODE -> edtMountDate.setDate(date);
                case PICK_UMOUNT_DATE_REQUEST_CODE -> edtUmountDate.setDate(date);
            }
        });
        getParentFragmentManager().setFragmentResultListener(TimePickerDialogFragment.REQUEST_KEY, this, (requestKey, result) -> {
            int requestCode = result.getInt(TimePickerDialogFragment.RESULT_REQUEST_CODE);
            var date = new Date(result.getLong(TimePickerDialogFragment.RESULT_TIME));
            if (requestCode == PICK_TIME_REQUEST_CODE) {
                edtTime.setDate(date);
            }
        });
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View v) {
        if (!isInEditMode()) {
            setupNewTireDefaults();
        } else {
            setupEditTireObservers();
        }

        viewModel.getCarNumTires().observe(getViewLifecycleOwner(), numTires -> this.carNumTires = numTires != null ? numTires : 0);
        viewModel.getNumTiresMounted().observe(getViewLifecycleOwner(), numMounted -> this.numTiresMounted = numMounted != null ? numMounted : 0);

        setupLatestMileageObserver();
        hideOptionalInputLayouts();
    }

    private void setupNewTireDefaults() {
        var prefs = new Preferences(requireContext());
        tire = new TireList();
        edtDate.setDate(new Date());
        edtTime.setDate(new Date());
        edtTrashDate.setDate(new Date());
        edtMountDate.setDate(new Date());
        edtUmountDate.setDate(new Date());
        chkMount.setVisibility(View.GONE);
        chkMountAnimator.hide();
        chkUmount.setVisibility(View.GONE);
        chkUmountAnimator.hide();
        chkTrashDate.setVisibility(View.GONE);
        chkTrashDateAnimator.hide();

        mInitialCarId = getArguments() != null ? getArguments().getLong(EXTRA_CAR_ID) : 0;
        if (mInitialCarId == 0) {
            mInitialCarId = prefs.getDefaultCar();
        }
        viewModel.setCarId(mInitialCarId);
    }

    private void setupEditTireObservers() {
        viewModel.getTire().observe(getViewLifecycleOwner(), tireList -> {
            if (tireList == null) return;
            tire = tireList;
            viewModel.setCarId(tireList.getCarId());
            updateFieldsFromTire(tireList);
            updateInitialCarSelection(tireList.getCarId());
            edtMountDate.setDate(new Date());
            edtUmountDate.setDate(new Date());
        });

        viewModel.isTireMounted().observe(getViewLifecycleOwner(), isMounted -> {
            if (isMounted) {
                chkMount.setVisibility(View.GONE);
                chkTrashDate.setVisibility(View.GONE);
                chkMountAnimator.hide();
                chkTrashDateAnimator.hide();
            } else {
                chkUmount.setVisibility(View.GONE);
                chkUmountAnimator.hide();
            }
        });
    }

    private void updateFieldsFromTire(TireList tireList) {
        edtDate.setDate(tireList.getBuyDate());
        edtTime.setDate(tireList.getBuyDate());
        edtManufacturer.setText(tireList.getManufacturer());
        edtModel.setText(tireList.getModel());
        edtQuantity.setText(String.valueOf(tireList.getQuantity()));
        edtPrice.setText(String.valueOf(tireList.getPrice()));
        edtNote.setText(tireList.getNote());

        if (tireList.getTrashDate() != null) {
            chkMount.setVisibility(View.GONE);
            chkUmount.setVisibility(View.GONE);
            chkMountAnimator.hide();
            chkUmountAnimator.hide();
            chkTrashDate.setChecked(true);
        }
        edtTrashDate.setDate(tireList.getTrashDate() == null ? new Date() : tireList.getTrashDate());
    }

    private void updateInitialCarSelection(long carId) {
        mInitialCarId = carId;
        int count = spnCar.getCount();
        for (int i = 0; i < count; i++) {
            if (spnCar.getItemIdAtPosition(i) == mInitialCarId) {
                spnCar.setSelection(i);
                break;
            }
        }
    }

    private void setupLatestMileageObserver() {
        viewModel.getLatestMileage().observe(getViewLifecycleOwner(), latestMileage -> {
            int mileage = latestMileage != null ? latestMileage : 0;
            if (TextUtils.isEmpty(edtMountDistance.getText())) {
                edtMountDistance.setText(String.valueOf(mileage));
            }
            if (TextUtils.isEmpty(edtUmountDistance.getText())) {
                edtUmountDistance.setText(String.valueOf(mileage));
            }
            if (edtMountDate.getDate() == null) {
                edtMountDate.setDate(new Date());
            }
            if (edtUmountDate.getDate() == null) {
                edtUmountDate.setDate(new Date());
            }
        });
    }

    private void hideOptionalInputLayouts() {
        if (!chkMount.isChecked()) {
            hideInputLayout(edtMountDistanceInputLayout);
            hideInputLayout(edtMountDateInputLayout);
        }
        if (!chkUmount.isChecked()) {
            hideInputLayout(edtUmountDistanceInputLayout);
            hideInputLayout(edtUmountDateInputLayout);
        }
        if (!chkTrashDate.isChecked()) {
            hideInputLayout(edtTrashDateInputLayout);
        }
    }

    private void hideInputLayout(TextInputLayout layout) {
        layout.setVisibility(View.GONE);
        layout.getLayoutParams().height = 0;
        layout.setAlpha(0);
    }

    @Override
    protected int getAlertDeleteMessage() {
        return R.string.alert_delete_tire_message;
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_data_detail_tire;
    }

    @Override
    protected int getTitleForEdit() {
        return R.string.title_edit_tire;
    }

    @Override
    protected int getTitleForNew() {
        return R.string.title_add_tire;
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        var prefs = new Preferences(requireContext());

        initViewReferences(v);
        setupDateAndTimePickers();
        setupUnits(prefs);
        setupMountListeners();
        setupUmountListeners();
        setupTrashListeners();
        setupCarSpinner();
    }

    private void initViewReferences(View v) {
        edtManufacturer = v.findViewById(R.id.edt_manufacturer);
        edtModel = v.findViewById(R.id.edt_model);
        edtDate = new DateTimeInput(v.findViewById(R.id.edt_date), DateTimeInput.Mode.DATE);
        edtTime = new DateTimeInput(v.findViewById(R.id.edt_time), DateTimeInput.Mode.TIME);
        edtQuantity = v.findViewById(R.id.edt_quantity);
        edtPrice = v.findViewById(R.id.edt_price);
        edtNote = v.findViewById(R.id.edt_note);
        spnCar = v.findViewById(R.id.spn_car);
        chkMount = v.findViewById(R.id.chk_mount);
        edtMountDistance = v.findViewById(R.id.edt_mount_distance);
        edtMountDate = new DateTimeInput(v.findViewById(R.id.edt_mount_date), DateTimeInput.Mode.DATE);
        edtMountDistanceInputLayout = v.findViewById(R.id.edt_mount_distance_input_layout);
        edtMountDateInputLayout = v.findViewById(R.id.edt_mount_date_input_layout);
        edtMountDistanceAnimator = new SimpleAnimator(getActivity(), edtMountDistanceInputLayout, SimpleAnimator.Property.HEIGHT);
        edtMountDateAnimator = new SimpleAnimator(getActivity(), edtMountDateInputLayout, SimpleAnimator.Property.HEIGHT);
        chkUmount = v.findViewById(R.id.chk_umount);
        edtUmountDistance = v.findViewById(R.id.edt_umount_distance);
        edtUmountDate = new DateTimeInput(v.findViewById(R.id.edt_umount_date), DateTimeInput.Mode.DATE);
        edtUmountDistanceInputLayout = v.findViewById(R.id.edt_umount_distance_input_layout);
        edtUmountDateInputLayout = v.findViewById(R.id.edt_umount_date_input_layout);
        edtUmountDistanceAnimator = new SimpleAnimator(getActivity(), edtUmountDistanceInputLayout, SimpleAnimator.Property.HEIGHT);
        edtUmountDateAnimator = new SimpleAnimator(getActivity(), edtUmountDateInputLayout, SimpleAnimator.Property.HEIGHT);
        chkMountAnimator = new SimpleAnimator(getActivity(), chkMount, SimpleAnimator.Property.HEIGHT);
        chkUmountAnimator = new SimpleAnimator(getActivity(), chkUmount, SimpleAnimator.Property.HEIGHT);
        chkTrashDate = v.findViewById(R.id.chk_trash_date);
        edtTrashDate = new DateTimeInput(v.findViewById(R.id.edt_trash_date), DateTimeInput.Mode.DATE);
        edtTrashDateInputLayout = v.findViewById(R.id.edt_trash_date_input_layout);
        edtTrashDateAnimator = new SimpleAnimator(getActivity(), edtTrashDateInputLayout, SimpleAnimator.Property.HEIGHT);
        chkTrashDateAnimator = new SimpleAnimator(getActivity(), chkTrashDate, SimpleAnimator.Property.HEIGHT);
    }

    private void setupDateAndTimePickers() {
        edtDate.applyOnClickListener(PICK_DATE_REQUEST_CODE, getParentFragmentManager());
        edtTime.applyOnClickListener(PICK_TIME_REQUEST_CODE, getParentFragmentManager());
        edtMountDate.applyOnClickListener(PICK_MOUNT_DATE_REQUEST_CODE, getParentFragmentManager());
        edtUmountDate.applyOnClickListener(PICK_UMOUNT_DATE_REQUEST_CODE, getParentFragmentManager());
        edtTrashDate.applyOnClickListener(PICK_TRASH_DATE_REQUEST_CODE, getParentFragmentManager());
    }

    private void setupUnits(Preferences prefs) {
        addUnitToHint(edtPrice, R.string.hint_price, prefs.getUnitCurrency());
    }

    private void setupMountListeners() {
        chkMount.setOnClickListener(v1 -> {
            boolean isChecked = chkMount.isChecked();
            if (isChecked) {
                int quantityToMount = getIntegerFromEditText(edtQuantity, 0);
                int totalAfterMount = numTiresMounted + quantityToMount;

                if (carNumTires < totalAfterMount) {
                    chkMount.setChecked(false);
                    Toast.makeText(requireContext(), R.string.toast_all_tires_mounted, Toast.LENGTH_LONG).show();
                    return;
                }

                if (edtMountDate.getDate() == null) edtMountDate.setDate(new Date());

                chkTrashDate.setChecked(false);
                edtTrashDateAnimator.hide();
                chkTrashDateAnimator.hide();
                edtMountDistanceAnimator.show();
                edtMountDateAnimator.show();
            } else {
                edtMountDistanceAnimator.hide();
                edtMountDateAnimator.hide();
                chkTrashDateAnimator.show();
            }
        });
    }

    private void setupUmountListeners() {
        chkUmount.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (edtUmountDate.getDate() == null) edtUmountDate.setDate(new Date());
                edtUmountDistanceAnimator.show();
                edtUmountDateAnimator.show();
            } else {
                edtUmountDistanceAnimator.hide();
                edtUmountDateAnimator.hide();
            }
        });
    }

    private void setupTrashListeners() {
        chkTrashDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (edtTrashDate.getDate() == null) edtTrashDate.setDate(new Date());
                edtTrashDateAnimator.show();
            } else {
                edtTrashDateAnimator.hide();
            }
        });
    }

    private void setupCarSpinner() {
        viewModel.getCars().observe(getViewLifecycleOwner(), cars -> {
            spnCar.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, cars) {
                @Override
                public long getItemId(int position) {
                    var item = getItem(position);
                    return item != null ? item.getId() : -1;
                }
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    var v1 = (TextView) super.getView(position, convertView, parent);
                    var item = getItem(position);
                    if (item != null) v1.setText(item.getName());
                    return v1;
                }
                @Override
                public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    var v1 = (TextView) super.getDropDownView(position, convertView, parent);
                    var item = getItem(position);
                    if (item != null) v1.setText(item.getName());
                    return v1;
                }
            });
            if (mInitialCarId != -1) {
                int count = spnCar.getCount();
                for (int i = 0; i < count; i++) {
                    if (spnCar.getItemIdAtPosition(i) == mInitialCarId) {
                        spnCar.setSelection(i);
                        break;
                    }
                }
            }
        });

        spnCar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setCarId(id);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Not used
            }
        });
    }


    @Override
    protected void saveAsync() {
        if (tire == null) return;

        updateTireFromFields();

        TireUsage newUsage = null;
        if (chkMount.isChecked()) {
            newUsage = prepareNewUsage();
            if (newUsage == null) return; // Validation failed
        }

        if (chkUmount.isChecked()) {
            processUnmounting(newUsage);
        } else {
            viewModel.save(tire, newUsage, null, this::onSaved);
        }
    }

    private void updateTireFromFields() {
        Date trashDate = null;
        if (chkTrashDate.isChecked()) {
            trashDate = edtTrashDate.getDate();
        }

        tire.setManufacturer(edtManufacturer.getText().toString().trim());
        tire.setModel(edtModel.getText().toString().trim());
        tire.setBuyDate(DateTimeInput.getDateTime(edtDate.getDate(), edtTime.getDate()));
        tire.setQuantity(getIntegerFromEditText(edtQuantity, 4));
        tire.setPrice((float) getDoubleFromEditText(edtPrice));
        tire.setTrashDate(trashDate);
        tire.setNote(edtNote.getText().toString().trim());
        tire.setCarId(spnCar.getSelectedItemId());
    }

    private TireUsage prepareNewUsage() {
        int currentQuantity = getIntegerFromEditText(edtQuantity, 0);
        Integer numMountedValue = viewModel.getNumTiresMounted().getValue();
        int totalAfterMount = (numMountedValue != null ? numMountedValue : 0);

        Boolean isAlreadyMounted = viewModel.isTireMounted().getValue();
        if (isAlreadyMounted != null && isAlreadyMounted) {
            totalAfterMount = totalAfterMount - tire.getQuantity() + currentQuantity;
        } else {
            totalAfterMount += currentQuantity;
        }

        if (carNumTires < totalAfterMount) {
            Toast.makeText(requireContext(), R.string.toast_all_tires_mounted, Toast.LENGTH_LONG).show();
            return null;
        }

        TireUsage newUsage = new TireUsage();
        var mountDate = edtMountDate.getDate();
        newUsage.setDateMount(mountDate != null ? mountDate : new Date());
        newUsage.setDistanceMount(getIntegerFromEditText(edtMountDistance, 0));
        newUsage.setDateUmount(null);
        newUsage.setDistanceUmount(0);
        return newUsage;
    }

    private void processUnmounting(TireUsage newUsage) {
        int umountDistance = getIntegerFromEditText(edtUmountDistance, 0);

        viewModel.getUsageByTireIdNotUmount(mId, usage -> {
            if (usage != null) {
                if (umountDistance < usage.getDistanceMount()) {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), R.string.alert_invalid_distance, Toast.LENGTH_SHORT).show());
                    return;
                }

                usage.setDateUmount(edtUmountDate.getDate());
                usage.setDistanceUmount(umountDistance);
                viewModel.save(tire, newUsage, usage, this::onSaved);
            } else {
                // Fallback if no active usage found, save only tire and potential newUsage
                viewModel.save(tire, newUsage, null, this::onSaved);
            }
        });
    }

    private void onSaved() {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                ReminderWorker.enqueueUpdate(requireContext());
                mOnItemActionListener.onItemSavedAsync(tire.getId());
            });
        }
    }

    @Override
    protected void deleteAsync() {
        viewModel.delete(mId, () -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> mOnItemActionListener.onItemDeletedAsync());
            }
        });
    }

    @Override
    protected long save() {
        return 0; // Using saveAsync
    }

    @Override
    protected void delete() {
        // Using deleteAsync
    }

    @Override
    protected boolean validate() {
        var validator = new FormValidator();
        validator.add(new FormFieldGreaterZeroValidator(edtQuantity));
        return validator.validate();
    }
}
