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

package org.juanro.autumandu.gui.dialog;

import java.util.Calendar;
import java.util.Date;

import android.app.Dialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;

public class TimePickerDialogFragment extends DialogFragment {
    public static final String REQUEST_KEY = "org.juanro.autumandu.TIME_PICKER_DIALOG_REQUEST";
    public static final String RESULT_TIME = "time";
    public static final String RESULT_REQUEST_CODE = "request_code";

    private static final String ARG_TIME = "time";
    private static final String ARG_REQUEST_CODE = "request_code";

    public static TimePickerDialogFragment newInstance(int requestCode, Date time) {
        TimePickerDialogFragment f = new TimePickerDialogFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_TIME, time.getTime());
        args.putInt(ARG_REQUEST_CODE, requestCode);
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        long timeMillis = args.getLong(ARG_TIME);
        int requestCode = args.getInt(ARG_REQUEST_CODE);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMillis);

        return new TimePickerDialog(requireActivity(), (view, hourOfDay, minute) -> {
            Calendar resultCal = Calendar.getInstance();
            resultCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
            resultCal.set(Calendar.MINUTE, minute);

            Bundle result = new Bundle();
            result.putLong(RESULT_TIME, resultCal.getTimeInMillis());
            result.putInt(RESULT_REQUEST_CODE, requestCode);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), DateFormat.is24HourFormat(requireActivity()));
    }
}
