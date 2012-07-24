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

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

public class ViewDataActivity extends Activity implements
		AbstractEditFragment.OnItemActionListener,
		ViewDataListFragment.ViewDataListListener {
	private static final int ADD_REFUELING_REQUEST_CODE = 0;
	private static final int ADD_OTHER_REQUEST_CODE = 1;
	private static final String TAG_LIST = "list";
	private static final String TAG_EDIT = "edit";

	private ViewDataListFragment mList;
	private AbstractEditFragment mEdit;
	private boolean mDualPane;
	private boolean mSkipNextBackStackEvent = false;
	private int mLowestBackStackState = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_data);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		View editFrame = findViewById(R.id.edit);
		mDualPane = !editFrame.getParent().equals(findViewById(R.id.list));

		FragmentManager fm = getFragmentManager();
		fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
		mList = (ViewDataListFragment) fm.findFragmentByTag(TAG_LIST);
		mEdit = (AbstractEditFragment) fm.findFragmentByTag(TAG_EDIT);
		if (mList == null) {
			mList = new ViewDataListFragment();
			fm.beginTransaction().add(R.id.list, mList, TAG_LIST).commit();
		}
		if (mEdit != null) {
			openItemDoFragmentTransactions();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_data, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_refueling:
			Intent intent = new Intent(this, EditFragmentActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			intent.putExtra(EditFragmentActivity.EXTRA_EDIT,
					EditFragmentActivity.EXTRA_EDIT_REFUELING);
			intent.putExtra(EditRefuelingFragment.EXTRA_CAR_ID, mList
					.getCurrentCar().getId());
			startActivityForResult(intent, ADD_REFUELING_REQUEST_CODE);
			return true;
		case R.id.menu_add_other:
			Intent intent1 = new Intent(this, EditFragmentActivity.class);
			intent1.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			intent1.putExtra(EditFragmentActivity.EXTRA_EDIT,
					EditFragmentActivity.EXTRA_EDIT_OTHER);
			intent1.putExtra(EditOtherCostFragment.EXTRA_CAR_ID, mList
					.getCurrentCar().getId());
			startActivityForResult(intent1, ADD_OTHER_REQUEST_CODE);
			return true;
		case android.R.id.home:
			if (mEdit != null) {
				itemCanceled();
			} else {
				finish();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			mList.updateLists();
		}
	}

	@Override
	public void closeCurrentItem() {
		if (mEdit != null) {
			getFragmentManager().popBackStack(mLowestBackStackState,
					FragmentManager.POP_BACK_STACK_INCLUSIVE);
			setNoEntryMessageVisible(true);
			mEdit = null;
		}
	}

	@Override
	public boolean isDualPane() {
		return mDualPane;
	}

	@Override
	public void itemCanceled() {
		closeCurrentItem();
		mList.unselectAll();
	}

	@Override
	public void itemDeleted() {
		closeCurrentItem();
		mList.updateLists();
	}

	@Override
	public void itemSaved() {
		closeCurrentItem();
		mList.updateLists();
	}

	@Override
	public void openItem(ViewDataListFragment.AbstractEditHelper helper) {
		setNoEntryMessageVisible(false);

		mEdit = helper.createEditFragment();
		openItemDoFragmentTransactions();
	}

	private void openItemDoFragmentTransactions() {
		mSkipNextBackStackEvent = true;
		getFragmentManager().popBackStackImmediate(mLowestBackStackState,
				FragmentManager.POP_BACK_STACK_INCLUSIVE);

		if (!mDualPane) {
			mSkipNextBackStackEvent = true;
			mLowestBackStackState = getFragmentManager().beginTransaction()
					.hide(mList).addToBackStack(null).commit();
			getFragmentManager().executePendingTransactions();
		}

		mSkipNextBackStackEvent = true;
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.replace(R.id.edit, mEdit, TAG_EDIT);
		ft.addToBackStack(null);
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.commit();
	}

	private void setNoEntryMessageVisible(boolean visible) {
		View msg = findViewById(R.id.txtNoEntrySelected);
		if (msg != null) {
			msg.setVisibility(visible ? View.VISIBLE : View.GONE);
		}
	}

	private OnBackStackChangedListener mOnBackStackChangedListener = new OnBackStackChangedListener() {
		@Override
		public void onBackStackChanged() {
			if (mSkipNextBackStackEvent) {
				mSkipNextBackStackEvent = false;
			} else {
				itemCanceled();
			}
		}
	};
}
