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

import android.app.DatePickerDialog;
import android.app.Dialog;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.os.Bundle;

public class DatePickerDialogFragment extends DialogFragment {
    public static final String REQUEST_KEY = "org.juanro.autumandu.DATE_PICKER_DIALOG_REQUEST";
    public static final String RESULT_DATE = "date";
    public static final String RESULT_REQUEST_CODE = "request_code";

    private static final String ARG_DATE = "date";
    private static final String ARG_REQUEST_CODE = "request_code";

    public static DatePickerDialogFragment newInstance(int requestCode, Date date) {
        DatePickerDialogFragment f = new DatePickerDialogFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_DATE, date.getTime());
        args.putInt(ARG_REQUEST_CODE, requestCode);
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        long dateMillis = args.getLong(ARG_DATE);
        int requestCode = args.getInt(ARG_REQUEST_CODE);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateMillis);

        return new DatePickerDialog(requireActivity(), (view, year, month, dayOfMonth) -> {
            Calendar resultCal = Calendar.getInstance();
            resultCal.set(Calendar.YEAR, year);
            resultCal.set(Calendar.MONTH, month);
            resultCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            Bundle result = new Bundle();
            result.putLong(RESULT_DATE, resultCal.getTimeInMillis());
            result.putInt(RESULT_REQUEST_CODE, requestCode);
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
    }
}
