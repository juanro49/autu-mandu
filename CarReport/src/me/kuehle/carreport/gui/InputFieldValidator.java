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

package me.kuehle.carreport.gui;

import java.util.Vector;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class InputFieldValidator {
	private Context context;
	private Vector<Field> requiredFields = new Vector<Field>();
	private Field recommendedField = null;
	private String recommendedFieldName = null;
	private ValidationCallback callback;

	public InputFieldValidator(Context context, ValidationCallback callback) {
		this.context = context;
		this.callback = callback;
	}

	public void addRequired(View view, ValidationType type, int messageID) {
		requiredFields.add(new Field(view, type, context.getString(messageID)));
	}

	public void setRecommended(View view, ValidationType type, int nameId,
			int messageId) {
		Preferences prefs = new Preferences(context);
		if (prefs.isValidateDontAskAgain(view.getId())) {
			recommendedField = null;
		} else {
			recommendedField = new Field(view, type,
					context.getString(messageId));
			recommendedFieldName = context.getString(nameId);
		}
	}

	public void validate() {
		Vector<String> msgRequired = new Vector<String>();
		for (Field field : requiredFields) {
			if (!field.validate()) {
				msgRequired.add(field.getMessage());
			}
		}

		if (msgRequired.size() > 0) {
			String msg = context.getString(
					R.string.alert_validate_required_message,
					joinList(msgRequired));
			new AlertDialog.Builder(context)
					.setTitle(R.string.alert_validate_required_title)
					.setMessage(msg)
					.setPositiveButton(android.R.string.ok, null).show();
		} else if (recommendedField != null && !recommendedField.validate()) {
			String title = context.getString(
					R.string.alert_validate_recommend_title,
					recommendedFieldName);
			final CheckBox chkDontAskAgain = new CheckBox(context);
			chkDontAskAgain
					.setText(R.string.alert_validate_recommend_dont_ask_again);
			String btnPositive = context.getString(
					R.string.alert_validate_recommend_save,
					recommendedFieldName);
			String btnNegative = context.getString(
					R.string.alert_validate_recommend_edit,
					recommendedFieldName);
			new AlertDialog.Builder(context).setTitle(title)
					.setMessage(recommendedField.getMessage())
					.setView(chkDontAskAgain)
					.setPositiveButton(btnPositive, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							callback.validationSuccessfull();
							if (chkDontAskAgain.isChecked()) {
								Preferences prefs = new Preferences(context);
								prefs.setValidateDontAskAgain(
										recommendedField.field.getId(), true);
							}
						}
					}).setNegativeButton(btnNegative, null).show();
		} else {
			callback.validationSuccessfull();
		}
	}

	private String joinList(Vector<String> list) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			sb.append("\n- " + list.get(i));
			if (list.size() - i > 2) {
				sb.append(",");
			} else if (list.size() - i == 2) {
				sb.append(context
						.getString(R.string.alert_validate_last_separator));
			}
		}
		return sb.toString();
	}

	public enum ValidationType {
		NotEmpty, GreaterZero
	}

	public interface ValidationCallback {
		public void validationSuccessfull();
	}

	private class Field {
		private View field;
		private ValidationType type;
		private String message;

		public Field(View field, ValidationType type, String message) {
			this.field = field;
			this.type = type;
			this.message = message;
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

		public String getMessage() {
			return message;
		}
	}
}
