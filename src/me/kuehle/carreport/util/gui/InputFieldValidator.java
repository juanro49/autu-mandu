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

package me.kuehle.carreport.util.gui;

import java.util.Vector;

import me.kuehle.carreport.R;
import me.kuehle.carreport.util.Strings;
import android.app.FragmentManager;
import android.content.Context;
import android.view.View;
import android.widget.EditText;

public class InputFieldValidator {
	private class Field {
		private View field;
		private ValidationType type;
		private String message;

		public Field(View field, ValidationType type, String name) {
			this.field = field;
			this.type = type;
			this.message = context
					.getString(
							type == ValidationType.GreaterZero ? R.string.alert_validate_greater_zero
									: R.string.alert_validate_not_empty, name);
		}

		public String getMessage() {
			return message;
		}

		public boolean validate() {
			if (field instanceof EditText) {
				EditText editText = (EditText) field;
				if (type == ValidationType.NotEmpty
						&& editText.getText().toString().trim().isEmpty()) {
					return false;
				} else if (type == ValidationType.GreaterZero) {
					try {
						double number = Double.parseDouble(editText.getText()
								.toString());
						if (number <= 0) {
							return false;
						}
					} catch (NumberFormatException e) {
						return false;
					}
				}
			}
			return true;
		}
	}

	public interface ValidationCallback {
		public void validationSuccessfull();
	}

	public enum ValidationType {
		NotEmpty, GreaterZero
	}

	private Context context;
	private Vector<Field> requiredFields = new Vector<Field>();
	private FragmentManager fragmentManager;
	private ValidationCallback callback;

	public InputFieldValidator(Context context, FragmentManager fm,
			ValidationCallback callback) {
		this.context = context;
		this.fragmentManager = fm;
		this.callback = callback;
	}

	public void add(View view, ValidationType type, int messageID) {
		requiredFields.add(new Field(view, type, context.getString(messageID)));
	}

	public void validate() {
		Vector<String> messages = new Vector<String>();
		for (Field field : requiredFields) {
			if (!field.validate()) {
				messages.add(field.getMessage());
			}
		}

		if (messages.size() > 0) {
			MessageDialogFragment.newInstance(null, 0,
					R.string.alert_validate_title,
					Strings.join(messages, "\n"), android.R.string.ok, null)
					.show(fragmentManager, null);
		} else {
			callback.validationSuccessfull();
		}
	}
}
