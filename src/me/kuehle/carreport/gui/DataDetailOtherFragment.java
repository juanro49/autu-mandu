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
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.util.Recurrence;
import me.kuehle.carreport.util.RecurrenceInterval;
import me.kuehle.carreport.util.gui.DatePickerDialogFragment;
import me.kuehle.carreport.util.gui.DatePickerDialogFragment.DatePickerDialogFragmentListener;
import me.kuehle.carreport.util.gui.InputFieldValidator;
import me.kuehle.carreport.util.gui.TimePickerDialogFragment;
import me.kuehle.carreport.util.gui.TimePickerDialogFragment.TimePickerDialogFragmentListener;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class DataDetailOtherFragment extends AbstractDataDetailFragment
		implements InputFieldValidator.ValidationCallback,
		DatePickerDialogFragmentListener, TimePickerDialogFragmentListener {
	private static final int PICK_DATE_REQUEST_CODE = 0;
	private static final int PICK_TIME_REQUEST_CODE = 1;

	public static DataDetailOtherFragment newInstance(int id) {
		DataDetailOtherFragment f = new DataDetailOtherFragment();

		Bundle args = new Bundle();
		args.putInt(AbstractDataDetailFragment.EXTRA_ID, id);
		f.setArguments(args);

		return f;
	}

	private AutoCompleteTextView edtTitle;
	private EditText edtDate;
	private EditText edtTime;
	private EditText edtMileage;
	private EditText edtPrice;
	private Spinner spnRepeat;
	private EditText edtNote;
	private Spinner spnCar;

	private Car[] cars;

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
		String title = edtTitle.getText().toString().trim();
		Date date = getDateTime(getDate(), getTime());
		int mileage = getIntegerFromEditText(edtMileage, -1);
		float price = (float) getDoubleFromEditText(edtPrice, 0);
		RecurrenceInterval repInterval = RecurrenceInterval
				.getByValue(spnRepeat.getSelectedItemPosition());
		Recurrence recurrence = new Recurrence(repInterval);
		String note = edtNote.getText().toString().trim();
		Car car = cars[spnCar.getSelectedItemPosition()];

		if (!isInEditMode()) {
			OtherCost
					.create(title, date, mileage, price, recurrence, note, car);
		} else {
			OtherCost other = (OtherCost) editItem;
			other.setTitle(title);
			other.setDate(date);
			other.setMileage(mileage);
			other.setPrice(price);
			other.setRecurrence(recurrence);
			other.setNote(note);
			other.setCar(car);
			other.save();
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
			OtherCost other = (OtherCost) editItem;

			edtDate.setText(DateFormat.getDateFormat(getActivity()).format(
					other.getDate()));
			edtTime.setText(DateFormat.getTimeFormat(getActivity()).format(
					other.getDate()));
			edtTitle.setText(String.valueOf(other.getTitle()));
			if (other.getMileage() > -1) {
				edtMileage.setText(String.valueOf(other.getMileage()));
			}
			edtPrice.setText(String.valueOf(other.getPrice()));
			spnRepeat.setSelection(other.getRecurrence().getInterval()
					.getValue());
			edtNote.setText(other.getNote());

			for (int pos = 0; pos < cars.length; pos++) {
				if (cars[pos].getId() == other.getCar().getId()) {
					spnCar.setSelection(pos);
				}
			}
		}
	}

	@Override
	protected int getAlertDeleteMessage() {
		return R.string.alert_delete_other_message;
	}

	@Override
	protected AbstractItem getEditObject(int id) {
		return new OtherCost(id);
	}

	@Override
	protected int getLayout() {
		return R.layout.fragment_data_detail_other;
	}

	@Override
	protected int getTitleForEdit() {
		return R.string.title_edit_other;
	}

	@Override
	protected int getTitleForNew() {
		return R.string.title_add_other;
	}

	@Override
	protected int getToastDeletedMessage() {
		return R.string.toast_other_deleted;
	}

	@Override
	protected int getToastSavedMessage() {
		return R.string.toast_other_saved;
	}

	@Override
	protected void initFields(View v) {
		Preferences prefs = new Preferences(getActivity());

		edtTitle = (AutoCompleteTextView) v.findViewById(R.id.edt_title);
		edtDate = (EditText) v.findViewById(R.id.edt_date);
		edtTime = (EditText) v.findViewById(R.id.edt_time);
		edtMileage = (EditText) v.findViewById(R.id.edt_mileage);
		edtPrice = (EditText) v.findViewById(R.id.edt_price);
		spnRepeat = (Spinner) v.findViewById(R.id.spn_repeat);
		edtNote = (EditText) v.findViewById(R.id.edt_note);
		spnCar = (Spinner) v.findViewById(R.id.spn_car);

		ArrayAdapter<String> titleAdapter = new ArrayAdapter<String>(
				getActivity(), android.R.layout.simple_dropdown_item_1line,
				OtherCost.getAllTitles());
		edtTitle.setAdapter(titleAdapter);

		edtDate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DatePickerDialogFragment.newInstance(
						DataDetailOtherFragment.this, PICK_DATE_REQUEST_CODE,
						getDate()).show(getFragmentManager(), null);
			}
		});

		edtTime.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TimePickerDialogFragment.newInstance(
						DataDetailOtherFragment.this, PICK_TIME_REQUEST_CODE,
						getTime()).show(getFragmentManager(), null);
			}
		});

		((TextView) v.findViewById(R.id.txt_unit_currency)).setText(prefs
				.getUnitCurrency());
		((TextView) v.findViewById(R.id.txt_unit_distance)).setText(prefs
				.getUnitDistance());

		ArrayAdapter<String> carAdapter = new ArrayAdapter<String>(
				getActivity(), android.R.layout.simple_spinner_dropdown_item);
		cars = Car.getAll();
		for (Car car : cars) {
			carAdapter.add(car.getName());
		}
		spnCar.setAdapter(carAdapter);
	}

	@Override
	protected void save() {
		InputFieldValidator validator = new InputFieldValidator(getActivity(),
				getFragmentManager(), this);

		validator.add(edtPrice, InputFieldValidator.ValidationType.GreaterZero,
				R.string.hint_price);

		validator.validate();
	}
}