package me.kuehle.carreport;

import java.util.Vector;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.EditText;

public class InputFieldValidator {
	private Context context;
	private Vector<Field> fields = new Vector<Field>();
	private ValidationCallback callback;

	public InputFieldValidator(Context context, ValidationCallback callback) {
		this.context = context;
		this.callback = callback;
	}

	public void add(View field, ValidationType type, boolean required,
			CharSequence message) {
		fields.add(new Field(field, type, required, message.toString()));
	}

	public void add(View field, ValidationType type, boolean required,
			int messageID) {
		fields.add(new Field(field, type, required, context
				.getString(messageID)));
	}

	public void validate() {
		Vector<String> msgAsk = new Vector<String>();
		Vector<String> msgError = new Vector<String>();

		for (Field field : fields) {
			if (!field.validate()) {
				if (field.isRequired()) {
					msgError.add(field.getMessage());
				} else {
					msgAsk.add(field.getMessage());
				}
			}
		}

		if (msgError.size() > 0) {
			callback.validationFinished(false);
			String msg = context.getResources().getQuantityString(
					R.plurals.alert_validate_msg_required, msgError.size(),
					joinString(msgError));
			new AlertDialog.Builder(context).setMessage(msg)
					.setPositiveButton(android.R.string.ok, null).show();
		} else if (msgAsk.size() > 0) {
			String msg = context.getResources().getQuantityString(
					R.plurals.alert_validate_msg_recommended, msgAsk.size(),
					joinString(msgAsk));
			new AlertDialog.Builder(context)
					.setMessage(msg)
					.setPositiveButton(R.string.alert_validate_save_anyway,
							new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									callback.validationFinished(true);
								}
							})
					.setNegativeButton(R.string.alert_validate_continue_edit,
							new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									callback.validationFinished(false);
								}
							}).show();
		} else {
			callback.validationFinished(true);
		}
	}

	private String joinString(Vector<String> list) {
		StringBuilder sb = new StringBuilder();
		if (list.size() > 0) {
			sb.append(list.get(0));
		}
		for (int i = 1; i < list.size() - 1; i++) {
			sb.append(", ");
			sb.append(list.get(i));
		}
		if (list.size() > 1) {
			sb.append(" "
					+ context.getString(R.string.alert_validate_last_separator)
					+ " ");
			sb.append(list.lastElement());
		}
		return sb.toString();
	}

	public enum ValidationType {
		NotEmpty, GreaterZero
	}

	public interface ValidationCallback {
		public void validationFinished(boolean success);
	}

	private class Field {
		private View field;
		private ValidationType type;
		private boolean required;
		private String message;

		public Field(View field, ValidationType type, boolean required,
				String message) {
			this.field = field;
			this.type = type;
			this.required = required;
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

		public boolean isRequired() {
			return required;
		}

		public String getMessage() {
			return message;
		}
	}
}
