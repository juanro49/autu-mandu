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

package org.juanro.autumandu.gui.util;

import android.widget.TextView;

import java.text.NumberFormat;
import java.text.ParseException;

import org.juanro.autumandu.R;

public class FormFieldGreaterEqualZeroOrEmptyValidator extends AbstractFormFieldValidator {
    public FormFieldGreaterEqualZeroOrEmptyValidator(TextView field) {
        super(field);
    }

    @Override
    protected int getMessage() {
        return R.string.validate_error_greater_equal_zero;
    }

    @Override
    protected boolean isValid() {
        String textValue = fields[0].getText().toString();
        if (textValue.isEmpty()) {
            return true;
        }

        try {
            double number = Double.parseDouble(textValue);
            if (number < 0) {
                return false;
            }
        } catch (NumberFormatException e) {
            try {
                Number number = NumberFormat.getNumberInstance().parse(textValue);
                return ((double) number >= 0);
            } catch (ParseException f) {
                return false;
            }
        }

        return true;
    }

}
