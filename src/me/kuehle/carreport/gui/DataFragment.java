package me.kuehle.carreport.gui;

import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.gui.MainActivity.DataChangeListener;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.activeandroid.Model;

public class DataFragment extends Fragment implements
		AbstractDataListFragment.DataListCallback,
		AbstractDataDetailFragment.OnItemActionListener, DataChangeListener {
	public static interface DataListListener {
		public void onCarChanged(Car newCar);

		public void unselectItem();

		public void updateData();
	}

	private class DataListBackStackListener implements
			OnBackStackChangedListener {
		@Override
		public void onBackStackChanged() {
			if (getChildFragmentManager().getBackStackEntryCount() == 0) {
				setNoEntrySelectedTextVisible(true);
				for (Fragment childFragment : getChildFragmentManager()
						.getFragments()) {
					if (childFragment instanceof DataListListener) {
						((DataListListener) childFragment).unselectItem();
					}
				}
			}
		}
	}

	private class DataListNavigationListener implements OnNavigationListener {
		@Override
		public boolean onNavigationItemSelected(int itemPosition, long itemId) {
			mCurrentCar = mCars.get(itemPosition);
			for (Fragment fragment : getChildFragmentManager().getFragments()) {
				if (fragment instanceof DataListListener) {
					((DataListListener) fragment).onCarChanged(mCurrentCar);
				}
			}

			return true;
		}
	}

	private class DataListOnPageChangeListener extends
			SimpleOnPageChangeListener {
		@Override
		public void onPageSelected(int position) {
			onItemUnselected();

			if (mTxtNoEntrySelected != null) {
				int id = position == 0 ? R.drawable.ic_data_detail_refueling
						: R.drawable.ic_data_detail_other;
				mTxtNoEntrySelected.setCompoundDrawablesWithIntrinsicBounds(0,
						id, 0, 0);
			}
		}
	}

	private class DataListPagerAdapter extends FragmentPagerAdapter {
		public DataListPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public Fragment getItem(int position) {
			AbstractDataListFragment<? extends Model> fragment;
			if (position == 0) {
				fragment = new DataListRefuelingFragment();
			} else {
				fragment = new DataListOtherFragment();
			}

			Bundle args = new Bundle();
			args.putBoolean(AbstractDataListFragment.EXTRA_ACTIVATE_ON_CLICK,
					mTwoPane);
			args.putLong(AbstractDataListFragment.EXTRA_CAR_ID,
					mCurrentCar.getId());
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return getResources().getStringArray(R.array.data_list_page_titles)[position];
		}
	}

	private static final String STATE_CURRENT_CAR = "current_car";
	private DataListPagerAdapter mAdapter;

	private ViewPager mPager;
	private boolean mTwoPane;
	private TextView mTxtNoEntrySelected;

	private List<Car> mCars;
	private Car mCurrentCar = null;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		ActionBar actionBar = getActivity().getActionBar();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_spinner_dropdown_item);
		for (Car car : mCars) {
			adapter.add(car.name);
		}

		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter,
				new DataListNavigationListener());
		for (int i = 0; i < mCars.size(); i++) {
			if (mCars.get(i).getId() == mCurrentCar.getId()) {
				actionBar.setSelectedNavigationItem(i);
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCars = Car.getAll();
		long carId;
		Preferences prefs = new Preferences(getActivity());
		if (savedInstanceState != null) {
			carId = savedInstanceState.getLong(STATE_CURRENT_CAR,
					prefs.getDefaultCar());
		} else {
			carId = prefs.getDefaultCar();
		}

		mCurrentCar = Car.load(Car.class, carId);

		getChildFragmentManager().addOnBackStackChangedListener(
				new DataListBackStackListener());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_data, container, false);

		mAdapter = new DataListPagerAdapter(getChildFragmentManager());
		mPager = (ViewPager) v.findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);
		mPager.setOnPageChangeListener(new DataListOnPageChangeListener());
		PagerTabStrip tabs = (PagerTabStrip) v
				.findViewById(R.id.pager_tab_strip);
		tabs.setTabIndicatorColorResource(android.R.color.holo_blue_dark);

		mTxtNoEntrySelected = (TextView) v
				.findViewById(R.id.txt_no_entry_selected);
		if (getChildFragmentManager().findFragmentById(R.id.detail) != null) {
			setNoEntrySelectedTextVisible(false);
		}

		mTwoPane = v.findViewById(R.id.detail) != null;

		return v;
	}

	@Override
	public void onDataChanged() {
		for (Fragment fragment : getChildFragmentManager().getFragments()) {
			if (fragment instanceof DataListListener) {
				((DataListListener) fragment).updateData();
			}
		}
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setListNavigationCallbacks(null, null);
	}

	@Override
	public void onItemCanceled() {
		onItemUnselected();
	}

	@Override
	public void onItemDeleted() {
		onItemUnselected();
		onDataChanged();
	}

	@Override
	public void onItemSaved() {
		onItemUnselected();
		onDataChanged();
	}

	@Override
	public void onItemSelected(int edit, long id) {
		if (mTwoPane) {
			setNoEntrySelectedTextVisible(false);

			AbstractDataDetailFragment fragment;
			if (edit == DataDetailActivity.EXTRA_EDIT_REFUELING) {
				fragment = DataDetailRefuelingFragment.newInstance(id, true);
			} else {
				fragment = DataDetailOtherFragment.newInstance(id, true);
			}

			FragmentManager fm = getChildFragmentManager();
			fm.popBackStack("detail", FragmentManager.POP_BACK_STACK_INCLUSIVE);

			FragmentTransaction ft = fm.beginTransaction().replace(R.id.detail,
					fragment);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
			ft.addToBackStack("detail");
			ft.commit();
		} else {
			Intent intent = new Intent(getActivity(), DataDetailActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			intent.putExtra(DataDetailActivity.EXTRA_EDIT, edit);
			intent.putExtra(AbstractDataDetailFragment.EXTRA_ID, id);
			startActivityForResult(intent, 0);
		}
	}

	@Override
	public void onItemUnselected() {
		getChildFragmentManager().popBackStack("detail",
				FragmentManager.POP_BACK_STACK_INCLUSIVE);

		/*
		 * setNoEntrySelectedTextVisible(true); for (Fragment childFragment :
		 * getChildFragmentManager().getFragments()) { if (childFragment
		 * instanceof DataListListener) { ((DataListListener)
		 * childFragment).unselectItem(); } }
		 */
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong(STATE_CURRENT_CAR, mCurrentCar.getId());
	}

	private void setNoEntrySelectedTextVisible(boolean visible) {
		if (mTxtNoEntrySelected != null) {
			mTxtNoEntrySelected.setVisibility(visible ? View.VISIBLE
					: View.GONE);
		}
	}
}
