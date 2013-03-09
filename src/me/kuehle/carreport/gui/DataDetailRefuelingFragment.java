/*
 * Copyright 2012 Jan Kühle
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

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.AbstractItem;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.FuelType;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.util.gui.DatePickerDialogFragment;
import me.kuehle.carreport.util.gui.DatePickerDialogFragment.DatePickerDialogFragmentListener;
import me.kuehle.carreport.util.gui.InputFieldValidator;
import me.kuehle.carreport.util.gui.TimePickerDialogFragment;
import me.kuehle.carreport.util.gui.TimePickerDialogFragment.TimePickerDialogFragmentListener;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class DataDetailRefuelingFragment extends AbstractDataDetailFragment
		implements InputFieldValidator.ValidationCallback,
		DatePickerDialogFragmentListener, TimePickerDialogFragmentListener {
	private static final int PICK_DATE_REQUEST_CODE = 0;
	private static final int PICK_TIME_REQUEST_CODE = 1;

	public static DataDetailRefuelingFragment newInstance(int id) {
		DataDetailRefuelingFragment f = new DataDetailRefuelingFragment();

		Bundle args = new Bundle();
		args.putInt(AbstractDataDetailFragment.EXTRA_ID, id);
		f.setArguments(args);

		return f;
	}

	private EditText edtDate;
	private EditText edtTime;
	private EditText edtMileage;
	private EditText edtVolume;
	private CheckBox chkPartial;
	private EditText edtPrice;
	private Spinner spnFuelType;
	private EditText edtNote;
	private Spinner spnCar;

	private Car[] cars;
	private FuelType[] fuelTypes;

	@Override
	public void onDialogPositiveClick(int requestCode, Date date) {
		if (requestCode == PICK_DATE_REQUEST_CODE) {
			edtDate.setText(DateFormat.getDateFormat(getActivity())
					.format(date));
		} else if (requestCode == PICK_TIME_REQUEST_CODE) {
			edtTime.setText(DateFormat.getTimeFormat(getActivity())
					.format(date));
		}
	}

	@Override
	public void validationSuccessfull() {
		Date date = getDateTime(getDate(), getTime());
		int mileage = getIntegerFromEditText(edtMileage, 0);
		float volume = (float) getDoubleFromEditText(edtVolume, 0);
		boolean partial = chkPartial.isChecked();
		float price = (float) getDoubleFromEditText(edtPrice, 0);
		FuelType fuelType = fuelTypes.length == 0 ? null
				: fuelTypes[spnFuelType.getSelectedItemPosition()];
		String note = edtNote.getText().toString().trim();
		Car car = cars[spnCar.getSelectedItemPosition()];

		if (!isInEditMode()) {
			Refueling.create(date, mileage, volume, price, partial, note, car,
					fuelType);
		} else {
			Refueling refueling = (Refueling) editItem;
			refueling.setDate(date);
			refueling.setMileage(mileage);
			refueling.setVolume(volume);
			refueling.setPrice(price);
			refueling.setPartial(partial);
			refueling.setNote(note);
			refueling.setCar(car);
			refueling.setFuelType(fuelType);
			refueling.save();
		}

		saveSuccess();
	}

	private Date getDate() {
		try {
			return DateFormat.getDateFormat(getActivity()).parse(
					edtDate.getText().toString());
		} catch (ParseException e) {
			return new Date();
		}
	}

	private Date getTime() {
		try {
			return DateFormat.getTimeFormat(getActivity()).parse(
					edtTime.getText().toString());
		} catch (ParseException e) {
			return new Date();
		}
	}

	@Override
	protected void fillFields(View v) {
		if (!isInEditMode()) {
			Preferences prefs = new Preferences(getActivity());

			edtDate.setText(DateFormat.getDateFormat(getActivity()).format(
					new Date()));
			edtTime.setText(DateFormat.getTimeFormat(getActivity()).format(
					new Date()));

			int selectCar = getArguments().getInt(EXTRA_CAR_ID);
			if (selectCar == 0) {
				selectCar = prefs.getDefaultCar();
			}
			for (int pos = 0; pos < cars.length; pos++) {
				if (cars[pos].getId() == selectCar) {
					spnCar.setSelection(pos);
				}
			}
		} else {
			Refueling refueling = (Refueling) editItem;

			edtDate.setText(DateFormat.getDateFormat(getActivity()).format(
					refueling.getDate()));
			edtTime.setText(DateFormat.getTimeFormat(getActivity()).format(
					refueling.getDate()));
			edtMileage.setText(String.valueOf(refueling.getMileage()));
			edtVolume.setText(String.valueOf(refueling.getVolume()));
			chkPartial.setChecked(refueling.isPartial());
			edtPrice.setText(String.valueOf(refueling.getPrice()));
			edtNote.setText(refueling.getNote());

			for (int pos = 0; pos < cars.length; pos++) {
				if (cars[pos].getId() == refueling.getCar().getId()) {
					spnCar.setSelection(pos);
				}
			}
		}
	}

	@Override
	protected int getAlertDeleteMessage() {
		return R.string.alert_delete_refueling_message;
	}

	@Override
	protected AbstractItem getEditObject(int id) {
		return new Refueling(id);
	}

	@Override
	protected int getLayout() {
		return R.layout.fragment_data_detail_refueling;
	}

	@Override
	protected int getTitleForEdit() {
		return R.string.title_edit_refueling;
	}

	@Override
	protected int getTitleForNew() {
		return R.string.title_add_refueling;
	}

	@Override
	protected int getToastDeletedMessage() {
		return R.string.toast_refueling_deleted;
	}

	@Override
	protected int getToastSavedMessage() {
		return R.string.toast_refueling_saved;
	}

	@Override
	protected void initFields(View v) {
		Preferences prefs = new Preferences(getActivity());

		edtDate = (EditText) v.findViewById(R.id.edt_date);
		edtTime = (EditText) v.findViewById(R.id.edt_time);
		edtMileage = (EditText) v.findViewById(R.id.edt_mileage);
		edtVolume = (EditText) v.findViewById(R.id.edt_volume);
		chkPartial = (CheckBox) v.findViewById(R.id.chk_partial);
		edtPrice = (EditText) v.findViewById(R.id.edt_price);
		spnFuelType = (Spinner) v.findViewById(R.id.spn_fueltype);
		edtNote = (EditText) v.findViewById(R.id.edt_note);
		spnCar = (Spinner) v.findViewById(R.id.spn_car);

		edtDate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DatePickerDialogFragment.newInstance(
						DataDetailRefuelingFragment.this,
						PICK_DATE_REQUEST_CODE, getDate()).show(
						getFragmentManager(), null);
			}
		});

		edtTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TimePickerDialogFragment.newInstance(
						DataDetailRefuelingFragment.this,
						PICK_TIME_REQUEST_CODE, getTime()).show(
						getFragmentManager(), null);
			}
		});

		((TextView) v.findViewById(R.id.txt_unit_currency)).setText(prefs
				.getUnitCurrency());
		((TextView) v.findViewById(R.id.txt_unit_distance)).setText(prefs
				.getUnitDistance());
		((TextView) v.findViewById(R.id.txt_unit_volume)).setText(prefs
				.getUnitVolume());

		ArrayAdapter<String> carAdapter = new ArrayAdapter<String>(
				getActivity(), android.R.layout.simple_spinner_dropdown_item);
		cars = Car.getAll();
		for (Car car : cars) {
			carAdapter.add(car.getName());
		}
		spnCar.setAdapter(carAdapter);

		spnCar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView,
					View selectedItemView, int position, long id) {
				Car car = cars[position];
				fuelTypes = FuelType.getAllForCar(car);

				if (fuelTypes.length > 0) {
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(
							getActivity(),
							android.R.layout.simple_spinner_dropdown_item);
					for (FuelType fuelType : fuelTypes) {
						adapter.add(fuelType.getName());
					}

					spnFuelType.setAdapter(adapter);
					spnFuelType.setVisibility(View.VISIBLE);
				} else {
					spnFuelType.setVisibility(View.GONE);
				}

				if (isInEditMode()) {
					Refueling refueling = (Refueling) editItem;
					if (refueling.getFuelType() != null) {
						for (int pos = 0; pos < fuelTypes.length; pos++) {
							if (fuelTypes[pos].getId() == refueling
									.getFuelType().getId()) {
								spnFuelType.setSelection(pos);
							}
						}
					}
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}
		});
	}

	@Override
	protected void save() {
		InputFieldValidator validator = new InputFieldValidator(getActivity(),
				getFragmentManager(), this);

		validator.add(edtMileage,
				InputFieldValidator.ValidationType.GreaterZero,
				R.string.hint_mileage);
		validator.add(edtVolume,
				InputFieldValidator.ValidationType.GreaterZero,
				R.string.hint_volume);
		validator.add(edtPrice, InputFieldValidator.ValidationType.GreaterZero,
				R.string.hint_price);

		validator.validate();
	}
}