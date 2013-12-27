package me.kuehle.carreport.gui;

import java.util.ArrayList;
import java.util.List;

import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.gui.DataFragment.DataListListener;
import me.kuehle.carreport.gui.dialog.SupportMessageDialogFragment;
import me.kuehle.carreport.gui.dialog.SupportMessageDialogFragment.SupportMessageDialogFragmentListener;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.activeandroid.Model;

public abstract class AbstractDataListFragment<T extends Model> extends
		ListFragment implements SupportMessageDialogFragmentListener,
		DataListListener {
	public static interface DataListCallback {
		public abstract void onItemSelected(int edit, long id);

		public abstract void onItemUnselected();
	}

	private class DataListAdapter extends BaseAdapter {
		private List<T> mItems = new ArrayList<T>();
		private final int[] fields = { R.id.title, R.id.subtitle, R.id.date,
				R.id.data1, R.id.data1_calculated, R.id.data2,
				R.id.data2_calculated, R.id.data3, R.id.data3_calculated };

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public T getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return mItems.get(position).id;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(
						R.layout.list_item_data, parent, false);
			}

			SparseArray<String> item = getItemData(mItems, position);
			for (int field : fields) {
				TextView textView = (TextView) convertView.findViewById(field);
				String value = item.get(field);
				if (value != null) {
					textView.setText(value);
					textView.setVisibility(View.VISIBLE);
				} else if (field == R.id.subtitle) {
					textView.setVisibility(View.GONE);
				} else {
					textView.setVisibility(View.INVISIBLE);
				}
			}

			return convertView;
		}

		public void update() {
			mItems = getItems();
			notifyDataSetChanged();
		}
	}

	private class DataListMultiChoiceModeListener implements
			MultiChoiceModeListener {
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch (item.getItemId()) {
			case R.id.menu_delete:
				String message = String.format(
						getString(getAlertDeleteManyMessage()), getListView()
								.getCheckedItemCount());
				SupportMessageDialogFragment.newInstance(
						AbstractDataListFragment.this, REQUEST_DELETE,
						R.string.alert_delete_title, message,
						android.R.string.yes, android.R.string.no).show(
						getFragmentManager(), null);
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

			mCurrentItem = ListView.INVALID_POSITION;
			mDataListCallback.onItemUnselected();

			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.view_data_cab, menu);

			mActionMode = mode;
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
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

	public static final String EXTRA_ACTIVATE_ON_CLICK = "activate_on_click";
	public static final boolean EXTRA_ACTIVATE_ON_CLICK_DEFAULT = false;
	public static final String EXTRA_CAR_ID = "car_id";

	private static final String STATE_CURRENT_CAR = "current_car";
	private static final String STATE_CURRENT_ITEM = "current_item";
	private static final int REQUEST_DELETE = 0;

	protected Car mCar = null;
	private DataListAdapter mListAdapter;
	private int mCurrentItem = ListView.INVALID_POSITION;
	private DataListCallback mDataListCallback;
	private boolean mActivateOnClick = false;
	private ActionMode mActionMode = null;
	private boolean dontStartActionMode = false;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mListAdapter = new DataListAdapter();
		if (mCar != null) {
			mListAdapter.update();
		}

		setListAdapter(mListAdapter);

		getListView().setMultiChoiceModeListener(
				new DataListMultiChoiceModeListener());
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

		if (savedInstanceState != null) {
			setCurrentPosition(savedInstanceState.getInt(STATE_CURRENT_ITEM,
					ListView.INVALID_POSITION));
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			if (getParentFragment() != null) {
				mDataListCallback = (DataListCallback) getParentFragment();
			} else {
				mDataListCallback = (DataListCallback) activity;
			}
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnItemSelectionListener");
		}
	}

	@Override
	public void onCarChanged(Car newCar) {
		mCar = newCar;

		setCurrentPosition(ListView.INVALID_POSITION);
		mListAdapter.update();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		mActivateOnClick = args.getBoolean(EXTRA_ACTIVATE_ON_CLICK,
				EXTRA_ACTIVATE_ON_CLICK_DEFAULT);

		long carId = args.getLong(EXTRA_CAR_ID);
		if (savedInstanceState != null) {
			carId = savedInstanceState.getLong(STATE_CURRENT_CAR, carId);
		}

		if (carId != 0) {
			mCar = Car.load(Car.class, carId);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_data_list, container, false);
	}

	@Override
	public void onDialogNegativeClick(int requestCode) {
	}

	@Override
	public void onDialogPositiveClick(int requestCode) {
		if (requestCode == REQUEST_DELETE) {
			SparseBooleanArray selected = getListView()
					.getCheckedItemPositions();
			for (int i = 0; i < mListAdapter.getCount(); i++) {
				if (selected.get(i)) {
					mListAdapter.getItem(i).delete();
				}
			}

			mActionMode.finish();
			mListAdapter.update();
		}
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (mActionMode != null) {
			return;
		}

		setCurrentPosition(position);
		mDataListCallback.onItemSelected(getExtraEdit(), getListAdapter()
				.getItemId(position));
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mActionMode != null) {
			mActionMode.finish();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(STATE_CURRENT_ITEM, mCar.id);
		outState.putInt(STATE_CURRENT_ITEM, mCurrentItem);
	}

	public void setCurrentPosition(int position) {
		if (mActionMode != null) {
			mActionMode.finish();
		}

		getListView().setItemChecked(mCurrentItem, false);
		if (position != ListView.INVALID_POSITION && mActivateOnClick) {
			dontStartActionMode = true;
			getListView().setItemChecked(position, true);
		}

		mCurrentItem = position;
	}

	@Override
	public void unselectItem() {
		setCurrentPosition(ListView.INVALID_POSITION);
	}

	@Override
	public void updateData() {
		setCurrentPosition(ListView.INVALID_POSITION);
		mListAdapter.update();
	}

	protected abstract int getAlertDeleteManyMessage();

	protected abstract int getExtraEdit();

	protected abstract SparseArray<String> getItemData(List<T> items,
			int position);

	protected abstract List<T> getItems();
}