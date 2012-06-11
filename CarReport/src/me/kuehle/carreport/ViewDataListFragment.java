package me.kuehle.carreport;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import me.kuehle.carreport.db.AbstractItem;
import me.kuehle.carreport.db.Car;
import me.kuehle.carreport.db.OtherCost;
import me.kuehle.carreport.db.Refueling;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

public class ViewDataListFragment extends Fragment {
	public static final String EXTRA_CAR = "car";

	private TabHost tabHost;
	private AbstractListFragment[] fragments;
	private boolean dualPane;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.view_data_list, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		fragments = new AbstractListFragment[2];
		fragments[0] = (AbstractListFragment) getFragmentManager()
				.findFragmentById(R.id.tabRefuelings);
		fragments[1] = (AbstractListFragment) getFragmentManager()
				.findFragmentById(R.id.tabOtherCosts);

		tabHost = (TabHost) getView();
		tabHost.setOnTabChangedListener(onTabChangeListener);
		tabHost.setup();
		addTab(RefuelingsFragment.TAG, R.string.tab_indicator_refuelings,
				R.id.tabRefuelings);
		addTab(OtherCostsFragment.TAG, R.string.tab_indicator_other,
				R.id.tabOtherCosts);

		View editFrame = getActivity().findViewById(R.id.edit);
		dualPane = editFrame != null
				&& editFrame.getVisibility() == View.VISIBLE;
		for (AbstractListFragment fragment : fragments) {
			fragment.setDualPaneMode(dualPane);
		}
	}

	public void setCar(Car car) {
		for (AbstractListFragment fragment : fragments) {
			fragment.setCar(car);
		}
	}

	public void updateLists() {
		for (AbstractListFragment fragment : fragments) {
			fragment.updateList();
		}
	}

	private void addTab(String tag, int indicatorId, int contentId) {
		TabSpec tabSpec = tabHost.newTabSpec(tag);
		tabSpec.setIndicator(getString(indicatorId));
		tabSpec.setContent(contentId);
		tabHost.addTab(tabSpec);
	}

	private OnTabChangeListener onTabChangeListener = new OnTabChangeListener() {
		@Override
		public void onTabChanged(String tabId) {
			for (AbstractListFragment fragment : fragments) {
				fragment.unselectAll();
			}
		}
	};

	public abstract static class AbstractListFragment extends ListFragment {
		protected Car car = null;
		protected AbstractItem[] items;
		protected boolean dualPane;
		protected boolean dontStartActionMode = false;
		protected ActionMode actionMode = null;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			getListView().setOnItemClickListener(onItemClickListener);
			getListView().setMultiChoiceModeListener(multiChoiceModeListener);
			getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		}

		protected abstract AbstractEditFragment createEditFragment(int id);

		protected abstract int getAlertDeleteManyMessage();

		protected abstract int getEditFragmentActivityValue();

		protected abstract void fillList();

		public void clearRightPane() {
			if (dualPane) {
				Fragment editFragment = getFragmentManager().findFragmentById(
						R.id.edit);
				if (editFragment != null) {
					getFragmentManager()
							.beginTransaction()
							.remove(editFragment)
							.setTransition(
									FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
							.commit();
				}
			}
		}

		public void setCar(Car car) {
			this.car = car;
			updateList();
		}

		public void setDualPaneMode(boolean dualPane) {
			this.dualPane = dualPane;
		}

		public void unselectAll() {
			clearRightPane();
			getListView().clearChoices();
			if (actionMode != null) {
				actionMode.finish();
			}
		}

		public void updateList() {
			clearRightPane();
			if (car != null) {
				fillList();
			} else {
				getListView().setAdapter(null);
			}
		}

		private void openItemInRightPane(int position) {
			if (dualPane) {
				dontStartActionMode = true;
				getListView().clearChoices();
				getListView().setItemChecked(position, true);

				Fragment editFragment = createEditFragment(items[position]
						.getId());
				getFragmentManager()
						.beginTransaction()
						.replace(R.id.edit, editFragment)
						.setTransition(
								FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
						.commit();
			}
		}

		private OnItemClickListener onItemClickListener = new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (dualPane) {
					if (getListView().getCheckedItemPosition() != position) {
						openItemInRightPane(position);
					}
				} else {
					Intent intent = new Intent(getActivity(),
							EditFragmentActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
					intent.putExtra(
							EditFragmentActivity.EXTRA_FINISH_ON_BIG_SCREEN,
							true);
					intent.putExtra(EditFragmentActivity.EXTRA_EDIT,
							getEditFragmentActivityValue());
					intent.putExtra(AbstractEditFragment.EXTRA_ID,
							items[position].getId());
					startActivityForResult(intent,
							ViewDataActivity.EDIT_REQUEST_CODE);
				}
			}
		};

		private MultiChoiceModeListener multiChoiceModeListener = new MultiChoiceModeListener() {
			private ActionMode mode;

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				getListView().setOnItemClickListener(onItemClickListener);
				actionMode = null;
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				if (dontStartActionMode) {
					dontStartActionMode = false;
					return false;
				}

				clearRightPane();
				getListView().setOnItemClickListener(null);

				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.cab_delete, menu);

				actionMode = mode;
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case R.id.menu_delete:
					this.mode = mode;

					String message = String.format(
							getString(getAlertDeleteManyMessage()),
							getListView().getCheckedItemCount());
					new AlertDialog.Builder(getActivity())
							.setTitle(R.string.alert_delete_title)
							.setMessage(message)
							.setPositiveButton(android.R.string.yes,
									deleteOnClickListener)
							.setNegativeButton(android.R.string.no, null)
							.show();
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
					for (int i = 0; i < items.length; i++) {
						if (selected.get(i)) {
							items[i].delete();
						}
					}
					mode.finish();
					fillList();
				}
			};
		};
	}

	public static class RefuelingsFragment extends AbstractListFragment {
		public static final String TAG = "refuelings";

		@Override
		protected AbstractEditFragment createEditFragment(int id) {
			return EditRefuelingFragment.newInstance(id);
		}

		@Override
		protected int getAlertDeleteManyMessage() {
			return R.string.alert_delete_refuelings_message;
		}

		@Override
		protected int getEditFragmentActivityValue() {
			return EditFragmentActivity.EXTRA_EDIT_REFUELING;
		}

		@Override
		protected void fillList() {
			ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
			SimpleAdapter adapter = new SimpleAdapter(getActivity(), data,
					R.layout.two_line_list_item_5, new String[] { "text_tl",
							"text_tr", "text_bl", "text_bm", "text_br" },
					new int[] { R.id.text_tl, R.id.text_tr, R.id.text_bl,
							R.id.text_bm, R.id.text_br });

			Preferences prefs = new Preferences(getActivity());
			DateFormat dateFmt = DateFormat.getDateInstance();
			items = Refueling.getAllForCar(car, false);
			for (AbstractItem item : items) {
				Refueling refueling = (Refueling) item;
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("text_tl", dateFmt.format(refueling.getDate()));
				map.put("text_tr",
						refueling.isPartial() ? getString(R.string.label_partial)
								: "");
				map.put("text_bl",
						String.format("%d %s", refueling.getTachometer(),
								prefs.getUnitDistance()));
				map.put("text_bm",
						String.format("%.2f %s", refueling.getVolume(),
								prefs.getUnitVolume()));
				map.put("text_br",
						String.format("%.2f %s", refueling.getPrice(),
								prefs.getUnitCurrency()));
				data.add(map);
			}

			setListAdapter(adapter);
		}
	}

	public static class OtherCostsFragment extends AbstractListFragment {
		public static final String TAG = "othercosts";

		@Override
		protected AbstractEditFragment createEditFragment(int id) {
			return EditOtherCostFragment.newInstance(id);
		}

		@Override
		protected int getAlertDeleteManyMessage() {
			return R.string.alert_delete_others_message;
		}

		@Override
		protected int getEditFragmentActivityValue() {
			return EditFragmentActivity.EXTRA_EDIT_OTHER;
		}

		@Override
		protected void fillList() {
			ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
			SimpleAdapter adapter = new SimpleAdapter(getActivity(), data,
					R.layout.two_line_list_item_5, new String[] { "text_tl",
							"text_tr", "text_bl", "text_br" }, new int[] {
							R.id.text_tl, R.id.text_tr, R.id.text_bl,
							R.id.text_br });

			Preferences prefs = new Preferences(getActivity());
			DateFormat dateFmt = DateFormat.getDateInstance();
			items = OtherCost.getAllForCar(car, false);
			for (AbstractItem item : items) {
				OtherCost other = (OtherCost) item;
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("text_tl", dateFmt.format(other.getDate()));
				map.put("text_tr", other.getTitle());
				map.put("text_bl",
						String.format("%d %s", other.getTachometer(),
								prefs.getUnitDistance()));
				map.put("text_br",
						String.format("%.2f %s", other.getPrice(),
								prefs.getUnitCurrency()));
				data.add(map);
			}

			setListAdapter(adapter);
		}
	}
}
