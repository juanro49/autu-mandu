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

import java.util.ArrayList;
import java.util.HashMap;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.AbstractItem;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.Refueling;
import me.kuehle.carreport.util.RecurrenceInterval;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

public class DataListFragment extends Fragment {
	public abstract class AbstractTabHelper {
		protected AbstractItem[] mItems;
		protected ListView mListView;

		public AbstractTabHelper(ListView listView) {
			mListView = listView;
		}

		protected abstract int getAlertDeleteManyMessage();

		protected abstract int getIndicator();

		protected abstract String getTag();

		protected abstract int getExtraEdit();

		protected abstract int getView();

		protected abstract void updateItems(Car car);

		protected abstract void updateListAdapter();
	}

	public interface Callback {
		public abstract void onItemClosed();

		public abstract void onItemSelected(int edit, int carId, int id);

		public abstract void onTabChanged(int edit);
	}

	public class OtherCostsTabHelper extends AbstractTabHelper {
		public OtherCostsTabHelper(ListView listView) {
			super(listView);
		}

		@Override
		protected int getAlertDeleteManyMessage() {
			return R.string.alert_delete_others_message;
		}

		@Override
		protected int getIndicator() {
			return R.string.tab_indicator_other;
		}

		@Override
		protected String getTag() {
			return "otherCosts";
		}

		@Override
		protected int getView() {
			return R.id.tabOtherCosts;
		}

		@Override
		protected void updateItems(Car car) {
			mItems = OtherCost.getAllForCar(car, false);
		}

		@Override
		protected void updateListAdapter() {
			ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
			SimpleAdapter adapter = new SimpleAdapter(getActivity(), data,
					R.layout.two_line_list_item_5, new String[] { "text_tl",
							"text_tr", "text_bl", "text_bm", "text_br" },
					new int[] { R.id.text_tl, R.id.text_tr, R.id.text_bl,
							R.id.text_bm, R.id.text_br });

			Preferences prefs = new Preferences(getActivity());
			java.text.DateFormat dateFmt = DateFormat
					.getDateFormat(getActivity());
			String[] repIntervals = getResources().getStringArray(
					R.array.repeat_intervals);
			for (AbstractItem item : mItems) {
				OtherCost other = (OtherCost) item;
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("text_tl", dateFmt.format(other.getDate()));
				map.put("text_tr", other.getTitle());
				if (other.getMileage() > -1) {
					map.put("text_bl",
							String.format("%d %s", other.getMileage(),
									prefs.getUnitDistance()));
				} else {
					map.put("text_bl", "");
				}
				if (other.getRecurrence().getInterval()
						.equals(RecurrenceInterval.ONCE)) {
					map.put("text_bm", "");
				} else {
					map.put("text_bm", repIntervals[other.getRecurrence()
							.getInterval().getValue()]);
				}
				map.put("text_br",
						String.format("%.2f %s", other.getPrice(),
								prefs.getUnitCurrency()));
				data.add(map);
			}

			mListView.setAdapter(adapter);
		}

		@Override
		protected int getExtraEdit() {
			return DataDetailActivity.EXTRA_EDIT_OTHER;
		}
	}

	public class RefuelingsTabHelper extends AbstractTabHelper {
		public RefuelingsTabHelper(ListView listView) {
			super(listView);
		}

		@Override
		protected int getAlertDeleteManyMessage() {
			return R.string.alert_delete_refuelings_message;
		}

		@Override
		protected int getIndicator() {
			return R.string.tab_indicator_refuelings;
		}

		@Override
		protected String getTag() {
			return "refuelings";
		}

		@Override
		protected int getView() {
			return R.id.tabRefuelings;
		}

		@Override
		protected void updateItems(Car car) {
			mItems = Refueling.getAllForCar(car, false);
		}

