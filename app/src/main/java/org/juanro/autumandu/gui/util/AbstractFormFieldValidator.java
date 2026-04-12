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

import android.content.Context;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

/**
 * Base class for form field validation.
 */
public abstract class AbstractFormFieldValidator {
    protected final Context context;
    protected final TextView[] fields;

    public AbstractFormFieldValidator(TextView field) {
        this.context = field.getContext();
        this.fields = new TextView[]{field};
    }

    public void clear() {
        for (TextView field : fields) {
            setError(field, null);
        }
    }

    public boolean validate() {
        boolean valid = isValid();

        for (TextView field : fields) {
            if (!valid) {
                CharSequence currentError = getError(field);
                String newError = context.getString(getMessage());
                String error;
                if (currentError == null || currentError.length() == 0) {
                    error = newError;
                } else {
                    error = currentError + "\n\n" + newError;
                }

                setError(field, error);
            }
        }

        return valid;
    }

    protected abstract int getMessage();

    /**
     * Validation logic. Subclasses implement this to check the field's validity.
     * Note: For data-driven validations, ensure DAOs from Room are used.
     */
    protected abstract boolean isValid();

    private CharSequence getError(TextView field) {
        TextInputLayout layout = getTextInputLayout(field);
        if (layout != null) {
            return layout.getError();
        } else {
            return field.getError();
        }
    }

    private void setError(TextView field, String error) {
        TextInputLayout layout = getTextInputLayout(field);
        if (layout != null) {
            layout.setError(error);
            layout.setErrorEnabled(error != null);
        } else {
            field.setError(error);
        }
    }

    private TextInputLayout getTextInputLayout(TextView field) {
        ViewParent parent = field.getParent();
        while (parent instanceof View) {
            if (parent instanceof TextInputLayout) {
                return (TextInputLayout) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }
}
