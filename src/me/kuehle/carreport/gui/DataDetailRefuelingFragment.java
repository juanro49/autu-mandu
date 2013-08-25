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
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.FuelTank;
import me.kuehle.carreport.db.FuelType;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.gui.dialog.DatePickerDialogFragment;
import me.kuehle.carreport.gui.dialog.DatePickerDialogFragment.DatePickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.TimePickerDialogFragment;
import me.kuehle.carreport.gui.dialog.TimePickerDialogFragment.TimePickerDialogFragmentListener;
import me.kuehle.carreport.gui.util.FormFieldGreaterZeroValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.activeandroid.Model;

public class DataDetailRefuelingFragment extends AbstractDataDetailFragment
		implements DatePickerDialogFragmentListener,
		TimePickerDialogFragmentListener {
	private static final int PICK_DATE_REQUEST_CODE = 0;
	private static final int PICK_TIME_REQUEST_CODE = 1;

	public static DataDetailRefuelingFragment newInstance(long id,
			boolean allowCancel) {
		DataDetailRefuelingFragment f = new DataDetailRefuelingFragment();

		Bundle args = new Bundle();
		args.putLong(AbstractDataDetailFragment.EXTRA_ID, id);
		args.putBoolean(AbstractDataDetailFragment.EXTRA_ALLOW_CANCEL,
				allowCancel);
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

	private List<Car> cars;
	private SparseArray<Long[]> fuelTypePositionIDMap;

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

			long selectCar = getArguments().getLong(EXTRA_CAR_ID);
			if (selectCar == 0) {
				selectCar = prefs.getDefaultCar();
			}
			for (int pos = 0; pos < cars.size(); pos++) {
				if (cars.get(pos).getId() == selectCar) {
					spnCar.setSelection(pos);
				}
			}
		} else {
			Refueling refueling = (Refueling) editItem;

			edtDate.setText(DateFormat.getDateFormat(getActivity()).format(
					refueling.date));
			edtTime.setText(DateFormat.getTimeFormat(getActivity()).format(
					refueling.date));
			edtMileage.setText(String.valueOf(refueling.mileage));
			edtVolume.setText(String.valueOf(refueling.volume));
			chkPartial.setChecked(refueling.partial);
			edtPrice.setText(String.valueOf(refueling.price));
			edtNote.setText(refueling.note);

			for (int pos = 0; pos < cars.size(); pos++) {
				if (cars.get(pos).getId() == refueling.fuelTank.car.getId()) {
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
	protected Model getEditItem(long id) {
		return Refueling.load(Refueling.class, id);
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
		spnFuelType = (Spinner) v.findViewById(R.id.spn_fuel_type);
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
			carAdapter.add(car.name);
		}
		spnCar.setAdapter(carAdapter);

		spnCar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView,
					View selectedItemView, int position, long id) {
				Car car = cars.get(position);

				ArrayAdapter<String> adapter = new ArrayAdapter<String>(
						getActivity(),
						android.R.layout.simple_spinner_dropdown_item);
				fuelTypePositionIDMap = new SparseArray<Long[]>();

				List<FuelTank> fuelTanks = car.fuelTanks();
				for (FuelTank fuelTank : fuelTanks) {
					List<FuelType> fuelTypes = fuelTank.fuelTypes();
					for (FuelType fuelType : fuelTypes) {
						adapter.add(String.format("%s (%s)", fuelType.name,
								fuelTank.name));
						fuelTypePositionIDMap.append(
								adapter.getCount() - 1,
								new Long[] { fuelType.getId(), fuelTank.getId() });
					}
				}

				spnFuelType.setAdapter(adapter);

				if (isInEditMode()) {
					Refueling refueling = (Refueling) editItem;
					for (int i = 0; i < fuelTypePositionIDMap.size(); i++) {
						Long[] ids = fuelTypePositionIDMap.valueAt(i);
						if (refueling.fuelType.getId() == ids[0]
								&& refueling.fuelTank.getId() == ids[1]) {
							spnFuelType.setSelection(fuelTypePositionIDMap
									.keyAt(i));
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
	protected boolean validate() {
		FormValidator validator = new FormValidator();
		validator.add(new FormFieldGreaterZeroValidator(edtMileage));
		validator.add(new FormFieldGreaterZeroValidator(edtVolume));
		validator.add(new FormFieldGreaterZeroValidator(edtPrice));
		return validator.validate();
	}

	@Override
	protected void save() {
		Date date = getDateTime(getDate(), getTime());
		int mileage = getIntegerFromEditText(edtMileage, 0);
		float volume = (float) getDoubleFromEditText(edtVolume, 0);
		boolean partial = chkPartial.isChecked();
		float price = (float) getDoubleFromEditText(edtPrice, 0);
		Long[] fuelIDs = fuelTypePositionIDMap.get(spnFuelType
				.getSelectedItemPosition());
		FuelType fuelType = FuelType.load(FuelType.class, fuelIDs[0]);
		FuelTank fuelTank = FuelTank.load(FuelTank.class, fuelIDs[1]);
		String note = edtNote.getText().toString().trim();

		if (!isInEditMode()) {
			new Refueling(date, mileage, volume, price, partial, note,
					fuelType, fuelTank).save();
		} else {
			Refueling refueling = (Refueling) editItem;
			refueling.date = date;
			refueling.mileage = mileage;
			refueling.volume = volume;
			refueling.price = price;
			refueling.partial = partial;
			refueling.note = note;
			refueling.fuelType = fuelType;
			refueling.fuelTank = fuelTank;
			refueling.save();
		}
	}
}