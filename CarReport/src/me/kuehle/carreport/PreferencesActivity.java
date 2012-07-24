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

package me.kuehle.carreport;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import me.kuehle.carreport.db.Car;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ListFragment;
import android.app.backup.BackupManager;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

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

			addPreferencesFromResource(R.xml.preferences_defaults);
			PreferenceManager.setDefaultValues(getActivity(),
					R.xml.preferences_defaults, false);

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
				sectionPos.setSummary(entries[prefs.getOverallSectionPos()]);
			}

			// Appearance color sections
			{
				CheckBoxPreference colorSections = (CheckBoxPreference) findPreference("appearance_color_sections");
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
					preference.setSummary(positions[Integer.parseInt(newValue
							.toString())]);
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

	public static class AboutFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View root = inflater.inflate(R.layout.about, container, false);

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
