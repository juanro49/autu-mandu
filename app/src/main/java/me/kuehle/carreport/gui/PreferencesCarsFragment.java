/*
 * Copyright 2013 Jan Kühle
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

import java.util.List;

import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment;
import me.kuehle.carreport.gui.dialog.MessageDialogFragment.MessageDialogFragmentListener;
import me.kuehle.carreport.util.IForEach;
import android.app.ListFragment;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PreferencesCarsFragment extends ListFragment implements
		MessageDialogFragmentListener {
	private class CarAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return cars.size();
		}

		@Override
		public Car getItem(int position) {
			return cars.get(position);
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
						R.layout.list_item_car, parent, false);

				holder = new CarViewHolder();
				holder.name = (TextView) convertView
						.findViewById(android.R.id.text1);
				holder.suspended = (TextView) convertView
						.findViewById(android.R.id.text2);
				holder.color = convertView.findViewById(android.R.id.custom);

				convertView.setTag(holder);
			} else {
				holder = (CarViewHolder) convertView
						.getTag();
			}

			holder.name.setText(cars.get(position).name);
			if (cars.get(position).isSuspended()) {
				holder.suspended.setText(getString(
						R.string.suspended_since,
						android.text.format.DateFormat.getDateFormat(
								getActivity()).format(
								cars.get(position).suspendedSince)));
				holder.suspended.setVisibility(View.VISIBLE);
			} else {
				holder.suspended.setVisibility(View.GONE);
			}

			holder.color.getBackground().setColorFilter(cars.get(position).color,
                    PorterDuff.Mode.SRC);
			return convertView;
		}
	}

	private class CarMultiChoiceModeListener implements MultiChoiceModeListener {
		private ActionMode mode;

		public void execActionAndFinish(IForEach<Car> forEach) {
			SparseBooleanArray selected = getListView()
					.getCheckedItemPositions();
			for (int i = 0; i < cars.size(); i++) {
				if (selected.get(i)) {
					forEach.action(cars.get(i));
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
			case R.id.menu_delete:
				if (getListView().getCheckedItemCount() == cars.size()) {
					MessageDialogFragment.newInstance(null, 0,
							R.string.alert_delete_title,
							getString(R.string.alert_cannot_delete_last_car),
							android.R.string.ok, null).show(
							getFragmentManager(), null);
				} else {
					String message = getString(
							R.string.alert_delete_cars_message, getListView()
									.getCheckedItemCount());
					MessageDialogFragment.newInstance(
							PreferencesCarsFragment.this, DELETE_REQUEST_CODE,
							R.string.alert_delete_title, message,
							android.R.string.yes, android.R.string.no).show(
							getFragmentManager(), null);
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
		public void onItemCheckedStateChanged(ActionMode mode, int position,
				long id, boolean checked) {
			int count = getListView().getCheckedItemCount();
			mode.setTitle(String.format(getString(R.string.cab_title_selected),
					count));
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
	}

	private static class CarViewHolder {
		public TextView name;
		public TextView suspended;
		public View color;
	}

	private static final int DELETE_REQUEST_CODE = 1;

	private List<Car> cars;
	private boolean carEditInProgress = false;

	private OnItemClickListener onItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			editCar(cars.get(position).id);
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
			multiChoiceModeListener.execActionAndFinish(new IForEach<Car>() {
				public void action(Car car) {
					car.delete();
				}
			});
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_car:
			editCar(AbstractDataDetailFragment.EXTRA_ID_DEFAULT);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (carEditInProgress) {
			carEditInProgress = false;
			fillList();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		multiChoiceModeListener.finishActionMode();
	}

	private void editCar(long id) {
		Intent intent = new Intent(getActivity(), DataDetailActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		intent.putExtra(DataDetailActivity.EXTRA_EDIT,
				DataDetailActivity.EXTRA_EDIT_CAR);
		intent.putExtra(AbstractDataDetailFragment.EXTRA_ID, id);
		carEditInProgress = true;
		startActivityForResult(intent, 0);
	}

	private void fillList() {
		cars = Car.getAll();
		setListAdapter(new CarAdapter());
	}
}