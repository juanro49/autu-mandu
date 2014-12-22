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

package me.kuehle.carreport.gui;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.FuelTank;
import me.kuehle.carreport.db.FuelType;
import me.kuehle.carreport.db.PossibleFuelTypeForFuelTank;
import me.kuehle.carreport.gui.dialog.SupportColorPickerDialogFragment;
import me.kuehle.carreport.gui.dialog.SupportColorPickerDialogFragment.SupportColorPickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment;
import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment.SupportDatePickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.SupportInputDialogFragment;
import me.kuehle.carreport.gui.dialog.SupportInputDialogFragment.SupportInputDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.SupportMessageDialogFragment;
import me.kuehle.carreport.gui.util.AbstractFormFieldValidator;
import me.kuehle.carreport.gui.util.FormFieldNotEmptyValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.gui.util.SimpleAnimator;
import me.kuehle.carreport.util.Strings;

public class DataDetailCarFragment extends AbstractDataDetailFragment implements
        SupportColorPickerDialogFragmentListener, SupportDatePickerDialogFragmentListener,
        SupportInputDialogFragmentListener {
    private class FuelTankHolder {
        public ViewGroup layout;
        public FuelTank tank;
        public List<Spinner> spnFuelTypes = new ArrayList<Spinner>();
    }

    private class FuelTypeSelectedListener implements OnItemSelectedListener {
        private FuelTankHolder holder;
        private ViewGroup layoutFuelTypes;
        private int selectedPosition;

        public FuelTypeSelectedListener(FuelTankHolder holder) {
            this.holder = holder;
            this.layoutFuelTypes = (ViewGroup) holder.layout
                    .findViewById(R.id.layout_fuel_types);
            this.selectedPosition = 0;
        }

        @Override
        public void onItemSelected(AdapterView<?> parentView,
                                   View selectedItemView, int position, long id) {
            // Do special actions, when the first (empty) item or
            // the last (add type) item has been selected. Otherwise
            // safe the current selection to be able to restore it
            // when the last item will be selected later.
            if (position == 0) {
                if (holder.spnFuelTypes.size() <= 1) {
                    return;
                }

                boolean emptyElementExists = false;
                for (int i = holder.spnFuelTypes.size() - 1; i >= 0; i--) {
                    final Spinner spn = holder.spnFuelTypes.get(i);
                    if (spn.getSelectedItemPosition() == 0) {
                        if (emptyElementExists) {
                            SimpleAnimator animator = new SimpleAnimator(
                                    getActivity(), spn,
                                    SimpleAnimator.Property.Height);
                            animator.hide(null, new Runnable() {
                                @Override
                                public void run() {
                                    layoutFuelTypes.removeView(spn);
                                    holder.spnFuelTypes.remove(spn);
                                }
                            });
                        } else {
                            emptyElementExists = true;
                        }
                    }
                }
            } else if (position + 1 == parentView.getCount()) {
                currentlyClickedFuelTypeSpinner = parentView;
                parentView.setSelection(selectedPosition);
                SupportInputDialogFragment.newInstance(
                        DataDetailCarFragment.this, REQUEST_ADD_FUEL_TYPE,
                        R.string.alert_add_fuel_type_title, null).show(
                        getFragmentManager(), null);
            } else {
                if (selectedPosition == 0) {
                    addFuelTypeView(holder, null);
                }

                selectedPosition = position;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parentView) {
        }
    }

    private class RemoveFuelTankListener implements OnClickListener {
        private FuelTankHolder holder;
        private boolean removePossible;

        public RemoveFuelTankListener(FuelTankHolder holder) {
            this.holder = holder;
            removePossible = holder.tank.id == null
                    || holder.tank.refuelings().size() == 0;
        }

        @Override
        public void onClick(View v) {
            if (!removePossible) {
                SupportMessageDialogFragment.newInstance(null, 0, null,
                        getString(R.string.alert_cannot_remove_fuel_tank_message),
                        android.R.string.ok, null)
                        .show(getFragmentManager(), null);
                return;
            }

            // Hide remove buttons, when only one item will be left after
            // removing this one.
            for (FuelTankHolder h : fuelTankHolders) {
                h.layout.findViewById(R.id.btn_remove).setVisibility(
                        layoutFuelTanks.getChildCount() == 2 ? View.INVISIBLE
                                : View.VISIBLE);
            }

            // Remove this tank with a nice animation.
            SimpleAnimator animator = new SimpleAnimator(getActivity(),
                    holder.layout, SimpleAnimator.Property.Height);
            animator.hide(null, new Runnable() {
                @Override
                public void run() {
                    layoutFuelTanks.removeView(holder.layout);
                    fuelTankHolders.remove(holder);
                }
            });
        }
    }

    private static final int REQUEST_PICK_COLOR = 1;
    private static final int REQUEST_PICK_SUSPEND_DATE = 2;
    private static final int REQUEST_ADD_FUEL_TYPE = 3;

    private static final String STATE_COLOR = "color";
    private static final String STATE_FUEL_TANK_IDS = "fuel_tank_ids";
    private static final String STATE_FUEL_TANK_NAMES = "fuel_tank_names";
    private static final String STATE_FUEL_TANK_SELECTED_TYPES = "fuel_tank_selected_types";
    private static final String STATE_FUEL_TYPE_IDS = "fuel_type_ids";
    private static final String STATE_FUEL_TYPE_NAMES = "fuel_type_names";
    private static final String STATE_CURRENT_FUEL_TYPE_SPINNER = "current_fuel_type_spinner";

    private EditText edtName;
    private View colorIndicator;
    private int color;
    private ViewGroup layoutFuelTanks;
    private List<FuelTankHolder> fuelTankHolders;
    private ArrayAdapter<String> fuelTypeAdapter;
    private SparseArray<FuelType> fuelTypePositionModelMap;
    private AdapterView<?> currentlyClickedFuelTypeSpinner;
    private CheckBox chkSuspend;

    private EditText edtSuspendDate;

    private SimpleAnimator edtSuspendDateAnimator;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (Car.getCount() == 1) {
            menu.removeItem(R.id.menu_delete);
        }
    }

    @Override
    public void onDialogNegativeClick(int requestCode) {
        currentlyClickedFuelTypeSpinner = null;
    }

    @Override
    public void onDialogPositiveClick(int requestCode, Date date) {
        if (requestCode == REQUEST_PICK_SUSPEND_DATE) {
            edtSuspendDate.setText(DateFormat.getDateFormat(getActivity()).format(date));
        }
    }

    @Override
    public void onDialogPositiveClick(int requestCode, int color) {
        if (requestCode == REQUEST_PICK_COLOR) {
            this.color = color;
            colorIndicator.getBackground().setColorFilter(color, PorterDuff.Mode.SRC);
        }
    }

    @Override
    public void onDialogPositiveClick(int requestCode, String input) {
        if (requestCode == REQUEST_ADD_FUEL_TYPE) {
            if (input.isEmpty()) {
                return;
            }

            if (fuelTypeAdapter.getPosition(input) != AdapterView.INVALID_POSITION) {
                SupportMessageDialogFragment.newInstance(null, 0, null,
                        getString(R.string.alert_fuel_type_exists_message),
                        android.R.string.ok, null).show(getFragmentManager(),
                        null);
            } else {
                int insertPosition = fuelTypeAdapter.getCount() - 1;
                fuelTypeAdapter.insert(input, insertPosition);
                fuelTypePositionModelMap.put(insertPosition, new FuelType(input));
                currentlyClickedFuelTypeSpinner.setSelection(insertPosition);
            }
        }

        currentlyClickedFuelTypeSpinner = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STATE_COLOR, color);

        // Fuel Tanks
        long[] fuelTankIDs = new long[fuelTankHolders.size()];
        String[] fuelTankNames = new String[fuelTankHolders.size()];
        String[] fuelTankSelectedTypes = new String[fuelTankHolders.size()];
        for (int i = 0; i < fuelTankHolders.size(); i++) {
            FuelTankHolder holder = fuelTankHolders.get(i);
            fuelTankIDs[i] = holder.tank.id == null ? 0 : holder.tank.id;
            fuelTankNames[i] = ((EditText) holder.layout
                    .findViewById(R.id.edt_fuel_type_name)).getText()
                    .toString();

            String[] selectedTypes = new String[holder.spnFuelTypes.size()];
            for (int j = 0; j < holder.spnFuelTypes.size(); j++) {
                selectedTypes[j] = String.valueOf(holder.spnFuelTypes.get(j)
                        .getSelectedItemPosition());
            }

            fuelTankSelectedTypes[i] = Strings.join(",", selectedTypes);
        }

        outState.putLongArray(STATE_FUEL_TANK_IDS, fuelTankIDs);
        outState.putStringArray(STATE_FUEL_TANK_NAMES, fuelTankNames);
        outState.putStringArray(STATE_FUEL_TANK_SELECTED_TYPES,
                fuelTankSelectedTypes);

        // Fuel Types
        long[] fuelTypeIDs = new long[fuelTypePositionModelMap.size()];
        String[] fuelTypeNames = new String[fuelTypePositionModelMap.size()];
        for (int i = 0; i < fuelTypePositionModelMap.size(); i++) {
            FuelType fuelType = fuelTypePositionModelMap
                    .get(fuelTypePositionModelMap.keyAt(i));
            fuelTypeIDs[i] = fuelType.id == null ? 0 : fuelType.id;
            fuelTypeNames[i] = fuelType.name;
        }

        outState.putLongArray(STATE_FUEL_TYPE_IDS, fuelTypeIDs);
        outState.putStringArray(STATE_FUEL_TYPE_NAMES, fuelTypeNames);

        // Currently used fuel type spinner
        if (currentlyClickedFuelTypeSpinner != null) {
            int iHolder = 0;
            int iSpinner = 0;

            outerloop:
            for (iHolder = 0; iHolder < fuelTankHolders.size(); iHolder++) {
                FuelTankHolder holder = fuelTankHolders.get(iHolder);
                for (iSpinner = 0; iSpinner < holder.spnFuelTypes.size(); iSpinner++) {
                    Spinner spinner = holder.spnFuelTypes.get(iSpinner);
                    if (spinner == currentlyClickedFuelTypeSpinner) {
                        break outerloop;
                    }
                }
            }

            outState.putIntArray(STATE_CURRENT_FUEL_TYPE_SPINNER, new int[]{
                    iHolder, iSpinner});
        }
    }

    private View addFuelTankView(FuelTank fuelTank, int[] fuelTypePositions) {
        final ViewGroup v = (ViewGroup) View.inflate(getActivity(),
                R.layout.row_fuel_tank, null);
        layoutFuelTanks.addView(v);

        FuelTankHolder holder = new FuelTankHolder();
        holder.layout = v;
        holder.tank = fuelTank;
        fuelTankHolders.add(holder);

        EditText edtTank = (EditText) v.findViewById(R.id.edt_fuel_type_name);
        edtTank.setText(fuelTank.name);
        edtTank.setSaveEnabled(false);

        if (fuelTypePositions == null && fuelTank.id == null) {
            addFuelTypeView(holder, 0);
        } else if (fuelTypePositions == null) {
            List<FuelType> fuelTypes = fuelTank.fuelTypes();
            for (FuelType fuelType : fuelTypes) {
                addFuelTypeView(holder, fuelType);
            }
        } else {
            for (int position : fuelTypePositions) {
                addFuelTypeView(holder, position);
            }
        }

        View btnRemove = v.findViewById(R.id.btn_remove);
        btnRemove.setOnClickListener(new RemoveFuelTankListener(holder));
        if (layoutFuelTanks.getChildCount() == 1) {
            btnRemove.setVisibility(View.INVISIBLE);
        } else {
            for (FuelTankHolder h : fuelTankHolders) {
                h.layout.findViewById(R.id.btn_remove).setVisibility(
                        View.VISIBLE);
            }
        }

        // Measure height, so the SimpleAnimator can store the original height.
        SimpleAnimator animator = new SimpleAnimator(getActivity(), v,
                SimpleAnimator.Property.Height);
        v.setAlpha(0);
        v.getLayoutParams().height = 0;
        animator.show();

        return v;
    }

    private void addFuelTypeView(FuelTankHolder holder, int fuelTypePosition) {
        Spinner spnType = (Spinner) View.inflate(getActivity(),
                R.layout.row_fuel_type, null);
        spnType.setAdapter(fuelTypeAdapter);
        spnType.setOnItemSelectedListener(new FuelTypeSelectedListener(holder));
        spnType.setSelection(fuelTypePosition);

        ViewGroup layoutFuelTypes = (ViewGroup) holder.layout
                .findViewById(R.id.layout_fuel_types);
        layoutFuelTypes.addView(spnType, ViewGroup.LayoutParams.MATCH_PARENT,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                        48, getResources().getDisplayMetrics()));
        holder.spnFuelTypes.add(spnType);

        SimpleAnimator animator = new SimpleAnimator(getActivity(), spnType,
                SimpleAnimator.Property.Height);
        spnType.setAlpha(0);
        spnType.getLayoutParams().height = 0;
        animator.show();
    }

    private void addFuelTypeView(FuelTankHolder holder, FuelType fuelType) {
        int position = fuelType == null ? 0 : fuelTypePositionModelMap
                .keyAt(fuelTypePositionModelMap.indexOfValue(fuelType));
        addFuelTypeView(holder, position);
    }

    private Date getSuspendDate() {
        try {
            return DateFormat.getDateFormat(getActivity()).parse(
                    edtSuspendDate.getText().toString());
        } catch (ParseException e) {
            return new Date();
        }
    }

    @Override
    protected void fillFields(Bundle savedInstanceState, View v) {
        Date suspendDate = new Date();
        if (savedInstanceState == null) {
            if (isInEditMode()) {
                Car car = (Car) editItem;

                edtName.setText(car.name);
                color = car.color;

                List<FuelTank> fuelTanks = car.fuelTanks();
                for (FuelTank fuelTank : fuelTanks) {
                    addFuelTankView(fuelTank, null);
                }

                chkSuspend.setChecked(car.isSuspended());
                if (car.isSuspended()) {
                    suspendDate = car.suspendedSince;
                }
            } else {
                color = Color.BLUE;

                addFuelTankView(new FuelTank(null, ""), null);
            }
        } else {
            color = savedInstanceState.getInt(STATE_COLOR);

            // Fuel tanks
            long[] fuelTankIDs = savedInstanceState
                    .getLongArray(STATE_FUEL_TANK_IDS);
            String[] fuelTankNames = savedInstanceState
                    .getStringArray(STATE_FUEL_TANK_NAMES);
            String[] fuelTankSelectedTypes = savedInstanceState
                    .getStringArray(STATE_FUEL_TANK_SELECTED_TYPES);
            for (int i = 0; i < fuelTankIDs.length; i++) {
                FuelTank fuelTank;
                if (fuelTankIDs[i] == 0) {
                    fuelTank = new FuelTank(null, fuelTankNames[i]);
                } else {
                    fuelTank = FuelTank.load(FuelTank.class, fuelTankIDs[i]);
                    fuelTank.name = fuelTankNames[i];
                }

                String[] strSelectedTypes = fuelTankSelectedTypes[i].split(",");
                int[] selectedTypes = new int[strSelectedTypes.length];
                for (int j = 0; j < strSelectedTypes.length; j++) {
                    selectedTypes[j] = Integer.parseInt(strSelectedTypes[j]);
                }

                addFuelTankView(fuelTank, selectedTypes);
            }

            // Currently clicked fuel type spinner
            int[] indices = savedInstanceState
                    .getIntArray(STATE_CURRENT_FUEL_TYPE_SPINNER);
            if (indices != null) {
                currentlyClickedFuelTypeSpinner = fuelTankHolders
                        .get(indices[0]).spnFuelTypes.get(indices[1]);
            }
        }

        colorIndicator.getBackground().setColorFilter(color, PorterDuff.Mode.SRC);
        if (!chkSuspend.isChecked()) {
            edtSuspendDate.getLayoutParams().height = 0;
            edtSuspendDate.setAlpha(0);
        }
        edtSuspendDate.setText(DateFormat.getDateFormat(getActivity()).format(
                suspendDate));
    }

    @Override
    protected int getAlertDeleteMessage() {
        return R.string.alert_delete_car_message;
    }

    @Override
    protected Model getEditItem(long id) {
        return Car.load(Car.class, id);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_data_detail_car;
    }

    @Override
    protected int getTitleForEdit() {
        return R.string.title_edit_car;
    }

    @Override
    protected int getTitleForNew() {
        return R.string.title_add_car;
    }

    @Override
    protected int getToastDeletedMessage() {
        return R.string.toast_car_deleted;
    }

    @Override
    protected int getToastSavedMessage() {
        return R.string.toast_car_saved;
    }

    @Override
    protected void initFields(Bundle savedInstanceState, View v) {
        edtName = (EditText) v.findViewById(R.id.edt_name);
        colorIndicator = v.findViewById(R.id.btn_color);
        layoutFuelTanks = (ViewGroup) v.findViewById(R.id.layout_fuel_tanks);
        fuelTankHolders = new ArrayList<FuelTankHolder>();
        fuelTypeAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item);
        fuelTypePositionModelMap = new SparseArray<FuelType>();
        chkSuspend = (CheckBox) v.findViewById(R.id.chk_suspend);
        edtSuspendDate = (EditText) v.findViewById(R.id.edt_suspend_date);
        edtSuspendDateAnimator = new SimpleAnimator(getActivity(),
                edtSuspendDate, SimpleAnimator.Property.Height);

        View rowColor = (View) colorIndicator.getParent();
        rowColor.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SupportColorPickerDialogFragment.newInstance(
                        DataDetailCarFragment.this, REQUEST_PICK_COLOR,
                        R.string.alert_change_color_title, color).show(
                        getFragmentManager(), null);
            }
        });

        fuelTypeAdapter.add(getString(R.string.hint_select_fuel_type));
        if (savedInstanceState == null) {
            List<FuelType> fuelTypes = FuelType.getAll();
            for (FuelType fuelType : fuelTypes) {
                fuelTypeAdapter.add(fuelType.name);
                fuelTypePositionModelMap.append(fuelTypeAdapter.getCount() - 1,
                        fuelType);
            }
        } else {
            // Restore state
            long[] fuelTypeIDs = savedInstanceState
                    .getLongArray(STATE_FUEL_TYPE_IDS);
            String[] fuelTypeNames = savedInstanceState
                    .getStringArray(STATE_FUEL_TYPE_NAMES);
            for (int i = 0; i < fuelTypeIDs.length; i++) {
                FuelType fuelType;
                if (fuelTypeIDs[i] == 0) {
                    fuelType = new FuelType(fuelTypeNames[i]);
                } else {
                    fuelType = FuelType.load(FuelType.class, fuelTypeIDs[i]);
                    fuelType.name = fuelTypeNames[i];
                }

                fuelTypeAdapter.add(fuelType.name);
                fuelTypePositionModelMap.append(fuelTypeAdapter.getCount() - 1,
                        fuelType);
            }
        }
        fuelTypeAdapter.add(getString(R.string.label_add_dialog));

        Button btnAddFuelTank = (Button) v.findViewById(R.id.btn_add_fuel_tank);
        btnAddFuelTank.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                addFuelTankView(new FuelTank(null, ""), null);
            }
        });

        chkSuspend.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {
                if (isChecked) {
                    edtSuspendDateAnimator.show();
                } else {
                    edtSuspendDateAnimator.hide();
                }
            }
        });

        edtSuspendDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SupportDatePickerDialogFragment.newInstance(
                        DataDetailCarFragment.this, REQUEST_PICK_SUSPEND_DATE,
                        getSuspendDate()).show(getFragmentManager(), null);
            }
        });
    }

    @Override
    protected void save() {
        String name = edtName.getText().toString();
        Date suspended = chkSuspend.isChecked() ? getSuspendDate() : null;

        ActiveAndroid.beginTransaction();
        try {
            Car car;
            if (!isInEditMode()) {
                car = new Car(name, color, suspended);
            } else {
                car = (Car) editItem;
                car.name = name;
                car.color = color;
                car.suspendedSince = suspended;
            }
            car.save();

            // Delete old fuel tank <> fuel type associations.
            PossibleFuelTypeForFuelTank.deleteAll(car);

            // Create new fuel tanks, types and associations.
            HashSet<String> addedPossibleTypes = new HashSet<String>();
            HashSet<Long> remainingFuelTanks = new HashSet<Long>();
            for (int i = 0; i < layoutFuelTanks.getChildCount(); i++) {
                View ftView = layoutFuelTanks.getChildAt(i);
                FuelTankHolder holder = fuelTankHolders.get(i);

                EditText edtName = (EditText) ftView
                        .findViewById(R.id.edt_fuel_type_name);
                holder.tank.car = car;
                holder.tank.name = edtName.getText().toString().trim();
                holder.tank.save();
                remainingFuelTanks.add(holder.tank.id);

                for (Spinner spnType : holder.spnFuelTypes) {
                    int typePos = spnType.getSelectedItemPosition();
                    if (typePos == 0) {
                        continue;
                    }

                    FuelType type = fuelTypePositionModelMap.get(typePos);
                    type.save();

                    // Only create an association, when the same has not been
                    // created before.
                    if (addedPossibleTypes.add(type.id + "<>" + holder.tank.id)) {
                        new PossibleFuelTypeForFuelTank(type, holder.tank)
                                .save();
                    }
                }
            }

            // Delete removed fuel tanks.
            for (FuelTank tank : car.fuelTanks()) {
                if (!remainingFuelTanks.contains(tank.id)) {
                    tank.delete();
                }
            }

            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }
    }

    @Override
    protected boolean validate() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldNotEmptyValidator(edtName));

        for (int i = 0; i < layoutFuelTanks.getChildCount(); i++) {
            View ftView = layoutFuelTanks.getChildAt(i);
            EditText edtTankName = (EditText) ftView
                    .findViewById(R.id.edt_fuel_type_name);
            validator.add(new FormFieldNotEmptyValidator(edtTankName));
            final FuelTankHolder holder = fuelTankHolders.get(i);
            validator.add(new AbstractFormFieldValidator(edtTankName) {
                @Override
                protected boolean isValid() {
                    return holder.spnFuelTypes.size() > 1;
                }

                @Override
                protected int getMessage() {
                    return R.string.validate_error_no_fuel_types;
                }
            });
        }

        return validator.validate();
    }
}
