/*
 * Copyright 2012 Jan K�hle
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
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.util.Recurrence;
import me.kuehle.carreport.util.RecurrenceInterval;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class EditOtherCostFragment extends AbstractEditFragment implements
		InputFieldValidator.ValidationCallback {
	public static final String EXTRA_CAR_ID = "car_id";

	private EditTextDateField edtDate;
	private EditTextTimeField edtTime;

	private Car[] cars;

	@Override
	protected int getLayout() {
		return R.layout.edit_other;
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
	protected int getAlertDeleteMessage() {
		return R.string.alert_delete_other_message;
	}

	@Override
	protected int getToastSavedMessage() {
		return R.string.toast_other_saved;
	}

	@Override
	protected int getToastDeletedMessage() {
		return R.string.toast_other_deleted;
	}

	@Override
	protected AbstractItem getEditObject(int id) {
		return new OtherCost(id);
	}

	@Override
	protected void initFields() {
		Preferences prefs = new Preferences(getActivity());

		ArrayAdapter<String> titleAdapter = new ArrayAdapter<String>(
				getActivity(), android.R.layout.simple_dropdown_item_1line,
				OtherCost.getAllTitles());
		AutoCompleteTextView edtTitle = (AutoCompleteTextView) getView()
				.findViewById(R.id.edtTitle);
		edtTitle.setAdapter(titleAdapter);

		edtDate = new EditTextDateField(R.id.edtDate);
		edtTime = new EditTextTimeField(R.id.edtTime);

		((TextView) getView().findViewById(R.id.txtUnitCurrency)).setText(prefs
				.getUnitCurrency());
		((TextView) getView().findViewById(R.id.txtUnitDistance)).setText(prefs
				.getUnitDistance());

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
			edtTime.setTime(new Date());

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
			OtherCost other = (OtherCost) editItem;

			edtDate.setDate(other.getDate());
			edtTime.setTime(other.getDate());

			EditText edtTitle = ((EditText) getView().findViewById(
					R.id.edtTitle));
			edtTitle.setText(String.valueOf(other.getTitle()));

			EditText edtMileage = ((EditText) getView().findViewById(
					R.id.edtMileage));
			if (other.getMileage() > -1) {
				edtMileage.setText(String.valueOf(other.getMileage()));
			}

			EditText edtPrice = ((EditText) getView().findViewById(
					R.id.edtPrice));
			edtPrice.setText(String.valueOf(other.getPrice()));

			Spinner spnRepeat = ((Spinner) getView().findViewById(
					R.id.spnRepeat));
			spnRepeat.setSelection(other.getRecurrence().getInterval()
					.getValue());

			EditText edtNote = ((EditText) getView().findViewById(R.id.edtNote));
			edtNote.setText(other.getNote());

			Spinner spnCar = ((Spinner) getView().findViewById(R.id.spnCar));
			for (int pos = 0; pos < cars.length; pos++) {
				if (cars[pos].getId() == other.getCar().getId()) {
					spnCar.setSelection(pos);
				}
			}
		}
	}

	@Override
	protected void save() {
		InputFieldValidator validator = new InputFieldValidator(getActivity(),
				this);

		validator.setRecommended(getView().findViewById(R.id.edtTitle),
				InputFieldValidator.ValidationType.NotEmpty,
				R.string.alert_validate_recommend_f_title,
				R.string.alert_validate_recommend_f_title_msg);
		validator.addRequired(getView().findViewById(R.id.edtPrice),
				InputFieldValidator.ValidationType.GreaterZero,
				R.string.alert_validate_required_f_price);

		validator.validate();
	}

	@Override
	public void validationSuccessfull() {
		String title = ((EditText) getView().findViewById(R.id.edtTitle))
				.getText().toString().trim();
		Date date = getDateTime(edtDate.getDate(), edtTime.getTime());
		int mileage = getIntegerFromEditText(R.id.edtMileage, -1);
		float price = (float) getDoubleFromEditText(R.id.edtPrice, 0);
		RecurrenceInterval repInterval = RecurrenceInterval
				.getByValue((int) ((Spinner) getView().findViewById(
						R.id.spnRepeat)).getSelectedItemId());
		Recurrence recurrence = new Recurrence(repInterval);
		String note = ((EditText) getView().findViewById(R.id.edtNote))
				.getText().toString().trim();
		Car car = cars[(int) ((Spinner) getView().findViewById(R.id.spnCar))
				.getSelectedItemId()];

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
		}

		saveSuccess();
	}

	public static EditOtherCostFragment newInstance(int id) {
		EditOtherCostFragment f = new EditOtherCostFragment();

		Bundle args = new Bundle();
		args.putInt(AbstractEditFragment.EXTRA_ID, id);
		f.setArguments(args);

		return f;
	}
}