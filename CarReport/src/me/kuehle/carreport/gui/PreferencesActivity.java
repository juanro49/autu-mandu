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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.CarTable;
import me.kuehle.carreport.db.Helper;
import me.kuehle.carreport.db.OtherCostTable;
import me.kuehle.carreport.db.RefuelingTable;
import me.kuehle.carreport.util.CSVWriter;
import me.kuehle.carreport.util.Strings;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.backup.BackupManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
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
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
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

	public static class GeneralFragment extends PreferenceFragment {
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
				BackupManager backupManager = new BackupManager(getActivity()
						.getApplicationContext());
				backupManager.dataChanged();
				return true;
			}
		};
	}

	public static class CarsFragment extends ListFragment {
		private Car[] cars;
		private EditText editInput;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setHasOptionsMenu(true);
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			getListView().setOnItemClickListener(onItemClickListener);
			getListView().setMultiChoiceModeListener(multiChoiceModeListener);
			getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

			fillList();
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			inflater.inflate(R.menu.edit_cars, menu);
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			switch (item.getItemId()) {
			case R.id.menu_add_car:
				editInput = new EditText(getActivity());
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
											Car.create(name, Color.BLUE);
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

		private void fillList() {
			cars = Car.getAll();
			setListAdapter(new CarAdapter());
		}

		private OnItemClickListener onItemClickListener = new OnItemClickListener() {
			private Car editCar;

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

			private DialogInterface.OnClickListener positiveOnClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String name = editInput.getText().toString();
					if (name.length() > 0) {
						editCar.setName(name);
						fillList();
					}
				}
			};
		};

		private View.OnClickListener colorOnClickListener = new View.OnClickListener() {
			private Car editCar;

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

			private DialogInterface.OnClickListener selectItemOnClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					editCar.setColor(getResources().getIntArray(R.array.colors)[which]);
					fillList();
					dialog.dismiss();
				}
			};
		};

		private MultiChoiceModeListener multiChoiceModeListener = new MultiChoiceModeListener() {
			private ActionMode mode;

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.cab_delete, menu);
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case R.id.menu_delete:
					this.mode = mode;

					if (getListView().getCheckedItemCount() == cars.length) {
						new AlertDialog.Builder(getActivity())
								.setTitle(R.string.alert_delete_title)
								.setMessage(
										R.string.alert_cannot_delete_last_car)
								.setPositiveButton(android.R.string.ok, null)
								.show();
					} else {
						String message = String.format(
								getString(R.string.alert_delete_cars_message),
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
			public void onItemCheckedStateChanged(ActionMode mode,
					int position, long id, boolean checked) {
				int count = getListView().getCheckedItemCount();
				mode.setTitle(String.format(
						getString(R.string.cab_title_selected), count));
			}

			private DialogInterface.OnClickListener deleteOnClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					SparseBooleanArray selected = getListView()
							.getCheckedItemPositions();
					for (int i = 0; i < cars.length; i++) {
						if (selected.get(i)) {
							cars[i].delete();
						}
					}
					mode.finish();
					fillList();
				}
			};
		};

		private static class CarViewHolder {
			public TextView name;
			public Button color;
		}

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
							R.layout.split_list_item_1, parent, false);

					holder = new CarViewHolder();
					holder.name = (TextView) convertView
							.findViewById(android.R.id.text1);
					holder.color = ((Button) convertView
							.findViewById(android.R.id.button1));
					holder.color.setBackgroundResource(R.drawable.color_button);
					holder.color.setOnClickListener(colorOnClickListener);

					convertView.setTag(holder);
				} else {
					holder = (CarViewHolder) convertView.getTag();
				}

				holder.name.setText(cars[position].getName());
				((GradientDrawable) holder.color.getBackground())
						.setColorFilter(cars[position].getColor(),
								Mode.SRC_ATOP);

				return convertView;
			}
		}
	}

	public static class BackupFragment extends PreferenceFragment {
		private static final String BACKUP_FILE_NAME = "carreport.backup";
		private static final String EXPORT_FILE_PREFIX = "carreport_export";
		private File dbFile;
		private File backupFile;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences_backup);

			dbFile = new File(Helper.getInstance().getReadableDatabase()
					.getPath());
			File dir = Environment.getExternalStorageDirectory();
			backupFile = new File(dir, BACKUP_FILE_NAME);

			// Backup
			{
				Preference backup = findPreference("backup");
				backup.setEnabled(dir.canWrite());
				backup.setOnPreferenceClickListener(mBackup);
			}

			// Restore
			{
				setupRestorePreference();
			}

			// Export CSV
			{
				Preference export = findPreference("exportcsv");
				export.setEnabled(dir.canWrite());
				export.setOnPreferenceClickListener(mExportCSV);
			}
		}

		private boolean copyFile(File from, File to) {
			try {
				FileInputStream inStream = new FileInputStream(from);
				FileOutputStream outStream = new FileOutputStream(to);
				FileChannel src = inStream.getChannel();
				FileChannel dst = outStream.getChannel();
				dst.transferFrom(src, 0, src.size());
				src.close();
				dst.close();
				inStream.close();
				outStream.close();
				return true;
			} catch (Exception e) {
				return false;
			}
		}

		private void setupRestorePreference() {
			Preference restore = findPreference("restore");
			if (backupFile.exists()) {
				restore.setSummary(getString(R.string.pref_summary_restore,
						BACKUP_FILE_NAME));
				restore.setEnabled(true);
			} else {
				restore.setSummary(getString(
						R.string.pref_summary_restore_no_data, BACKUP_FILE_NAME));
				restore.setEnabled(false);
			}
			restore.setOnPreferenceClickListener(mRestore);
		}

		private OnPreferenceClickListener mBackup = new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if (backupFile.exists()) {
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
					if (copyFile(dbFile, backupFile)) {
						Toast.makeText(
								getActivity(),
								getString(R.string.toast_backup_success,
										BACKUP_FILE_NAME), Toast.LENGTH_SHORT)
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
					if (copyFile(backupFile, dbFile)) {
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
				Helper helper = Helper.getInstance();
				File dir = Environment.getExternalStorageDirectory();
				if (option == 0) { // Single file
					File export = new File(dir, EXPORT_FILE_PREFIX + ".csv");
					if (!overwrite && export.exists()) {
						Toast.makeText(getActivity(),
								R.string.toast_export_failed_overwrite,
								Toast.LENGTH_SHORT).show();
						return;
					}

					// Build SQL select statement
					HashMap<String, String> replacements = new HashMap<String, String>();
					replacements.put(
							"%r_columns",
							Strings.join(new String[] {
									"'Refueling' AS title",
									RefuelingTable.COL_DATE,
									RefuelingTable.COL_TACHO,
									RefuelingTable.COL_VOLUME,
									RefuelingTable.COL_PRICE,
									RefuelingTable.COL_PARTIAL,
									"'0' AS repeat_interval",
									"'1' AS repeat_multiplier",
									RefuelingTable.COL_NOTE,
									CarTable.NAME + "." + CarTable.COL_NAME
											+ " AS carname",
									CarTable.NAME + "." + CarTable.COL_COLOR
											+ " AS carcolor" }, ", "));
					replacements.put(
							"%o_columns",
							Strings.join(new String[] {
									OtherCostTable.COL_TITLE,
									OtherCostTable.COL_DATE,
									OtherCostTable.COL_TACHO,
									"'' AS volume",
									OtherCostTable.COL_PRICE,
									"'0' AS partial",
									OtherCostTable.COL_REP_INT,
									OtherCostTable.COL_REP_MULTI,
									OtherCostTable.COL_NOTE,
									CarTable.NAME + "." + CarTable.COL_NAME
											+ " AS carname",
									CarTable.NAME + "." + CarTable.COL_COLOR
											+ " AS carcolor" }, ", "));
					replacements.put("%refuelings", RefuelingTable.NAME);
					replacements.put("%othercosts", OtherCostTable.NAME);
					replacements.put("%cars", CarTable.NAME);
					replacements.put("%r_car_id", RefuelingTable.COL_CAR);
					replacements.put("%o_car_id", OtherCostTable.COL_CAR);
					replacements.put("%id", BaseColumns._ID);
					String sql = Strings
							.replaceMap(
									"SELECT %r_columns "
											+ "FROM %refuelings "
											+ "JOIN %cars ON %refuelings.%r_car_id = %cars.%id "
											+ "UNION ALL SELECT %o_columns "
											+ "FROM %othercosts "
											+ "JOIN %cars ON %othercosts.%o_car_id = %cars.%id",
									replacements);

					CSVWriter writer = new CSVWriter();
					synchronized (Helper.dbLock) {
						SQLiteDatabase db = helper.getReadableDatabase();
						Cursor cursor = db.rawQuery(sql, null);
						writer.write(cursor, true);
						cursor.close();
					}

					writer.toFile(export);
				} else if (option == 1) { // Two files
					File exportRefuelings = new File(dir, EXPORT_FILE_PREFIX
							+ "_refuelings.csv");
					File exportOtherCosts = new File(dir, EXPORT_FILE_PREFIX
							+ "_othercosts.csv");
					if (!overwrite
							&& (exportRefuelings.exists() || exportOtherCosts
									.exists())) {
						Toast.makeText(getActivity(),
								R.string.toast_export_failed_overwrite,
								Toast.LENGTH_SHORT).show();
						return;
					}

					// Build SQL select statement for refuelings
					HashMap<String, String> replacementsRefuelings = new HashMap<String, String>();
					replacementsRefuelings.put(
							"%columns",
							Strings.join(new String[] {
									RefuelingTable.COL_DATE,
									RefuelingTable.COL_TACHO,
									RefuelingTable.COL_VOLUME,
									RefuelingTable.COL_PRICE,
									RefuelingTable.COL_PARTIAL,
									RefuelingTable.COL_NOTE,
									CarTable.NAME + "." + CarTable.COL_NAME
											+ " AS carname",
									CarTable.NAME + "." + CarTable.COL_COLOR
											+ " AS carcolor" }, ", "));
					replacementsRefuelings.put("%refuelings",
							RefuelingTable.NAME);
					replacementsRefuelings.put("%cars", CarTable.NAME);
					replacementsRefuelings.put("%car_id",
							RefuelingTable.COL_CAR);
					replacementsRefuelings.put("%id", BaseColumns._ID);
					String sqlRefuelings = Strings
							.replaceMap(
									"SELECT %columns "
											+ "FROM %refuelings "
											+ "JOIN %cars ON %refuelings.%car_id = %cars.%id ",
									replacementsRefuelings);

					// Build SQL select statement for other costs
					HashMap<String, String> replacementsOtherCosts = new HashMap<String, String>();
					replacementsOtherCosts.put(
							"%columns",
							Strings.join(new String[] {
									OtherCostTable.COL_TITLE,
									OtherCostTable.COL_DATE,
									OtherCostTable.COL_TACHO,
									OtherCostTable.COL_PRICE,
									OtherCostTable.COL_REP_INT,
									OtherCostTable.COL_REP_MULTI,
									OtherCostTable.COL_NOTE,
									CarTable.NAME + "." + CarTable.COL_NAME
											+ " AS carname",
									CarTable.NAME + "." + CarTable.COL_COLOR
											+ " AS carcolor" }, ", "));
					replacementsOtherCosts.put("%othercosts",
							OtherCostTable.NAME);
					replacementsOtherCosts.put("%cars", CarTable.NAME);
					replacementsOtherCosts.put("%car_id",
							OtherCostTable.COL_CAR);
					replacementsOtherCosts.put("%id", BaseColumns._ID);
					String sqlOtherCosts = Strings
							.replaceMap(
									"SELECT %columns "
											+ "FROM %othercosts "
											+ "JOIN %cars ON %othercosts.%car_id = %cars.%id",
									replacementsOtherCosts);

					CSVWriter writerRefuelings = new CSVWriter();
					CSVWriter writerOtherCosts = new CSVWriter();
					synchronized (Helper.dbLock) {
						SQLiteDatabase db = helper.getReadableDatabase();
						Cursor cursor = db.rawQuery(sqlRefuelings, null);
						writerRefuelings.write(cursor, true);
						cursor.close();
						cursor = db.rawQuery(sqlOtherCosts, null);
						writerOtherCosts.write(cursor, true);
						cursor.close();
					}

					writerRefuelings.toFile(exportRefuelings);
					writerOtherCosts.toFile(exportOtherCosts);
				} else if (option == 2) { // Three files
					File exportCars = new File(dir, EXPORT_FILE_PREFIX
							+ "_cars.csv");
					File exportRefuelings = new File(dir, EXPORT_FILE_PREFIX
							+ "_refuelings.csv");
					File exportOtherCosts = new File(dir, EXPORT_FILE_PREFIX
							+ "_othercosts.csv");
					if (!overwrite
							&& (exportCars.exists()
									|| exportRefuelings.exists() || exportOtherCosts
										.exists())) {
						Toast.makeText(getActivity(),
								R.string.toast_export_failed_overwrite,
								Toast.LENGTH_SHORT).show();
						return;
					}

					CSVWriter writerCars = new CSVWriter();
					CSVWriter writerRefuelings = new CSVWriter();
					CSVWriter writerOtherCosts = new CSVWriter();
					synchronized (Helper.dbLock) {
						SQLiteDatabase db = helper.getReadableDatabase();
						Cursor cursor = db.query(CarTable.NAME, null, null,
								null, null, null, null);
						writerCars.write(cursor, true);
						cursor.close();
						cursor = db.query(RefuelingTable.NAME, null, null,
								null, null, null, null);
						writerRefuelings.write(cursor, true);
						cursor.close();
						cursor = db.query(OtherCostTable.NAME, null, null,
								null, null, null, null);
						writerOtherCosts.write(cursor, true);
						cursor.close();
					}

					writerCars.toFile(exportCars);
					writerRefuelings.toFile(exportRefuelings);
					writerOtherCosts.toFile(exportOtherCosts);
				}

				Toast.makeText(getActivity(), R.string.toast_export_success,
						Toast.LENGTH_SHORT).show();
			}
		};
	}

	public static class AboutFragment extends Fragment {
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
	}
}
