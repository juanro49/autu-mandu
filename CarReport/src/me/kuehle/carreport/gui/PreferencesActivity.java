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
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity {
	public static class AboutFragment extends Fragment {
		private View.OnClickListener licensesOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
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
				new AlertDialog.Builder(getActivity())
						.setTitle(R.string.alert_about_licenses_title)
						.setView(view)
						.setPositiveButton(android.R.string.ok, null).show();
			}
		};

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View root = inflater.inflate(R.layout.fragment_prefs_about,
					container, false);

			String strVersion = String.format(
					getString(R.string.about_version),
					getString(R.string.app_version));
			((TextView) root.findViewById(R.id.txtVersion)).setText(strVersion);
			((Button) root.findViewById(R.id.btnLicenses))
					.setOnClickListener(licensesOnClickListener);

			return root;
		}
	}

	public static class BackupFragment extends PreferenceFragment {
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
					new AlertDialog.Builder(getActivity())
							.setTitle(R.string.alert_backup_overwrite_title)
							.setMessage(R.string.alert_backup_overwrite_message)
							.setPositiveButton(R.string.overwrite,
									new OnClickListener() {
										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											doBackup();
										}
									})
							.setNegativeButton(android.R.string.cancel, null)
							.show();
				} else {
					doBackup();
				}
				return true;
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
						Toast.makeText(getActivity(),
								R.string.toast_backup_failed,
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		};

		private OnPreferenceClickListener mRestore = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				new AlertDialog.Builder(getActivity())
						.setTitle(R.string.alert_restore_title)
						.setMessage(R.string.alert_restore_message)
						.setPositiveButton(R.string.restore,
								new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										doRestore();
									}
								})
						.setNegativeButton(android.R.string.cancel, null)
						.show();
				return true;
			}

			private void doRestore() {
				synchronized (Helper.dbLock) {
					if (backup.restore()) {
						Toast.makeText(getActivity(),
								R.string.toast_restore_success,
								Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(getActivity(),
								R.string.toast_restore_failed,
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		};

		private OnPreferenceClickListener mExportCSV = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				final View view = inflater.inflate(R.layout.dialog_exportcsv,
						null);
				new AlertDialog.Builder(getActivity())
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
										doExport(option, overwrite);
									}
								})
						.setNegativeButton(android.R.string.cancel, null)
						.show();
				return true;
			}

			private void doExport(int option, boolean overwrite) {
				if (!overwrite && csvExportImport.anyExportFileExist(option)) {
					Toast.makeText(getActivity(),
							R.string.toast_export_failed_overwrite,
							Toast.LENGTH_SHORT).show();
				} else {
					csvExportImport.export(option);
					Toast.makeText(getActivity(),
							R.string.toast_export_success, Toast.LENGTH_SHORT)
							.show();
				}
			}
		};

		private OnPreferenceClickListener mImportCSV = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				LayoutInflater inflater = getActivity().getLayoutInflater();
				final View view = inflater.inflate(R.layout.dialog_importcsv,
						null);
				new AlertDialog.Builder(getActivity())
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
										doImport(option);
									}
								})
						.setNegativeButton(android.R.string.cancel, null)
						.show();
				return true;
			}

			private void doImport(int option) {
				if (!csvExportImport.allExportFilesExist(option)) {
					Toast.makeText(getActivity(),
							R.string.toast_import_files_dont_exist,
							Toast.LENGTH_SHORT).show();
				} else if (csvExportImport.import_(option)) {
					Toast.makeText(getActivity(),
							R.string.toast_import_success, Toast.LENGTH_SHORT)
							.show();
				} else {
					Toast.makeText(getActivity(), R.string.toast_import_failed,
							Toast.LENGTH_SHORT).show();
				}
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
		public void onResume() {
			super.onResume();
			
			if (dropboxAuthenticationInProgress) {
				dropboxAuthenticationInProgress = false;

				final ProgressDialog progressDialog = new ProgressDialog(
						getActivity());
				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDialog
						.setMessage(getString(R.string.alert_dropbox_finishing_authentication));
				progressDialog.setCancelable(false);
				progressDialog.show();

				dropbox.finishAuthentication(new Dropbox.OnAuthenticationFinishedListener() {
					@Override
					public void authenticationFinished(boolean success,
							String accountName, boolean remoteDataAvailable) {
						progressDialog.dismiss();
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

		private void dropboxFirstSynchronisation() {
			new AlertDialog.Builder(getActivity())
					.setTitle(R.string.alert_dropbox_first_sync_title)
					.setMessage(R.string.alert_dropbox_first_sync_message)
					.setPositiveButton(
							R.string.alert_dropbox_first_sync_download,
							new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dropbox.synchronize(Dropbox.SYNC_DOWNLOAD);
								}
							})
					.setNegativeButton(
							R.string.alert_dropbox_first_sync_upload,
							new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dropbox.synchronize(Dropbox.SYNC_UPLOAD);
								}
							}).show();
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

	public static class CarsFragment extends ListFragment {
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
								Mode.SRC_ATOP);

				return convertView;
			}
		}

		private class CarMultiChoiceModeListener implements
				MultiChoiceModeListener {
			private ActionMode mode;

			private DialogInterface.OnClickListener deleteOnClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					execActionAndFinish(new IForEach<Car>() {
						public void action(Car car) {
							car.delete();
						}
					});
				}
			};

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
						new AlertDialog.Builder(getActivity())
								.setTitle(R.string.alert_delete_title)
								.setMessage(
										R.string.alert_cannot_delete_last_car)
								.setPositiveButton(android.R.string.ok, null)
								.show();
					} else {
						String message = getString(
								R.string.alert_delete_cars_message,
								getListView().getCheckedItemCount());
						new AlertDialog.Builder(getActivity())
								.setTitle(R.string.alert_delete_title)
								.setMessage(message)
								.setPositiveButton(android.R.string.yes,
										deleteOnClickListener)
								.setNegativeButton(android.R.string.no, null)
								.show();
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

			private void execActionAndFinish(IForEach<Car> forEach) {
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
		}

		private static class CarViewHolder {
			public TextView name;
			public TextView suspended;
			public Button color;
		}

		private Car[] cars;

		private OnItemClickListener onItemClickListener = new OnItemClickListener() {
			private Car editCar;
			private EditText editInput;

			private DialogInterface.OnClickListener positiveOnClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String name = editInput.getText().toString();
					if (name.length() > 0) {
						editCar.setName(name);
						editCar.save();
						fillList();
					}
				}
			};

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				editCar = cars[position];

				editInput = new EditText(getActivity());
				editInput.setText(editCar.getName());
				new AlertDialog.Builder(getActivity())
						.setTitle(R.string.alert_edit_car_title)
						.setMessage(R.string.alert_add_car_message)
						.setView(editInput)
						.setPositiveButton(android.R.string.ok,
								positiveOnClickListener)
						.setNegativeButton(android.R.string.cancel, null)
						.show();
			}
		};

		private View.OnClickListener colorOnClickListener = new View.OnClickListener() {
			private Car editCar;

			private DialogInterface.OnClickListener selectItemOnClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					editCar.setColor(getResources().getIntArray(R.array.colors)[which]);
					editCar.save();
					fillList();
					dialog.dismiss();
				}
			};

			@Override
			public void onClick(View v) {
				final int position = getListView().getPositionForView(v);
				if (position != ListView.INVALID_POSITION) {
					editCar = cars[position];
					int[] colors = getResources().getIntArray(R.array.colors);
					int colorIndex = -1;
					for (int i = 0; i < colors.length; i++) {
						if (colors[i] == editCar.getColor()) {
							colorIndex = i;
						}
					}

					new AlertDialog.Builder(getActivity())
							.setTitle(R.string.alert_change_color_title)
							.setSingleChoiceItems(R.array.color_names,
									colorIndex, selectItemOnClickListener)
							.setNegativeButton(android.R.string.cancel, null)
							.show();
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
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case R.id.menu_add_car:
				final EditText editInput = new EditText(getActivity());
				new AlertDialog.Builder(getActivity())
						.setTitle(R.string.alert_add_car_title)
						.setMessage(R.string.alert_add_car_message)
						.setView(editInput)
						.setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										String name = editInput.getText()
												.toString();
										if (name.length() > 0) {
											Car.create(name, Color.BLUE, null);
											fillList();
										}
									}
								})
						.setNegativeButton(android.R.string.cancel, null)
						.show();
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