		@Override
		protected void updateListAdapter() {
			ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
			SimpleAdapter adapter = new SimpleAdapter(getActivity(), data,
					R.layout.two_line_list_item_5, new String[] { "text_tl",
							"text_tr", "text_bl", "text_bm", "text_br" },
					new int[] { R.id.text_tl, R.id.text_tr, R.id.text_bl,
							R.id.text_bm, R.id.text_br });

			Preferences prefs = new Preferences(getActivity());
			java.text.DateFormat dateFmt = DateFormat
					.getDateFormat(getActivity());
			for (AbstractItem item : mItems) {
				Refueling refueling = (Refueling) item;
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("text_tl", dateFmt.format(refueling.getDate()));
				map.put("text_tr",
						refueling.isPartial() ? getString(R.string.label_partial)
								: "");
				map.put("text_bl",
						String.format("%d %s", refueling.getMileage(),
								prefs.getUnitDistance()));
				map.put("text_bm",
						String.format("%.2f %s", refueling.getVolume(),
								prefs.getUnitVolume()));
				map.put("text_br",
						String.format("%.2f %s", refueling.getPrice(),
								prefs.getUnitCurrency()));
				data.add(map);
			}

			mListView.setAdapter(adapter);
		}

		@Override
		protected int getExtraEdit() {
			return DataDetailActivity.EXTRA_EDIT_REFUELING;
		}
	}

	private static final String STATE_CURRENT_ITEM = "current_position";
	private static final String STATE_CURRENT_CAR = "current_car";
	private static final String STATE_CURRENT_TAB = "current_tab";

	private Car[] mCars;
	private Car mCurrentCar = null;

	private TabHost mTabHost;
	private AbstractTabHelper[] mTabs;
	private AbstractTabHelper mCurrentTab;

	private int mCurrentItem = ListView.INVALID_POSITION;
	private boolean mActivateOnClick = false;
	private ActionMode mActionMode = null;
	private boolean dontStartActionMode = false;

	private Callback mCallback;

	private OnTabChangeListener mOnTabChangeListener = new OnTabChangeListener() {
		@Override
		public void onTabChanged(String tabId) {
			setCurrentPosition(ListView.INVALID_POSITION);
			mCallback.onItemClosed();
			mCurrentTab = mTabs[mTabHost.getCurrentTab()];
			mCallback.onTabChanged(mCurrentTab.getExtraEdit());
		}
	};

	private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			setCurrentPosition(position);

