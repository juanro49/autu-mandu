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

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.FuelTank;
import me.kuehle.carreport.db.FuelType;
import me.kuehle.carreport.db.PossibleFuelTypeForFuelTank;
import me.kuehle.carreport.gui.dialog.ColorPickerDialogFragment;
import me.kuehle.carreport.gui.dialog.ColorPickerDialogFragment.ColorPickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.DatePickerDialogFragment;
import me.kuehle.carreport.gui.dialog.DatePickerDialogFragment.DatePickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.InputDialogFragment;
import me.kuehle.carreport.gui.dialog.InputDialogFragment.InputDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.SupportMessageDialogFragment;
import me.kuehle.carreport.gui.util.FormFieldNotEmptyValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.gui.util.SimpleAnimator;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
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

public class DataDetailCarFragment extends AbstractDataDetailFragment implements
		ColorPickerDialogFragmentListener, DatePickerDialogFragmentListener,
		InputDialogFragmentListener {
	private static final int REQUEST_PICK_COLOR = 1;
	private static final int REQUEST_PICK_SUSPEND_DATE = 2;
	private static final int REQUEST_ADD_FUEL_TYPE = 3;
	private static final int REQUEST_ADD_FUEL_TANK = 4;

	private EditText edtName;
	private View colorIndicator;
	private int color;
	private ViewGroup possibleFuelTypeGroup;
	private ArrayAdapter<String> fuelTypeAdapter;
	private SparseArray<FuelType> fuelTypePositionModelMap;
	private ArrayAdapter<String> fuelTankAdapter;
	private SparseArray<FuelTank> fuelTankPositionModelMap;
	private AdapterView<?> currentlyClickedFuelTankTypeSpinner;
	private CheckBox chkSuspend;
	private EditText edtSuspendDate;
	private SimpleAnimator edtSuspendDateAnimator;

	private OnItemSelectedListener fuelTypeSelectedListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parentView,
				View selectedItemView, int position, long id) {
			// Check if the last item (Add Type) has been selected.
			int count = parentView.getCount();
			if (position + 1 == count) {
				currentlyClickedFuelTankTypeSpinner = parentView;
				InputDialogFragment.newInstance(DataDetailCarFragment.this,
						REQUEST_ADD_FUEL_TYPE,
						R.string.alert_add_fuel_type_title, null).show(
						getFragmentManager(), null);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parentView) {
		}
	};

	private OnItemSelectedListener fuelTankSelectedListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parentView,
				View selectedItemView, int position, long id) {
			// Check if the last item (Add Tank) has been selected.
			int count = parentView.getCount();
			if (position + 1 == count) {
				currentlyClickedFuelTankTypeSpinner = parentView;
				InputDialogFragment.newInstance(DataDetailCarFragment.this,
						REQUEST_ADD_FUEL_TANK,
						R.string.alert_add_fuel_tank_title, null).show(
						getFragmentManager(), null);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parentView) {
		}
	};

	private View.OnClickListener removePossibleFuelTypeListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			final View ftView = (View) v.getParent();

			SimpleAnimator animator = new SimpleAnimator(getActivity(), ftView,
					SimpleAnimator.Property.Height);
			animator.hide(null, new Runnable() {
				@Override
				public void run() {
					possibleFuelTypeGroup.removeView(ftView);
				}
			});
		}
	};

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		if (Car.getCount() == 1) {
			menu.removeItem(R.id.menu_delete);
		}
	}

	@Override
	public void onDialogNegativeClick(int requestCode) {
	}

	@Override
	public void onDialogPositiveClick(int requestCode, Date date) {
		if (requestCode == REQUEST_PICK_SUSPEND_DATE) {
			edtSuspendDate.setText(DateFormat.getDateFormat(getActivity())
					.format(date));
		}
	}

	@Override
	public void onDialogPositiveClick(int requestCode, int color) {
		if (requestCode == REQUEST_PICK_COLOR) {
			this.color = color;
			((GradientDrawable) colorIndicator.getBackground()).setColorFilter(
					color, PorterDuff.Mode.SRC);
		}
	}

	@Override
	public void onDialogPositiveClick(int requestCode, String input) {
		input = input.trim();
		if (requestCode == REQUEST_ADD_FUEL_TYPE) {
			if (!input.isEmpty()) {
				if (fuelTypeAdapter.getPosition(input) != AdapterView.INVALID_POSITION) {
					SupportMessageDialogFragment.newInstance(null, 0, null,
							getString(R.string.alert_fuel_type_exists_message),
							android.R.string.ok, null).show(
							getFragmentManager(), null);
				} else {
					fuelTypeAdapter.insert(input,
							fuelTypeAdapter.getCount() - 1);
					fuelTypePositionModelMap
							.put(fuelTypeAdapter.getCount() - 2, new FuelType(
									input));
					currentlyClickedFuelTankTypeSpinner
							.setSelection(fuelTypeAdapter.getCount() - 2);
				}
			}
		} else if (requestCode == REQUEST_ADD_FUEL_TANK) {
			if (!input.isEmpty()) {
				if (fuelTypeAdapter.getPosition(input) != AdapterView.INVALID_POSITION) {
					SupportMessageDialogFragment.newInstance(null, 0, null,
							getString(R.string.alert_fuel_tank_exists_message),
							android.R.string.ok, null).show(
							getFragmentManager(), null);
				} else {
					fuelTankAdapter.insert(input,
							fuelTankAdapter.getCount() - 1);
					fuelTankPositionModelMap.put(
							fuelTankAdapter.getCount() - 2, new FuelTank(null,
									input));
					currentlyClickedFuelTankTypeSpinner
							.setSelection(fuelTankAdapter.getCount() - 2);
				}
			}
		}
	}

	private View addPossibleFuelTypeView(FuelType fuelType, FuelTank fuelTank) {
		final View ftView = getActivity().getLayoutInflater().inflate(
				R.layout.row_possible_fuel_type, null);
		possibleFuelTypeGroup.addView(ftView);

		Spinner spnType = (Spinner) ftView.findViewById(R.id.spn_type);
		spnType.setAdapter(fuelTypeAdapter);
		spnType.setOnItemSelectedListener(fuelTypeSelectedListener);

		Spinner spnTank = (Spinner) ftView.findViewById(R.id.spn_tank);
		spnTank.setAdapter(fuelTankAdapter);
		spnTank.setOnItemSelectedListener(fuelTankSelectedListener);

		View btnRemove = ftView.findViewById(R.id.btn_remove);
		btnRemove.setOnClickListener(removePossibleFuelTypeListener);
		if (possibleFuelTypeGroup.getChildCount() == 1) {
			btnRemove.setVisibility(View.INVISIBLE);
		}

		if (fuelType != null) {
			int index = fuelTypePositionModelMap.indexOfValue(fuelType);
			spnType.setSelection(fuelTypePositionModelMap.keyAt(index));
		}

		if (fuelTank != null) {
			int index = fuelTankPositionModelMap.indexOfValue(fuelTank);
			spnTank.setSelection(fuelTankPositionModelMap.keyAt(index));
		}

		// The view has wrap_content as height and setting it to 48dp in
		// the layouts file doesn't change this, so we change it here.
		ftView.getLayoutParams().height = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 48, getResources()
						.getDisplayMetrics());
		SimpleAnimator animator = new SimpleAnimator(getActivity(), ftView,
				SimpleAnimator.Property.Height);
		ftView.setAlpha(0);
		ftView.getLayoutParams().height = 0;
		animator.show();

		return ftView;
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
	protected void fillFields(View v) {
		Date suspendDate = new Date();
		if (isInEditMode()) {
			Car car = (Car) editItem;

			edtName.setText(car.name);
			color = car.color;

			List<FuelTank> fuelTanks = car.fuelTanks();
			for (FuelTank fuelTank : fuelTanks) {
				fuelTankAdapter.insert(fuelTank.name,
						fuelTankAdapter.getCount() - 1);
				fuelTankPositionModelMap.append(fuelTankAdapter.getCount() - 2,
						fuelTank);

				List<FuelType> fuelTypes = fuelTank.fuelTypes();
				for (FuelType fuelType : fuelTypes) {
					addPossibleFuelTypeView(fuelType, fuelTank);
				}
			}

			chkSuspend.setChecked(car.isSuspended());
			if (car.isSuspended()) {
				suspendDate = car.suspendedSince;
			}
		} else {
			color = Color.BLUE;

			fuelTankAdapter.insert("Fuel", fuelTankAdapter.getCount() - 1);
			addPossibleFuelTypeView(null, null);
		}

		((GradientDrawable) colorIndicator.getBackground()).setColorFilter(
				color, PorterDuff.Mode.SRC);
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
	protected void initFields(View v) {
		edtName = (EditText) v.findViewById(R.id.edt_name);
		colorIndicator = v.findViewById(R.id.btn_color);
		possibleFuelTypeGroup = (ViewGroup) v
				.findViewById(R.id.layout_fueltypes);
		fuelTypeAdapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_dropdown_item);
		fuelTypePositionModelMap = new SparseArray<FuelType>();
		fuelTankAdapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_dropdown_item);
		fuelTankPositionModelMap = new SparseArray<FuelTank>();
		chkSuspend = (CheckBox) v.findViewById(R.id.chk_suspend);
		edtSuspendDate = (EditText) v.findViewById(R.id.edt_suspend_date);
		edtSuspendDateAnimator = new SimpleAnimator(getActivity(),
				edtSuspendDate, SimpleAnimator.Property.Height);

		View rowColor = (View) colorIndicator.getParent();
		rowColor.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ColorPickerDialogFragment.newInstance(
						DataDetailCarFragment.this, REQUEST_PICK_COLOR,
						R.string.alert_change_color_title, color).show(
						getFragmentManager(), null);
			}
		});

		List<FuelType> fuelTypes = FuelType.getAll();
		if (fuelTypes.size() > 0) {
			for (FuelType fuelType : fuelTypes) {
				fuelTypeAdapter.add(fuelType.name);
				fuelTypePositionModelMap.append(fuelTypeAdapter.getCount() - 1,
						fuelType);
			}
		} else {
			fuelTypeAdapter.add("Super");
		}
		fuelTypeAdapter.add(getString(R.string.label_add_dialog));

		fuelTankAdapter.add(getString(R.string.label_add_dialog));

		Button btnAddFuelType = (Button) v.findViewById(R.id.btn_add_fueltype);
		btnAddFuelType.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addPossibleFuelTypeView(null, null);
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

		edtSuspendDate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DatePickerDialogFragment.newInstance(
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
			for (int i = 0; i < possibleFuelTypeGroup.getChildCount(); i++) {
				View ftView = possibleFuelTypeGroup.getChildAt(i);

				int typePos = ((Spinner) ftView.findViewById(R.id.spn_type))
						.getSelectedItemPosition();
				int tankPos = ((Spinner) ftView.findViewById(R.id.spn_tank))
						.getSelectedItemPosition();

				FuelType type = fuelTypePositionModelMap.get(typePos);
				type.save();

				FuelTank tank = fuelTankPositionModelMap.get(tankPos);
				tank.car = car;
				tank.save();

				// Only create an association, when the same has not been
				// created before.
				if (addedPossibleTypes.add(type.getId() + "<>" + tank.getId())) {
					new PossibleFuelTypeForFuelTank(type, tank).save();
				}
			}

			// Clean up fuel types and tanks.
			FuelType.cleanUp();
			FuelTank.cleanUp();

			ActiveAndroid.setTransactionSuccessful();
		} finally {
			ActiveAndroid.endTransaction();
		}
	}

	@Override
	protected boolean validate() {
		FormValidator validator = new FormValidator();
		validator.add(new FormFieldNotEmptyValidator(edtName));
		return validator.validate();
	}
}
