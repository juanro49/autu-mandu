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

import android.text.TextUtils;
import android.widget.TextView;

import java.text.NumberFormat;
import java.text.ParseException;

import org.juanro.autumandu.R;

/**
 * Validator that checks if a field is either empty or contains a number greater than or equal to zero.
 * Optimized for API 25 compatibility and localized number parsing.
 */
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

        // Use TextUtils.isEmpty for API 25 compatibility
        if (TextUtils.isEmpty(textValue)) {
            return true;
        }

        try {
            double number = Double.parseDouble(textValue);
            return number >= 0;
        } catch (NumberFormatException e) {
            try {
                // Fallback to localized number parsing
                Number number = NumberFormat.getNumberInstance().parse(textValue);
                return number != null && number.doubleValue() >= 0;
            } catch (ParseException f) {
                return false;
            }
        }
    }
}