			int edit = mCurrentTab.getExtraEdit();
			int itemId = mCurrentTab.mItems[mCurrentItem].getId();
			mCallback.onItemSelected(edit, mCurrentCar.getId(), itemId);
		}
	};

	private MultiChoiceModeListener mMultiChoiceModeListener = new MultiChoiceModeListener() {
		private DialogInterface.OnClickListener deleteOnClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				SparseBooleanArray selected = mCurrentTab.mListView
						.getCheckedItemPositions();
				for (int i = 0; i < mCurrentTab.mItems.length; i++) {
					if (selected.get(i)) {
						mCurrentTab.mItems[i].delete();
					}
				}
				mActionMode.finish();
				mCurrentTab.updateItems(mCurrentCar);
				mCurrentTab.updateListAdapter();
			}
		};

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.menu_delete:
				String message = String.format(
						getString(mCurrentTab.getAlertDeleteManyMessage()),
						mCurrentTab.mListView.getCheckedItemCount());
				new AlertDialog.Builder(getActivity())
						.setTitle(R.string.alert_delete_title)
						.setMessage(message)
						.setPositiveButton(android.R.string.yes,
								deleteOnClickListener)
						.setNegativeButton(android.R.string.no, null).show();
				return true;
			default:
				return false;
			}
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			if (dontStartActionMode) {
				dontStartActionMode = false;
				return false;
			}

			mCallback.onItemClosed();
			mCurrentItem = ListView.INVALID_POSITION;
			mCurrentTab.mListView.setOnItemClickListener(null);

			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.cab_delete, menu);

			mActionMode = mode;
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mCurrentTab.mListView.setOnItemClickListener(mOnItemClickListener);
			mActionMode = null;
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position,
				long id, boolean checked) {
			int count = mCurrentTab.mListView.getCheckedItemCount();
			mode.setTitle(String.format(getString(R.string.cab_title_selected),
					count));
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}
	};

	private OnNavigationListener mListNavigationCallback = new OnNavigationListener() {
		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			mCurrentCar = mCars[itemPosition];
			mCallback.onItemClosed();
			updateLists();
			return true;
		}
	};

	private void addTab(String tag, int indicatorId, int contentId) {
		TabSpec tabSpec = mTabHost.newTabSpec(tag);
		tabSpec.setIndicator(getString(indicatorId));
		tabSpec.setContent(contentId);
		mTabHost.addTab(tabSpec);
	}

	public Car getCurrentCar() {
		return mCurrentCar;
	}

	public boolean isItemActivated() {
		return mCurrentTab.mListView.getCheckedItemCount() > 0;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mCallback = (Callback) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement ViewDataListListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_dropdown_item);
		mCars = Car.getAll();
		for (Car car : mCars) {
			adapter.add(car.getName());
		}
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter, mListNavigationCallback);

		// Set selected car from config instance of preferences
		Preferences prefs = new Preferences(getActivity());
		if (savedInstanceState != null) {
			selectCarById(savedInstanceState.getInt(STATE_CURRENT_CAR,
					prefs.getDefaultCar()));
		} else {
			selectCarById(prefs.getDefaultCar());
		}

		mTabHost = (TabHost) inflater.inflate(R.layout.fragment_data_list,
				container, false);

		mTabs = new AbstractTabHelper[2];
		mTabs[0] = new RefuelingsTabHelper(
				(ListView) mTabHost.findViewById(R.id.tabRefuelings));
		mTabs[1] = new OtherCostsTabHelper(
				(ListView) mTabHost.findViewById(R.id.tabOtherCosts));

		mTabHost.setup();
		for (AbstractTabHelper tab : mTabs) {
			addTab(tab.getTag(), tab.getIndicator(), tab.getView());

			tab.mListView.setOnItemClickListener(mOnItemClickListener);
			tab.mListView.setMultiChoiceModeListener(mMultiChoiceModeListener);
			tab.mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		}

		int position = ListView.INVALID_POSITION;
		if (savedInstanceState != null) {
			mTabHost.setCurrentTab(savedInstanceState.getInt(STATE_CURRENT_TAB,
					0));
			position = savedInstanceState.getInt(STATE_CURRENT_ITEM,
					ListView.INVALID_POSITION);
		}
		mCurrentTab = mTabs[mTabHost.getCurrentTab()];
		setCurrentPosition(position);

		mTabHost.setOnTabChangedListener(mOnTabChangeListener);
		updateLists();

		return mTabHost;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallback = null;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_CURRENT_CAR, mCurrentCar.getId());
		outState.putInt(STATE_CURRENT_TAB, mTabHost.getCurrentTab());
		outState.putInt(STATE_CURRENT_ITEM, mCurrentItem);
	}

	private void selectCarById(int id) {
		ActionBar actionBar = getActivity().getActionBar();
		for (int pos = 0; pos < mCars.length; pos++) {
			if (mCars[pos].getId() == id) {
				mCurrentCar = mCars[pos];
				actionBar.setSelectedNavigationItem(pos);
			}
		}
	}

	public void setActivateOnItemClick(boolean activate) {
		mActivateOnClick = activate;
	}

	public void setCurrentPosition(int position) {
		if (mActionMode != null) {
			mActionMode.finish();
		}

		mCurrentTab.mListView.setItemChecked(mCurrentItem, false);
		if (position != ListView.INVALID_POSITION && mActivateOnClick) {
			dontStartActionMode = true;
			mCurrentTab.mListView.setItemChecked(position, true);
		}
		mCurrentItem = position;
	}

	public void updateLists() {
		setCurrentPosition(ListView.INVALID_POSITION);
		for (AbstractTabHelper tab : mTabs) {
			tab.updateItems(mCurrentCar);
			tab.updateListAdapter();
		}
	}
}
