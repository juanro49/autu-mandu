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
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.gui.dialog.DatePickerDialogFragment;
import me.kuehle.carreport.gui.dialog.DatePickerDialogFragment.DatePickerDialogFragmentListener;
import me.kuehle.carreport.gui.dialog.TimePickerDialogFragment;
import me.kuehle.carreport.gui.dialog.TimePickerDialogFragment.TimePickerDialogFragmentListener;
import me.kuehle.carreport.gui.util.FormFieldGreaterZeroValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.util.Recurrence;
import me.kuehle.carreport.util.RecurrenceInterval;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.activeandroid.Model;

public class DataDetailOtherFragment extends AbstractDataDetailFragment
		implements DatePickerDialogFragmentListener,
		TimePickerDialogFragmentListener {
	private static final int PICK_DATE_REQUEST_CODE = 0;
	private static final int PICK_TIME_REQUEST_CODE = 1;

	public static DataDetailOtherFragment newInstance(long id,
			boolean allowCancel) {
		DataDetailOtherFragment f = new DataDetailOtherFragment();

		Bundle args = new Bundle();
		args.putLong(AbstractDataDetailFragment.EXTRA_ID, id);
		args.putBoolean(AbstractDataDetailFragment.EXTRA_ALLOW_CANCEL,
				allowCancel);
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

	private List<Car> cars;

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
			OtherCost other = (OtherCost) editItem;

			edtDate.setText(DateFormat.getDateFormat(getActivity()).format(
					other.date));
			edtTime.setText(DateFormat.getTimeFormat(getActivity()).format(
					other.date));
			edtTitle.setText(String.valueOf(other.title));
			if (other.mileage > -1) {
				edtMileage.setText(String.valueOf(other.mileage));
			}
			edtPrice.setText(String.valueOf(other.price));
			spnRepeat.setSelection(other.recurrence.getInterval().getValue());
			edtNote.setText(other.note);

			for (int pos = 0; pos < cars.size(); pos++) {
				if (cars.get(pos).getId() == other.car.getId()) {
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
	protected Model getEditItem(long id) {
		return OtherCost.load(OtherCost.class, id);
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
			carAdapter.add(car.name);
		}
		spnCar.setAdapter(carAdapter);
	}

	@Override
	protected boolean validate() {
		FormValidator validator = new FormValidator();
		validator.add(new FormFieldGreaterZeroValidator(edtPrice));
		return validator.validate();
	}

	@Override
	protected void save() {
		String title = edtTitle.getText().toString().trim();
		Date date = getDateTime(getDate(), getTime());
		int mileage = getIntegerFromEditText(edtMileage, -1);
		float price = (float) getDoubleFromEditText(edtPrice, 0);
		RecurrenceInterval repInterval = RecurrenceInterval
				.getByValue(spnRepeat.getSelectedItemPosition());
		Recurrence recurrence = new Recurrence(repInterval);
		String note = edtNote.getText().toString().trim();
		Car car = cars.get(spnCar.getSelectedItemPosition());

		if (!isInEditMode()) {
			new OtherCost(title, date, mileage, price, recurrence, note, car)
					.save();
		} else {
			OtherCost other = (OtherCost) editItem;
			other.title = title;
			other.date = date;
			other.mileage = mileage;
			other.price = price;
			other.recurrence = recurrence;
			other.note = note;
			other.car = car;
			other.save();
		}
	}
}