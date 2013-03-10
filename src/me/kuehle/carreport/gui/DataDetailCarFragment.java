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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import me.kuehle.carreport.R;
import me.kuehle.carreport.db.AbstractItem;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.FuelType;
import me.kuehle.carreport.util.gui.ColorPickerDialogFragment;
import me.kuehle.carreport.util.gui.ColorPickerDialogFragment.ColorPickerDialogFragmentListener;
import me.kuehle.carreport.util.gui.DatePickerDialogFragment;
import me.kuehle.carreport.util.gui.DatePickerDialogFragment.DatePickerDialogFragmentListener;
import me.kuehle.carreport.util.gui.InputFieldValidator;
import me.kuehle.carreport.util.gui.InputFieldValidator.ValidationCallback;
import me.kuehle.carreport.util.gui.SimpleAnimator;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;

public class DataDetailCarFragment extends AbstractDataDetailFragment implements
		ColorPickerDialogFragmentListener, ValidationCallback,
		DatePickerDialogFragmentListener {
	private static final int PICK_COLOR_REQUEST_CODE = 1;
	private static final int PICK_SUSPEND_DATE_REQUEST_CODE = 2;

	private EditText edtName;
	private View colorIndicator;
	private int color;
	private ViewGroup fuelTypeGroup;
	private ArrayList<FuelType> fuelTypeRemovals = new ArrayList<FuelType>();
	private HashMap<View, FuelType> fuelTypeInputMappings = new HashMap<View, FuelType>();
	private ArrayAdapter<String> fuelTypeNameAdapter;
	private ArrayAdapter<String> fuelTypeTankAdapter;
	private CheckBox chkSuspend;
	private EditText edtSuspendDate;
	private SimpleAnimator edtSuspendDateAnimator;

	private OnItemSelectedListener tankSelectedListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> parentView,
				View selectedItemView, int position, long id) {
			int count = parentView.getCount();
			if (position + 1 == count) {
				int tank = count;
				fuelTypeTankAdapter.insert(
						getString(R.string.label_tank, tank), tank - 1);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> parentView) {
		}
	};

	private View.OnClickListener removeFuelTypeListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			final View ftView = (View) v.getParent();

			FuelType fuelType = fuelTypeInputMappings.get(ftView);
			fuelTypeInputMappings.remove(ftView);
			if (fuelType != null) {
				fuelTypeRemovals.add(fuelType);
			}

			SimpleAnimator animator = new SimpleAnimator(getActivity(), ftView,
					SimpleAnimator.Property.Height);
			animator.hide(null, new Runnable() {
				@Override
				public void run() {
					fuelTypeGroup.removeView(ftView);
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
		if (requestCode == PICK_SUSPEND_DATE_REQUEST_CODE) {
			edtSuspendDate.setText(DateFormat.getDateFormat(getActivity())
					.format(date));
		}
	}

	@Override
	public void onDialogPositiveClick(int requestCode, int color) {
		if (requestCode == PICK_COLOR_REQUEST_CODE) {
			this.color = color;
			((GradientDrawable) colorIndicator.getBackground()).setColorFilter(
					color, PorterDuff.Mode.SRC);
		}
	}

	@Override
	public void validationSuccessfull() {
		String name = edtName.getText().toString();
		Date suspended = null;
		if (chkSuspend.isChecked()) {
			suspended = getSuspendDate();
		}

		Car car;
		if (!isInEditMode()) {
			car = Car.create(name, color, suspended);
		} else {
			car = (Car) editItem;
			car.setName(name);
			car.setColor(color);
			car.setSuspended(suspended);
			car.save();
		}

		Set<View> ftViews = fuelTypeInputMappings.keySet();
		for (View ftView : ftViews) {
			name = ((EditText) ftView.findViewById(R.id.edt_name)).getText()
					.toString().trim();
			int tank = ((Spinner) ftView.findViewById(R.id.spn_tank))
					.getSelectedItemPosition() + 1;

			FuelType fuelType = fuelTypeInputMappings.get(ftView);
			if (fuelType == null && !name.isEmpty()) {
				FuelType.create(car, name, tank);
			} else if (fuelType != null && !name.isEmpty()) {
				fuelType.setName(name);
				fuelType.setTank(tank);
				fuelType.save();
			} else if (fuelType != null && name.isEmpty()) {
				fuelType.delete();
			}
		}

		for (FuelType fuelType : fuelTypeRemovals) {
			fuelType.delete();
		}

		saveSuccess();
	}

	private View addFuelTypeView(FuelType fuelType) {
		final View ftView = getActivity().getLayoutInflater().inflate(
				R.layout.row_fueltype, null);
		fuelTypeGroup.addView(ftView);

		AutoCompleteTextView edtName = (AutoCompleteTextView) ftView
				.findViewById(R.id.edt_name);
		edtName.setAdapter(fuelTypeNameAdapter);

		Spinner spnTank = (Spinner) ftView.findViewById(R.id.spn_tank);
		spnTank.setAdapter(fuelTypeTankAdapter);
		spnTank.setOnItemSelectedListener(tankSelectedListener);

		View btnRemove = ftView.findViewById(R.id.btn_remove);
		btnRemove.setOnClickListener(removeFuelTypeListener);

		if (fuelType != null) {
			edtName.setText(fuelType.getName());
			spnTank.setSelection(fuelType.getTank() - 1);
		}

		// The view has wrap_context as height and setting it to 48dp in
		// the layouts file doesn't change this, so we change it here.
		ftView.getLayoutParams().height = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 48, getResources()
						.getDisplayMetrics());
		SimpleAnimator animator = new SimpleAnimator(getActivity(), ftView,
				SimpleAnimator.Property.Height);
		ftView.setAlpha(0);
		ftView.getLayoutParams().height = 0;
		animator.show();

		fuelTypeInputMappings.put(ftView, fuelType);
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

			edtName.setText(car.getName());
			color = car.getColor();

			FuelType[] fuelTypes = FuelType.getAllForCar(car);
			ArrayList<String> fuelTypeNames = new ArrayList<String>();
			int maxTank = 0;
			for (FuelType fuelType : fuelTypes) {
				if (!fuelTypeNames.contains(fuelType.getName())) {
					fuelTypeNames.add(fuelType.getName());
				}
				maxTank = Math.max(maxTank, fuelType.getTank());
			}
			Collections.sort(fuelTypeNames);

			for (String name : fuelTypeNames) {
				fuelTypeNameAdapter.add(name);
			}
			for (int tank = 1; tank <= maxTank; tank++) {
				fuelTypeTankAdapter.insert(
						getString(R.string.label_tank, tank), tank - 1);
			}

			for (FuelType fuelType : fuelTypes) {
				addFuelTypeView(fuelType);
			}

			chkSuspend.setChecked(car.isSuspended());
			if (car.isSuspended()) {
				suspendDate = car.getSuspended();
			}
		} else {
			color = Color.BLUE;
			addFuelTypeView(null);
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
	protected AbstractItem getEditObject(int id) {
		return new Car(id);
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
		fuelTypeGroup = (ViewGroup) v.findViewById(R.id.layout_fueltypes);
		fuelTypeNameAdapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_dropdown_item);
		fuelTypeTankAdapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_dropdown_item);
		chkSuspend = (CheckBox) v.findViewById(R.id.chk_suspend);
		edtSuspendDate = (EditText) v.findViewById(R.id.edt_suspend_date);
		edtSuspendDateAnimator = new SimpleAnimator(getActivity(),
				edtSuspendDate, SimpleAnimator.Property.Height);

		View rowColor = (View) colorIndicator.getParent();
		rowColor.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ColorPickerDialogFragment.newInstance(
						DataDetailCarFragment.this, PICK_COLOR_REQUEST_CODE,
						R.string.alert_change_color_title, color).show(
						getFragmentManager(), null);
			}
		});

		fuelTypeTankAdapter.add(getString(R.string.label_add_tank));

		Button btnAddFuelType = (Button) v.findViewById(R.id.btn_add_fueltype);
		btnAddFuelType.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addFuelTypeView(null);
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
						DataDetailCarFragment.this,
						PICK_SUSPEND_DATE_REQUEST_CODE, getSuspendDate()).show(
						getFragmentManager(), null);
			}
		});
	}

	@Override
	protected void save() {
		InputFieldValidator validator = new InputFieldValidator(getActivity(),
				getFragmentManager(), this);
		validator.add(edtName, InputFieldValidator.ValidationType.NotEmpty,
				R.string.hint_name);
		validator.validate();
	}
}
