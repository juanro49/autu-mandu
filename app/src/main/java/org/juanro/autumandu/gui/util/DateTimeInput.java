/*
 * Copyright 2014 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.juanro.autumandu.gui.util;

import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import org.juanro.autumandu.gui.dialog.DatePickerDialogFragment;
import org.juanro.autumandu.gui.dialog.TimePickerDialogFragment;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

public class DateTimeInput {
    public enum Mode {
        DATE, TIME
    }

    private final EditText mParent;
    private final Mode mMode;
    private final DateFormat mFormat;

    public DateTimeInput(@NonNull EditText parent, Mode mode) {
        mParent = parent;
        mMode = mode;

        if (mode == Mode.DATE) {
            mParent.setInputType(InputType.TYPE_CLASS_DATETIME |
                    InputType.TYPE_DATETIME_VARIATION_DATE);
            mFormat = android.text.format.DateFormat.getDateFormat(mParent.getContext());
        } else {
            mParent.setInputType(InputType.TYPE_CLASS_DATETIME |
                    InputType.TYPE_DATETIME_VARIATION_TIME);
            mFormat = android.text.format.DateFormat.getTimeFormat(mParent.getContext());
        }
    }

    public void applyOnClickListener(final int requestCode,
                                     final FragmentManager fragmentManager) {
        mParent.setFocusable(false);
        mParent.setFocusableInTouchMode(false);

        mParent.setOnClickListener(v -> {
            if (mMode == Mode.DATE) {
                DatePickerDialogFragment
                        .newInstance(requestCode, getDate() != null ? getDate() : new Date())
                        .show(fragmentManager, null);
            } else {
                TimePickerDialogFragment
                        .newInstance(requestCode, getDate() != null ? getDate() : new Date())
                        .show(fragmentManager, null);
            }
        });
    }

    @Nullable
    public Date getDate() {
        String text = mParent.getText().toString();
        if (TextUtils.isEmpty(text)) {
            return null;
        } else {
            try {
                return mFormat.parse(text);
            } catch (ParseException e) {
                return null;
            }
        }
    }

    public void setDate(@Nullable Date date) {
        if (date == null) {
            mParent.setText("");
        } else {
            mParent.setText(mFormat.format(date));
        }
    }

    public EditText getEditText() {
        return mParent;
    }

    public static Date getDateTime(Date date, Date time) {
        Calendar calTime = Calendar.getInstance();
        calTime.setTime(time);

        Calendar calDateTime = Calendar.getInstance();
        calDateTime.setTime(date);
        calDateTime.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
        calDateTime.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));
        calDateTime.set(Calendar.SECOND, calTime.get(Calendar.SECOND));

        return calDateTime.getTime();
    }
}
