package me.kuehle.carreport;

import java.util.Date;

import me.kuehle.carreport.db.AbstractItem;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Refueling;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class EditRefuelingFragment extends AbstractEditFragment {
	private EditTextDateField edtDate;

	private Car[] cars;

	@Override
	protected int getLayout() {
		return R.layout.edit_refueling;
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
		int defaultCar = prefs.getDefaultCar();
		for (int pos = 0; pos < cars.length; pos++) {
			if (cars[pos].getId() == defaultCar) {
				spnCar.setSelection(pos);
			}
		}
	}

	@Override
	protected void fillFields() {
		if (!isInEditMode()) {
			edtDate.setDate(new Date());

			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(getActivity()
							.getApplicationContext());
			int defaultCar = Integer.parseInt(prefs.getString("default_car",
					"1"));
			Spinner spnCar = ((Spinner) getView().findViewById(R.id.spnCar));
			for (int pos = 0; pos < cars.length; pos++) {
				if (cars[pos].getId() == defaultCar) {
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
		int tachometer = getIntFromEditText(R.id.edtTachometer, 0);
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