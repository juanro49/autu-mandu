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

package me.kuehle.carreport.gui.util;

import android.content.Context;
import android.widget.TextView;

public abstract class AbstractFormFieldValidator {
	protected Context context;
	protected TextView[] fields;

	public AbstractFormFieldValidator(TextView field) {
		this.context = field.getContext();
		this.fields = new TextView[] { field };
	}

	public AbstractFormFieldValidator(TextView... fields) {
		this.context = fields[0].getContext();
		this.fields = fields;
	}

	protected abstract int getMessage();

	protected abstract boolean isValid();

	public boolean validate() {
		boolean valid = isValid();

		for (TextView field : fields) {
			field.setError(valid ? null : context.getString(getMessage()));
		}

		return valid;
	}
}
