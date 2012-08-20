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

import java.util.ArrayList;
import java.util.HashMap;

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

public class ViewDataListFragment extends Fragment {
	private Car[] mCars;
	private Car mCurrentCar = null;
	private TabHost mTabHost;
	private AbstractTabHelper[] mTabs;
	private AbstractTabHelper mCurrentTab;
	private int mCurrentItem = -1;
	private boolean mDontStartActionMode = false;
	private ActionMode mActionMode = null;

	private ViewDataListListener mViewDataListListener;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mViewDataListListener = (ViewDataListListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement ViewDataListListener");
		}

		// ActionBar
		ActionBar actionBar = activity.getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
				android.R.layout.simple_spinner_dropdown_item);
		mCars = Car.getAll();
		for (Car car : mCars) {
			adapter.add(car.getName());
		}
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter, mListNavigationCallback);

		// Set selected car from config instance of preferences
		Preferences prefs = new Preferences(activity);
		selectCarById(prefs.getDefaultCar());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mTabHost = (TabHost) inflater.inflate(R.layout.view_data_list,
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

		if (savedInstanceState != null) {
			selectCarById(savedInstanceState.getInt("curCar", 0));
			mTabHost.setCurrentTab(savedInstanceState.getInt("curTab", 0));
			mCurrentItem = savedInstanceState.getInt("curItem", -1);
		}
		mCurrentTab = mTabs[mTabHost.getCurrentTab()];

		mTabHost.setOnTabChangedListener(mOnTabChangeListener);
		updateLists();

		return mTabHost;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("curCar", mCurrentCar.getId());
		outState.putInt("curTab", mTabHost.getCurrentTab());
		outState.putInt("curItem", mCurrentItem);
	}

	public Car getCurrentCar() {
		return mCurrentCar;
	}

	public void openItem(int position) {
		mCurrentTab.setId(mCurrentTab.mItems[position].getId());
		if (mViewDataListListener.isDualPane()) {
			if (mCurrentItem != position) {
				mDontStartActionMode = true;
				mCurrentTab.mListView.clearChoices();
				mCurrentTab.mListView.setItemChecked(position, true);
				mViewDataListListener.openItem(mCurrentTab);
			}
		} else {
			mViewDataListListener.openItem(mCurrentTab);
		}
		mCurrentItem = position;
	}

	public void unselectAll() {
		mCurrentTab.mListView.clearChoices();
		mCurrentTab.mListView.requestLayout();
		mCurrentItem = -1;
		if (mActionMode != null) {
			mActionMode.finish();
		}
	}

	public void updateLists() {
		if (isVisible()) {
			mViewDataListListener.closeCurrentItem();
			unselectAll();
		}
		for (AbstractTabHelper tab : mTabs) {
			tab.updateItems(mCurrentCar);
			tab.updateListAdapter();
		}
	}

	private void addTab(String tag, int indicatorId, int contentId) {
		TabSpec tabSpec = mTabHost.newTabSpec(tag);
		tabSpec.setIndicator(getString(indicatorId));
		tabSpec.setContent(contentId);
		mTabHost.addTab(tabSpec);
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

	private OnTabChangeListener mOnTabChangeListener = new OnTabChangeListener() {
		@Override
		public void onTabChanged(String tabId) {
			mViewDataListListener.closeCurrentItem();
			unselectAll();
			mCurrentTab = mTabs[mTabHost.getCurrentTab()];
		}
	};

	private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			openItem(position);
		}
	};

	private MultiChoiceModeListener mMultiChoiceModeListener = new MultiChoiceModeListener() {
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mCurrentTab.mListView.setOnItemClickListener(mOnItemClickListener);
			mActionMode = null;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			if (mDontStartActionMode) {
				mDontStartActionMode = false;
				return false;
			}

			mViewDataListListener.closeCurrentItem();
			mCurrentItem = -1;
			mCurrentTab.mListView.setOnItemClickListener(null);

			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.cab_delete, menu);

			mActionMode = mode;
			return true;
		}

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
		public void onItemCheckedStateChanged(ActionMode mode, int position,
				long id, boolean checked) {
			int count = mCurrentTab.mListView.getCheckedItemCount();
			mode.setTitle(String.format(getString(R.string.cab_title_selected),
					count));
		}

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
	};

	private OnNavigationListener mListNavigationCallback = new OnNavigationListener() {
		private boolean first = true;

		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			if (first) {
				first = false;
			} else {
				mCurrentCar = mCars[itemPosition];
				updateLists();
			}
			return true;
		}
	};

	public abstract class AbstractEditHelper {
		protected abstract AbstractEditFragment createEditFragment();

		protected abstract int getId();
	}

	public abstract class AbstractTabHelper extends AbstractEditHelper {
		protected AbstractItem[] mItems;
		protected ListView mListView;

		private int mId;

		protected abstract int getAlertDeleteManyMessage();

		@Override
		protected int getId() {
			return mId;
		}

		protected abstract int getIndicator();

		protected abstract String getTag();

		protected abstract int getView();

		protected void setId(int id) {
			mId = id;
		}

		protected abstract void updateItems(Car car);

		protected abstract void updateListAdapter();
	}

	public class RefuelingsTabHelper extends AbstractTabHelper {
		public RefuelingsTabHelper(ListView listView) {
			mListView = listView;
		}

		@Override
		protected AbstractEditFragment createEditFragment() {
			return EditRefuelingFragment.newInstance(getId());
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
	}

	public class OtherCostsTabHelper extends AbstractTabHelper {
		public OtherCostsTabHelper(ListView listView) {
			mListView = listView;
		}

		@Override
		protected AbstractEditFragment createEditFragment() {
			return EditOtherCostFragment.newInstance(getId());
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
	}

	public interface ViewDataListListener {
		public abstract void closeCurrentItem();

		public abstract boolean isDualPane();

		public abstract void openItem(AbstractEditHelper helper);
	}
}
