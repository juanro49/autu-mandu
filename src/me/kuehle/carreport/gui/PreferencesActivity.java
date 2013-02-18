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

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.Helper;
import me.kuehle.carreport.util.IForEach;
import me.kuehle.carreport.util.backup.Backup;
import me.kuehle.carreport.util.backup.CSVExportImport;
import me.kuehle.carreport.util.backup.Dropbox;
import me.kuehle.carreport.util.gui.ColorPickerDialogFragment;
import me.kuehle.carreport.util.gui.ColorPickerDialogFragment.ColorPickerDialogFragmentListener;
import me.kuehle.carreport.util.gui.InputDialogFragment;
import me.kuehle.carreport.util.gui.InputDialogFragment.InputDialogFragmentListener;
import me.kuehle.carreport.util.gui.MessageDialogFragment;
import me.kuehle.carreport.util.gui.MessageDialogFragment.MessageDialogFragmentListener;
import me.kuehle.carreport.util.gui.ProgressDialogFragment;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity {
	public static class AboutFragment extends Fragment {
		public static class LicenseDialogFragment extends DialogFragment {
			public static LicenseDialogFragment newInstance() {
				return new LicenseDialogFragment();
			}

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				TextView view = new TextView(getActivity());
				view.setMovementMethod(LinkMovementMethod.getInstance());
				view.setPadding(16, 16, 16, 16);
				try {
					InputStream in = getActivity().getAssets().open(
							"licenses.html");
					byte[] buffer = new byte[in.available()];
					in.read(buffer);
					in.close();
					view.setText(Html.fromHtml(new String(buffer)));
				} catch (IOException e) {
				}

				return new AlertDialog.Builder(getActivity())
						.setTitle(R.string.alert_about_licenses_title)
						.setView(view)
						.setPositiveButton(android.R.string.ok, null).create();
			}
		}

		private View.OnClickListener licensesOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				LicenseDialogFragment.newInstance().show(getFragmentManager(),
						null);
			}
		};

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View root = inflater.inflate(R.layout.fragment_prefs_about,
					container, false);

			String strVersion = getString(R.string.about_version, getVersion());
			((TextView) root.findViewById(R.id.txtVersion)).setText(strVersion);
			((Button) root.findViewById(R.id.btnLicenses))
					.setOnClickListener(licensesOnClickListener);

			return root;
		}

		public String getVersion() {
			try {
				return getActivity().getPackageManager().getPackageInfo(
						getActivity().getPackageName(), 0).versionName;
			} catch (NameNotFoundException e) {
				return "";
			}
		}
	}

	public static class BackupFragment extends PreferenceFragment implements
			MessageDialogFragmentListener {
		public static class ExportDialogFragment extends DialogFragment {
			public static ExportDialogFragment newInstance(Fragment parent) {
				ExportDialogFragment f = new ExportDialogFragment();
				f.setTargetFragment(parent, 0);
				return f;
			}

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				final View view = inflater.inflate(R.layout.dialog_exportcsv,
						null);
				return new AlertDialog.Builder(getActivity())
						.setTitle(R.string.pref_title_exportcsv)
						.setView(view)
						.setPositiveButton(R.string.export,
								new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										int option = ((Spinner) view
												.findViewById(R.id.spnSingleMultipleFile))
												.getSelectedItemPosition();
										boolean overwrite = ((CheckBox) view
												.findViewById(R.id.chkOverwrite))
												.isChecked();
										((BackupFragment) getTargetFragment())
												.doExport(option, overwrite);
									}
								})
						.setNegativeButton(android.R.string.cancel, null)
						.create();
			}
		}

		public static class ImportDialogFragment extends DialogFragment {
			public static ImportDialogFragment newInstance(Fragment parent) {
				ImportDialogFragment f = new ImportDialogFragment();
				f.setTargetFragment(parent, 0);
				return f;
			}

			@Override
			public Dialog onCreateDialog(Bundle savedInstanceState) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				final View view = inflater.inflate(R.layout.dialog_importcsv,
						null);
				return new AlertDialog.Builder(getActivity())
						.setTitle(R.string.pref_title_importcsv)
						.setView(view)
						.setPositiveButton(R.string.import_,
								new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										int option = ((Spinner) view
												.findViewById(R.id.spnSingleMultipleFile))
												.getSelectedItemPosition();
										((BackupFragment) getTargetFragment())
												.doImport(option);
									}
								})
						.setNegativeButton(android.R.string.cancel, null)
						.create();
			}
		}

		private static final int DROPBOX_FIRST_SYNC_REQUEST_CODE = 0;
		private static final int BACKUP_OVERWRITE_REQUEST_CODE = 1;
		private static final int RESTORE_REQUEST_CODE = 2;

		private Dropbox dropbox;
		private boolean dropboxAuthenticationInProgress = false;
		private Backup backup;
		private CSVExportImport csvExportImport;

		private OnPreferenceClickListener mSyncDropbox = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (dropbox.isLinked()) {
					dropbox.unlink();
					setupDropdoxPreference();
				} else {
					dropboxAuthenticationInProgress = true;
					dropbox.startAuthentication(getActivity());
				}

				return true;
			}
		};

		private OnPreferenceClickListener mBackup = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (backup.backupFileExists()) {
					MessageDialogFragment.newInstance(BackupFragment.this,
							BACKUP_OVERWRITE_REQUEST_CODE,
							R.string.alert_backup_overwrite_title,
							getString(R.string.alert_backup_overwrite_message),
							R.string.overwrite, android.R.string.cancel).show(
							getFragmentManager(), null);
				} else {
					doBackup();
				}
				return true;
			}

		};

		private OnPreferenceClickListener mRestore = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				MessageDialogFragment.newInstance(BackupFragment.this,
						RESTORE_REQUEST_CODE, R.string.alert_restore_title,
						getString(R.string.alert_restore_message),
						R.string.restore, android.R.string.cancel).show(
						getFragmentManager(), null);
				return true;
			}
		};

		private OnPreferenceClickListener mExportCSV = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				ExportDialogFragment.newInstance(BackupFragment.this).show(
						getFragmentManager(), null);
				return true;
			}
		};

		private OnPreferenceClickListener mImportCSV = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				ImportDialogFragment.newInstance(BackupFragment.this).show(
						getFragmentManager(), null);
				return true;
			}
		};

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences_backup);
			dropbox = Dropbox.getInstance();
			backup = new Backup();
			csvExportImport = new CSVExportImport(
					DateFormat.getDateTimeInstance());

			// Sync Dropbox
			{
				setupDropdoxPreference();
			}

			// Backup
			{
				Preference backup = findPreference("backup");
				backup.setEnabled(this.backup.canBackup());
				backup.setOnPreferenceClickListener(mBackup);
			}

			// Restore
			{
				setupRestorePreference();
			}

			// Export CSV
			{
				Preference export = findPreference("exportcsv");
				export.setEnabled(csvExportImport.canExport());
				export.setOnPreferenceClickListener(mExportCSV);
			}

			// Import CSV
			{
				Preference import_ = findPreference("importcsv");
				import_.setOnPreferenceClickListener(mImportCSV);
			}
		}

		@Override
		public void onDialogNegativeClick(int requestCode) {
			if (requestCode == DROPBOX_FIRST_SYNC_REQUEST_CODE) {
				dropbox.synchronize(Dropbox.SYNC_UPLOAD);
			}
		}

		@Override
		public void onDialogPositiveClick(int requestCode) {
			if (requestCode == DROPBOX_FIRST_SYNC_REQUEST_CODE) {
				dropbox.synchronize(Dropbox.SYNC_DOWNLOAD);
			} else if (requestCode == BACKUP_OVERWRITE_REQUEST_CODE) {
				doBackup();
			} else if (requestCode == RESTORE_REQUEST_CODE) {
				doRestore();
			}
		}

		@Override
		public void onResume() {
			super.onResume();

			if (dropboxAuthenticationInProgress) {
				dropboxAuthenticationInProgress = false;

				ProgressDialogFragment
						.newInstance(
								getString(R.string.alert_dropbox_finishing_authentication))
						.show(getFragmentManager(), "progress");

				dropbox.finishAuthentication(new Dropbox.OnAuthenticationFinishedListener() {
					@Override
					public void authenticationFinished(boolean success,
							String accountName, boolean remoteDataAvailable) {
						((ProgressDialogFragment) getFragmentManager()
								.findFragmentByTag("progress")).dismiss();
						if (success) {
							setupDropdoxPreference();
							if (remoteDataAvailable) {
								dropboxFirstSynchronisation();
							} else {
								dropbox.synchronize(Dropbox.SYNC_UPLOAD);
							}
						} else {
							Toast.makeText(
									getActivity(),
									R.string.toast_dropbox_authentication_failed,
									Toast.LENGTH_SHORT).show();
						}
					}
				});
			}
		}

		private void doBackup() {
			synchronized (Helper.dbLock) {
				if (backup.backup()) {
					Toast.makeText(
							getActivity(),
							getString(R.string.toast_backup_success,
									Backup.FILE_NAME), Toast.LENGTH_SHORT)
							.show();
					setupRestorePreference();
				} else {
					Toast.makeText(getActivity(), R.string.toast_backup_failed,
							Toast.LENGTH_SHORT).show();
				}
			}
		}

		private void doExport(int option, boolean overwrite) {
			if (!overwrite && csvExportImport.anyExportFileExist(option)) {
				Toast.makeText(getActivity(),
						R.string.toast_export_failed_overwrite,
						Toast.LENGTH_SHORT).show();
			} else {
				csvExportImport.export(option);
				Toast.makeText(getActivity(), R.string.toast_export_success,
						Toast.LENGTH_SHORT).show();
			}
		}

		private void doImport(int option) {
			if (!csvExportImport.allExportFilesExist(option)) {
				Toast.makeText(getActivity(),
						R.string.toast_import_files_dont_exist,
						Toast.LENGTH_SHORT).show();
			} else if (csvExportImport.import_(option)) {
				Toast.makeText(getActivity(), R.string.toast_import_success,
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getActivity(), R.string.toast_import_failed,
						Toast.LENGTH_SHORT).show();
			}
		}

		private void doRestore() {
			synchronized (Helper.dbLock) {
				if (backup.restore()) {
					Toast.makeText(getActivity(),
							R.string.toast_restore_success, Toast.LENGTH_SHORT)
							.show();
				} else {
					Toast.makeText(getActivity(),
							R.string.toast_restore_failed, Toast.LENGTH_SHORT)
							.show();
				}
			}
		}

		private void dropboxFirstSynchronisation() {
			MessageDialogFragment.newInstance(this,
					DROPBOX_FIRST_SYNC_REQUEST_CODE,
					R.string.alert_dropbox_first_sync_title,
					getString(R.string.alert_dropbox_first_sync_message),
					R.string.alert_dropbox_first_sync_download,
					R.string.alert_dropbox_first_sync_upload).show(
					getFragmentManager(), null);
		}

		private void setupDropdoxPreference() {
			Preference sync = findPreference("sync_dropbox");
			sync.setOnPreferenceClickListener(mSyncDropbox);

			if (dropbox.isLinked()) {
				sync.setSummary(getString(
						R.string.pref_summary_sync_dropbox_unlink,
						dropbox.getAccountName()));
			} else {
				sync.setSummary(R.string.pref_summary_sync_dropbox_link);
			}
		}

		private void setupRestorePreference() {
			Preference restore = findPreference("restore");
			if (backup.canRestore()) {
				restore.setSummary(getString(R.string.pref_summary_restore,
						Backup.FILE_NAME));
				restore.setEnabled(true);
			} else {
				restore.setSummary(getString(
						R.string.pref_summary_restore_no_data, Backup.FILE_NAME));
				restore.setEnabled(false);
			}
			restore.setOnPreferenceClickListener(mRestore);
		}
	}

	public static class CarsFragment extends ListFragment implements
			MessageDialogFragmentListener, InputDialogFragmentListener,
			ColorPickerDialogFragmentListener {
		private class CarAdapter extends BaseAdapter {
			@Override
			public int getCount() {
				return cars.length;
			}

			@Override
			public Car getItem(int position) {
				return cars[position];
			}

			@Override
			public long getItemId(int position) {
				return position;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				CarViewHolder holder = null;

				if (convertView == null) {
					convertView = getActivity().getLayoutInflater().inflate(
							R.layout.split_list_item_2, parent, false);

					holder = new CarViewHolder();
					holder.name = (TextView) convertView
							.findViewById(android.R.id.text1);
					holder.suspended = (TextView) convertView
							.findViewById(android.R.id.text2);
					holder.color = ((Button) convertView
							.findViewById(android.R.id.button1));
					holder.color.setBackgroundResource(R.drawable.color_button);
					holder.color.setOnClickListener(colorOnClickListener);

					convertView.setTag(holder);
				} else {
					holder = (CarViewHolder) convertView.getTag();
				}

				holder.name.setText(cars[position].getName());
				if (cars[position].isSuspended()) {
					holder.suspended.setText(getString(
							R.string.suspended_since,
							android.text.format.DateFormat.getDateFormat(
									getActivity()).format(
									cars[position].getSuspended())));
					holder.suspended.setVisibility(View.VISIBLE);
				} else {
					holder.suspended.setVisibility(View.GONE);
				}
				((GradientDrawable) holder.color.getBackground())
						.setColorFilter(cars[position].getColor(),
								PorterDuff.Mode.SRC);
				return convertView;
			}
		}

		private class CarMultiChoiceModeListener implements
				MultiChoiceModeListener {
			private ActionMode mode;

			public void execActionAndFinish(IForEach<Car> forEach) {
				SparseBooleanArray selected = getListView()
						.getCheckedItemPositions();
				for (int i = 0; i < cars.length; i++) {
					if (selected.get(i)) {
						forEach.action(cars[i]);
					}
				}

				mode.finish();
				fillList();
			}

			public void finishActionMode() {
				if (mode != null) {
					mode.finish();
				}
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case R.id.menu_suspend:
					execActionAndFinish(new IForEach<Car>() {
						Date now = new Date();

						public void action(Car car) {
							if (!car.isSuspended()) {
								car.setSuspended(now);
								car.save();
							}
						}
					});
					return true;
				case R.id.menu_unsuspend:
					execActionAndFinish(new IForEach<Car>() {
						public void action(Car car) {
							car.setSuspended(null);
							car.save();
						}
					});
					return true;
				case R.id.menu_delete:
					if (getListView().getCheckedItemCount() == cars.length) {
						MessageDialogFragment
								.newInstance(
										CarsFragment.this,
										CANNOT_DELETE_REQUEST_CODE,
										R.string.alert_delete_title,
										getString(R.string.alert_cannot_delete_last_car),
										android.R.string.ok, null).show(
										getFragmentManager(), null);
					} else {
						String message = getString(
								R.string.alert_delete_cars_message,
								getListView().getCheckedItemCount());
						MessageDialogFragment.newInstance(CarsFragment.this,
								DELETE_REQUEST_CODE,
								R.string.alert_delete_title, message,
								android.R.string.yes, android.R.string.no)
								.show(getFragmentManager(), null);
					}
					return true;
				default:
					return false;
				}
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				this.mode = mode;
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.edit_cars_cab, menu);
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode,
					int position, long id, boolean checked) {
				int count = getListView().getCheckedItemCount();
				mode.setTitle(String.format(
						getString(R.string.cab_title_selected), count));
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}
		}

		private static class CarViewHolder {
			public TextView name;
			public TextView suspended;
			public Button color;
		}

		private static final int CANNOT_DELETE_REQUEST_CODE = 0;
		private static final int DELETE_REQUEST_CODE = 1;
		private static final int ADD_REQUEST_CODE = 2;
		private static final int EDIT_REQUEST_CODE = 3;
		private static final int EDIT_COLOR_REQUEST_CODE = 4;

		private Car[] cars;
		private Car carToEdit;

		private OnItemClickListener onItemClickListener = new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				carToEdit = cars[position];
				InputDialogFragment.newInstance(CarsFragment.this,
						EDIT_REQUEST_CODE, R.string.alert_edit_car_title,
						carToEdit.getName()).show(getFragmentManager(), null);
			}
		};

		private View.OnClickListener colorOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final int position = getListView().getPositionForView(v);
				if (position != ListView.INVALID_POSITION) {
					carToEdit = cars[position];
					ColorPickerDialogFragment.newInstance(CarsFragment.this,
							EDIT_COLOR_REQUEST_CODE,
							R.string.alert_change_color_title,
							carToEdit.getColor()).show(getFragmentManager(),
							null);
				}
			}
		};

		private CarMultiChoiceModeListener multiChoiceModeListener = new CarMultiChoiceModeListener();

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			getListView().setOnItemClickListener(onItemClickListener);
			getListView().setMultiChoiceModeListener(multiChoiceModeListener);
			getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

			fillList();
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setHasOptionsMenu(true);
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			inflater.inflate(R.menu.edit_cars, menu);
		}

		@Override
		public void onDialogNegativeClick(int requestCode) {

		}

		@Override
		public void onDialogPositiveClick(int requestCode) {
			if (requestCode == DELETE_REQUEST_CODE) {
				multiChoiceModeListener
						.execActionAndFinish(new IForEach<Car>() {
							public void action(Car car) {
								car.delete();
							}
						});
			}
		}

		@Override
		public void onDialogPositiveClick(int requestCode, int color) {
			if (requestCode == EDIT_COLOR_REQUEST_CODE) {
				carToEdit.setColor(color);
				carToEdit.save();
				fillList();
			}
		}

		@Override
		public void onDialogPositiveClick(int requestCode, String input) {
			if (requestCode == ADD_REQUEST_CODE) {
				if (input.length() > 0) {
					Car.create(input, Color.BLUE, null);
					fillList();
				}
			} else if (requestCode == EDIT_REQUEST_CODE) {
				if (input.length() > 0) {
					carToEdit.setName(input);
					carToEdit.save();
					fillList();
				}
			}
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case R.id.menu_add_car:
				InputDialogFragment.newInstance(this, ADD_REQUEST_CODE,
						R.string.alert_add_car_title, "").show(
						getFragmentManager(), null);
				return true;
			default:
				return super.onOptionsItemSelected(item);
			}
		}

		@Override
		public void onStop() {
			super.onStop();
			multiChoiceModeListener.finishActionMode();
		}

		private void fillList() {
			cars = Car.getAll();
			setListAdapter(new CarAdapter());
		}
	}

	public static class GeneralFragment extends PreferenceFragment {
		private OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				if (preference.getKey().equals("default_car")) {
					Car car = new Car(Integer.parseInt(newValue.toString()));
					preference.setSummary(car.getName());
				} else if (preference.getKey().equals("default_report")) {
					String[] reports = getResources().getStringArray(
							R.array.reports);
					preference.setSummary(reports[Integer.parseInt(newValue
							.toString())]);
				} else if (preference.getKey().equals(
						"appearance_overall_section_pos")) {
					String[] positions = getResources().getStringArray(
							R.array.overall_section_positions);
					preference
							.setSummary(getString(
									R.string.pref_summary_appearance_overall_section_pos,
									positions[Integer.parseInt(newValue
											.toString())]));
				} else if (preference instanceof EditTextPreference) {
					preference.setSummary(newValue.toString());
				}

				Helper.getInstance().dataChanged();
				return true;
			}
		};

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences_general);
			PreferenceManager.setDefaultValues(getActivity(),
					R.xml.preferences_general, false);

			Preferences prefs = new Preferences(getActivity());

			// Default Car
			{
				Car[] cars = Car.getAll();
				String[] defaultEntries = new String[cars.length];
				String[] defaultEntryValues = new String[cars.length];
				for (int i = 0; i < cars.length; i++) {
					defaultEntries[i] = cars[i].getName();
					defaultEntryValues[i] = String.valueOf(cars[i].getId());
				}
				ListPreference defaultCar = (ListPreference) findPreference("default_car");
				defaultCar.setEntries(defaultEntries);
				defaultCar.setEntryValues(defaultEntryValues);
				defaultCar
						.setOnPreferenceChangeListener(onPreferenceChangeListener);
				Car car = new Car(prefs.getDefaultCar());
				defaultCar.setSummary(car.getName());
			}

			// Default Report
			{
				String[] defaultEntries = getResources().getStringArray(
						R.array.reports);
				String[] defaultEntryValues = new String[defaultEntries.length];
				for (int i = 0; i < defaultEntries.length; i++) {
					defaultEntryValues[i] = String.valueOf(i);
				}
				ListPreference defaultReport = (ListPreference) findPreference("default_report");
				defaultReport.setEntries(R.array.reports);
				defaultReport.setEntryValues(defaultEntryValues);
				defaultReport
						.setOnPreferenceChangeListener(onPreferenceChangeListener);
				defaultReport.setSummary(defaultEntries[prefs
						.getDefaultReport()]);
			}

			// Appearance Overall section position
			{
				String[] entries = getResources().getStringArray(
						R.array.overall_section_positions);
				String[] entryValues = new String[entries.length];
				for (int i = 0; i < entries.length; i++) {
					entryValues[i] = String.valueOf(i);
				}
				ListPreference sectionPos = (ListPreference) findPreference("appearance_overall_section_pos");
				sectionPos.setEntryValues(entryValues);
				sectionPos
						.setOnPreferenceChangeListener(onPreferenceChangeListener);
				sectionPos.setSummary(getString(
						R.string.pref_summary_appearance_overall_section_pos,
						entries[prefs.getOverallSectionPos()]));
			}

			// Appearance color sections
			{
				CheckBoxPreference colorSections = (CheckBoxPreference) findPreference("appearance_color_sections");
				colorSections
						.setOnPreferenceChangeListener(onPreferenceChangeListener);
			}

			// Appearance show legend
			{
				CheckBoxPreference colorSections = (CheckBoxPreference) findPreference("appearance_show_legend");
				colorSections
						.setOnPreferenceChangeListener(onPreferenceChangeListener);
			}

			// Unit Currency
			{
				EditTextPreference unitCurrency = (EditTextPreference) findPreference("unit_currency");
				unitCurrency
						.setOnPreferenceChangeListener(onPreferenceChangeListener);
				unitCurrency.setSummary(prefs.getUnitCurrency());
			}

			// Unit Volume
			{
				EditTextPreference unitVolume = (EditTextPreference) findPreference("unit_volume");
				unitVolume
						.setOnPreferenceChangeListener(onPreferenceChangeListener);
				unitVolume.setSummary(prefs.getUnitVolume());
			}

			// Unit Volume
			{
				EditTextPreference unitDistance = (EditTextPreference) findPreference("unit_distance");
				unitDistance
						.setOnPreferenceChangeListener(onPreferenceChangeListener);
				unitDistance.setSummary(prefs.getUnitDistance());
			}
		}
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
