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

package me.kuehle.carreport.gui.util;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import me.kuehle.carreport.gui.dialog.SupportDatePickerDialogFragment;
import me.kuehle.carreport.gui.dialog.SupportTimePickerDialogFragment;

public class DateTimeInput {
    public enum Mode {
        DATE, TIME
    }

    private EditText mParent;
    private Mode mMode;
    private DateFormat mFormat;

    public DateTimeInput(EditText parent, Mode mode) {
        mParent = parent;
        mMode = mode;

        if(mode == Mode.DATE) {
            mParent.setInputType(InputType.TYPE_CLASS_DATETIME |
                    InputType.TYPE_DATETIME_VARIATION_DATE);
            mFormat = android.text.format.DateFormat.getDateFormat(mParent.getContext());
        } else {
            mParent.setInputType(InputType.TYPE_CLASS_DATETIME |
                    InputType.TYPE_DATETIME_VARIATION_TIME);
            mFormat = android.text.format.DateFormat.getTimeFormat(mParent.getContext());
        }
    }

    public void applyOnClickListener(final Fragment targetFragment, final int requestCode,
                                     final FragmentManager fragmentManager) {
        mParent.setFocusable(false);
        mParent.setFocusableInTouchMode(false);

        mParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMode == Mode.DATE) {
                    SupportDatePickerDialogFragment
                            .newInstance(targetFragment, requestCode, getDate())
                            .show(fragmentManager, null);
                } else {
                    SupportTimePickerDialogFragment
                            .newInstance(targetFragment, requestCode, getDate())
                            .show(fragmentManager, null);
                }
            }
        });
    }

    public Date getDate() {
        if (mParent.getText().toString().isEmpty()) {
            return null;
        } else {
            try {
                return mFormat.parse(mParent.getText().toString());
            } catch (ParseException e) {
                return new Date();
            }
        }
    }

    public void setDate(Date date) {
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
