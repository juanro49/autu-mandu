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

package me.kuehle.carreport.gui.dialog;

import java.util.Calendar;
import java.util.Date;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.widget.DatePicker;

public class SupportDatePickerDialogFragment extends DialogFragment implements
		DatePickerDialog.OnDateSetListener {
	public interface SupportDatePickerDialogFragmentListener {
		void onDialogPositiveClick(int requestCode, Date date);
	}

	public static SupportDatePickerDialogFragment newInstance(Fragment parent,
			int requestCode, Date date) {
		SupportDatePickerDialogFragment f = new SupportDatePickerDialogFragment();
		f.setTargetFragment(parent, requestCode);

		Bundle args = new Bundle();
		args.putLong("date", date.getTime());
		f.setArguments(args);
		return f;
	}

	@NonNull
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(args.getLong("date"));

		return new DatePickerDialog(getActivity(), this,
				cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
				cal.get(Calendar.DATE));
	}

	@Override
	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, monthOfYear);
		cal.set(Calendar.DATE, dayOfMonth);

		getListener().onDialogPositiveClick(getTargetRequestCode(), cal.getTime());
	}

	private SupportDatePickerDialogFragmentListener getListener() {
		return (SupportDatePickerDialogFragmentListener) getTargetFragment();
	}
}
