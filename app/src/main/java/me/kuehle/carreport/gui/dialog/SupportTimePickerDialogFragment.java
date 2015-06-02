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

import android.app.Dialog;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

public class SupportTimePickerDialogFragment extends DialogFragment implements
		TimePickerDialog.OnTimeSetListener {
	public interface SupportTimePickerDialogFragmentListener {
		void onDialogPositiveClick(int requestCode, Date time);
	}

	public static SupportTimePickerDialogFragment newInstance(Fragment parent,
			int requestCode, Date time) {
		SupportTimePickerDialogFragment f = new SupportTimePickerDialogFragment();
		f.setTargetFragment(parent, requestCode);

		Bundle args = new Bundle();
		args.putLong("time", time.getTime());
		f.setArguments(args);
		return f;
	}

	@NonNull
    @Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(args.getLong("time"));

		return new TimePickerDialog(getActivity(), this,
				cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
				DateFormat.is24HourFormat(getActivity()));
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
		cal.set(Calendar.MINUTE, minute);

		getListener().onDialogPositiveClick(getTargetRequestCode(),
				cal.getTime());
	}

	private SupportTimePickerDialogFragmentListener getListener() {
		return (SupportTimePickerDialogFragmentListener) getTargetFragment();
	}
}
