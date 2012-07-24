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

package me.kuehle.carreport;

import java.util.Date;

import me.kuehle.carreport.db.AbstractItem;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Refueling;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class EditRefuelingFragment extends AbstractEditFragment {
	public static final String EXTRA_CAR_ID = "car_id";
	
	private EditTextDateField edtDate;

	private Car[] cars;

	@Override
	protected int getLayout() {
		return R.layout.edit_refueling;
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
	protected int getAlertDeleteMessage() {
		return R.string.alert_delete_refueling_message;
	}

	@Override
	protected AbstractItem getEditObject(int id) {
		return new Refueling(id);
	}

	@Override
	protected int getToastSavedMessage() {
		return R.string.toast_refueling_saved;
	}

	@Override
	protected int getToastDeletedMessage() {
		return R.string.toast_refueling_deleted;
	}

	@Override
	protected void initFields() {
		Preferences prefs = new Preferences(getActivity());

		edtDate = new EditTextDateField(R.id.edtDate);

		((TextView) getView().findViewById(R.id.txtUnitCurrency)).setText(prefs
				.getUnitCurrency());
		((TextView) getView().findViewById(R.id.txtUnitDistance)).setText(prefs
				.getUnitDistance());
		((TextView) getView().findViewById(R.id.txtUnitVolume)).setText(prefs
				.getUnitVolume());

		ArrayAdapter<String> carAdapter = new ArrayAdapter<String>(
				getActivity(), android.R.layout.simple_spinner_dropdown_item);
		cars = Car.getAll();
		for (Car car : cars) {
			carAdapter.add(car.getName());
		}
		Spinner spnCar = (Spinner) getView().findViewById(R.id.spnCar);
		spnCar.setAdapter(carAdapter);
	}

	@Override
	protected void fillFields() {
		if (!isInEditMode()) {
			Preferences prefs = new Preferences(getActivity());
			
			edtDate.setDate(new Date());

			Spinner spnCar = ((Spinner) getView().findViewById(R.id.spnCar));
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

			edtDate.setDate(refueling.getDate());

			EditText edtTachometer = ((EditText) getView().findViewById(
					R.id.edtTachometer));
			edtTachometer.setText(String.valueOf(refueling.getTachometer()));

			EditText edtVolume = ((EditText) getView().findViewById(
					R.id.edtVolume));
			edtVolume.setText(String.valueOf(refueling.getVolume()));

			CheckBox chkPartial = ((CheckBox) getView().findViewById(
					R.id.chkPartial));
			chkPartial.setChecked(refueling.isPartial());

			EditText edtPrice = ((EditText) getView().findViewById(
					R.id.edtPrice));
			edtPrice.setText(String.valueOf(refueling.getPrice()));

			EditText edtNote = ((EditText) getView().findViewById(R.id.edtNote));
			edtNote.setText(refueling.getNote());

			Spinner spnCar = ((Spinner) getView().findViewById(R.id.spnCar));
			for (int pos = 0; pos < cars.length; pos++) {
				if (cars[pos].getId() == refueling.getCar().getId()) {
					spnCar.setSelection(pos);
				}
			}
		}
	}

	@Override
	protected boolean save() {
		Date date = edtDate.getDate();
		int tachometer = getIntegerFromEditText(R.id.edtTachometer, 0);
		float volume = (float) getDoubleFromEditText(R.id.edtVolume, 0);
		boolean partial = ((CheckBox) getView().findViewById(R.id.chkPartial))
				.isChecked();
		float price = (float) getDoubleFromEditText(R.id.edtPrice, 0);
		String note = ((EditText) getView().findViewById(R.id.edtNote))
				.getText().toString();
		Car car = cars[(int) ((Spinner) getView().findViewById(R.id.spnCar))
				.getSelectedItemId()];
		if (tachometer > 0 && volume > 0 && price > 0) {
			if (!isInEditMode()) {
				Refueling.create(date, tachometer, volume, price, partial,
						note, car);
			} else {
				Refueling refueling = (Refueling) editItem;
				refueling.setDate(date);
				refueling.setTachometer(tachometer);
				refueling.setVolume(volume);
				refueling.setPrice(price);
				refueling.setPartial(partial);
				refueling.setNote(note);
				refueling.setCar(car);
			}
			return true;
		} else {
			return false;
		}
	}

	public static EditRefuelingFragment newInstance(int id) {
		EditRefuelingFragment f = new EditRefuelingFragment();

		Bundle args = new Bundle();
		args.putInt(AbstractEditFragment.EXTRA_ID, id);
		f.setArguments(args);

		return f;
	}
}