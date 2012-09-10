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
import java.util.Calendar;
import java.util.Date;

import me.kuehle.carreport.R;
import me.kuehle.carreport.db.AbstractItem;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

public abstract class AbstractDataDetailFragment extends Fragment {
	public class EditTextDateField implements View.OnClickListener,
			DatePickerDialog.OnDateSetListener {
		private EditText editText;

		public EditTextDateField(EditText view) {
			editText = view;
			editText.setOnClickListener(this);
		}

		public Date getDate() {
			Date date = new Date();
			try {
				date = DateFormat.getDateFormat(getActivity()).parse(
						editText.getText().toString());
			} catch (ParseException e) {
			}
			return date;
		}

		@Override
		public void onClick(View v) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(getDate());
			new DatePickerDialog(getActivity(), this, cal.get(Calendar.YEAR),
					cal.get(Calendar.MONTH), cal.get(Calendar.DATE)).show();
		}

		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, year);
			cal.set(Calendar.MONTH, monthOfYear);
			cal.set(Calendar.DATE, dayOfMonth);

			setDate(cal.getTime());
		}

		public void setDate(Date date) {
			editText.setText(DateFormat.getDateFormat(getActivity()).format(
					date));
		}
	}
	public class EditTextTimeField implements View.OnClickListener,
			TimePickerDialog.OnTimeSetListener {
		private EditText editText;

		public EditTextTimeField(EditText view) {
			editText = view;
			editText.setOnClickListener(this);
		}

		public Date getTime() {
			Date time = new Date();
			try {
				time = DateFormat.getTimeFormat(getActivity()).parse(
						editText.getText().toString());
			} catch (ParseException e) {
			}
			return time;
		}

		@Override
		public void onClick(View v) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(getTime());
			new TimePickerDialog(getActivity(), this,
					cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
					DateFormat.is24HourFormat(getActivity())).show();
		}

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
			cal.set(Calendar.MINUTE, minute);

			setTime(cal.getTime());
		}

		public void setTime(Date time) {
			editText.setText(DateFormat.getTimeFormat(getActivity()).format(
					time));
		}
	}
	public interface OnItemActionListener {
		public void itemCanceled();

		public void itemDeleted();

		public void itemSaved();
	}

	public static final String EXTRA_ID = "id";
	public static final int EXTRA_ID_DEFAULT = -1;

	public static final String EXTRA_CAR_ID = "car_id";
	protected OnItemActionListener onItemActionListener;

	protected AbstractItem editItem = null;

	private CharSequence savedABTitle;

	private int savedABNavMode;

	protected abstract void fillFields(View v);

	protected abstract int getAlertDeleteMessage();

	protected Date getDateTime(Date date, Date time) {
		Calendar calTime = Calendar.getInstance();
		calTime.setTime(time);

		Calendar calDateTime = Calendar.getInstance();
		calDateTime.setTime(date);
		calDateTime
				.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
		calDateTime.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));
		calDateTime.set(Calendar.SECOND, calTime.get(Calendar.SECOND));

		return calDateTime.getTime();
	}

	protected double getDoubleFromEditText(int view, double defaultValue) {
		EditText editText = (EditText) getView().findViewById(view);
		String strDouble = editText.getText().toString();
		try {
			return Double.parseDouble(strDouble);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	protected abstract AbstractItem getEditObject(int id);

	protected int getIntegerFromEditText(int view, int defaultValue) {
		EditText editText = (EditText) getView().findViewById(view);
		String strInt = editText.getText().toString();
		try {
			return Integer.parseInt(strInt);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	protected abstract int getLayout();

	protected abstract int getTitleForEdit();

	protected abstract int getTitleForNew();

	protected abstract int getToastDeletedMessage();

	protected abstract int getToastSavedMessage();

	protected abstract void initFields(View v);

	protected boolean isInEditMode() {
		return editItem != null;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		savedABTitle = actionBar.getTitle();
		savedABNavMode = actionBar.getNavigationMode();

		if (isInEditMode()) {
			actionBar.setTitle(getTitleForEdit());
		} else {
			actionBar.setTitle(getTitleForNew());
		}
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			onItemActionListener = (OnItemActionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnItemActionListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int id = getArguments().getInt(EXTRA_ID, EXTRA_ID_DEFAULT);
		if (id != EXTRA_ID_DEFAULT) {
			editItem = getEditObject(id);
		}
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.edit, menu);
		if (!isInEditMode()) {
			menu.removeItem(R.id.menu_delete);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(getLayout(), container, false);
		initFields(v);
		fillFields(v);
		return v;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setTitle(savedABTitle);
		actionBar.setNavigationMode(savedABNavMode);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_save:
			save();
			return true;
		case R.id.menu_cancel:
			onItemActionListener.itemCanceled();
			return true;
		case R.id.menu_delete:
			new AlertDialog.Builder(getActivity())
					.setTitle(R.string.alert_delete_title)
					.setMessage(getAlertDeleteMessage())
					.setPositiveButton(android.R.string.yes,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									editItem.delete();
									Toast.makeText(getActivity(),
											getToastDeletedMessage(),
											Toast.LENGTH_SHORT).show();
									onItemActionListener.itemDeleted();
								}
							}).setNegativeButton(android.R.string.no, null)
					.show();
			return true;
		case android.R.id.home:
			onItemActionListener.itemCanceled();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	protected abstract void save();

	protected void saveSuccess() {
		Toast.makeText(getActivity(), getToastSavedMessage(),
				Toast.LENGTH_SHORT).show();
		onItemActionListener.itemSaved();
	}
}
