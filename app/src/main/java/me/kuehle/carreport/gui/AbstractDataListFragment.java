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

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bignerdranch.android.multiselector.ModalMultiSelectorCallback;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.bignerdranch.android.multiselector.SwappingHolder;

import java.util.List;

import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.SupportMessageDialogFragment;
import me.kuehle.carreport.gui.dialog.SupportMessageDialogFragment.SupportMessageDialogFragmentListener;
import me.kuehle.carreport.gui.util.DividerItemDecoration;

public abstract class AbstractDataListFragment extends
        Fragment implements SupportMessageDialogFragmentListener,
        DataListListener, LoaderManager.LoaderCallbacks<Cursor> {
    public static final String EXTRA_ACTIVATE_ON_CLICK = "activate_on_click";
    public static final boolean EXTRA_ACTIVATE_ON_CLICK_DEFAULT = false;
    public static final String EXTRA_CAR_ID = "car_id";

    private static final String STATE_SELECTED_ITEMS = "selected_items";
    private static final int REQUEST_DELETE = 0;

    protected long mCarId;

    private DataAdapter mListAdapter;
    private MultiSelector mMultiSelector = new MultiSelector();
    private DataModalMultiSelector mModalMultiSelector = new DataModalMultiSelector(mMultiSelector);
    private DataListCallback mDataListCallback;
    private boolean mActivateOnClick = EXTRA_ACTIVATE_ON_CLICK_DEFAULT;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            if (getParentFragment() != null) {
                mDataListCallback = (DataListCallback) getParentFragment();
            } else {
                mDataListCallback = (DataListCallback) context;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " or parent fragment must implement OnItemSelectionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mActivateOnClick = args.getBoolean(EXTRA_ACTIVATE_ON_CLICK,
                EXTRA_ACTIVATE_ON_CLICK_DEFAULT);
        mCarId = args.getLong(EXTRA_CAR_ID);

        mMultiSelector.setSelectable(mActivateOnClick);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_data_list, container, false);

        mListAdapter = new DataAdapter();

        RecyclerView list = (RecyclerView) v.findViewById(android.R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.addItemDecoration(new DividerItemDecoration(getContext()));
        list.setAdapter(mListAdapter);
        mDataListCallback.onViewCreated(list);

        getLoaderManager().initLoader(0, null, this);

        if (savedInstanceState != null) {
            Bundle savedSelections = savedInstanceState.getBundle(STATE_SELECTED_ITEMS);
            if (savedSelections != null) {
                mMultiSelector.restoreSelectionStates(savedSelections);
            }
        }

        return v;
    }

    @Override
    public void onDialogNegativeClick(int requestCode) {
    }

    @Override
    public void onDialogPositiveClick(int requestCode) {
        if (requestCode == REQUEST_DELETE) {
            List<Integer> positions = mMultiSelector.getSelectedPositions();
            for (int position : positions) {
                deleteItem(mListAdapter.getItemId(position));
            }

            mModalMultiSelector.finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mModalMultiSelector.finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBundle(STATE_SELECTED_ITEMS, mMultiSelector.saveSelectionStates());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mListAdapter.setCursor(cursor);
        if (mMultiSelector.getSelectedPositions().size() == 1) {
            int pos = mMultiSelector.getSelectedPositions().get(0);
            mDataListCallback.onItemSelected(getExtraEdit(), mListAdapter.getItemId(pos));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        unselectItem(true);
        mListAdapter.setCursor(null);
    }

    @Override
    public void unselectItem(boolean finishActionMode) {
        if (finishActionMode) {
            mModalMultiSelector.finish();
        }

        mMultiSelector.clearSelections();
    }

    protected abstract int getAlertDeleteManyMessage();

    protected abstract int getExtraEdit();

    protected abstract SparseArray<String> getItemData(Cursor cursor);

    protected abstract boolean isMissingData(Cursor cursor);

    protected abstract void deleteItem(long id);

    private abstract class AbstractDataViewHolder extends SwappingHolder {
        public AbstractDataViewHolder(View itemView) {
            super(itemView, mMultiSelector);
        }

        public abstract void bind(SparseArray<String> item);
    }

    private class DataViewHolder extends AbstractDataViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final int[] mFields = {R.id.title, R.id.subtitle, R.id.date,
                R.id.data1, R.id.data1_calculated, R.id.data2,
                R.id.data2_calculated, R.id.data3, R.id.data3_calculated};
        private SparseArray<TextView> mTextViews;
        private TextView mDataInvalid;

        public DataViewHolder(View itemView) {
            super(itemView);

            mTextViews = new SparseArray<>(mFields.length);
            for (int field : mFields) {
                TextView textView = (TextView) itemView.findViewById(field);
                mTextViews.append(field, textView);
            }

            mDataInvalid = (TextView) itemView.findViewById(R.id.data_invalid);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void bind(SparseArray<String> item) {
            for (int field : mFields) {
                TextView textView = mTextViews.get(field);
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

            if ("true".equals(item.get(R.id.data_invalid))) {
                mDataInvalid.setVisibility(View.VISIBLE);
            } else {
                mDataInvalid.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View v) {
            long id = mListAdapter.getItemId(getAdapterPosition());
            if (mMultiSelector.tapSelection(this)) {
                // Selection is on, so tapSelection() toggled item selection.
                if (mModalMultiSelector.isActive()) {
                    mModalMultiSelector.notifyMultiSelectionChanged();
                } else {
                    mMultiSelector.clearSelections();
                    mMultiSelector.setSelected(this, true);
                    mDataListCallback.onItemSelected(getExtraEdit(), id);
                }
            } else {
                // Selection is off; handle normal item click here.
                mDataListCallback.onItemSelected(getExtraEdit(), id);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.startSupportActionMode(mModalMultiSelector);
            mMultiSelector.setSelected(this, true);
            mModalMultiSelector.notifyMultiSelectionChanged();
            return true;
        }
    }

    private class DataMissingViewHolder extends AbstractDataViewHolder {
        private TextView mTitle;

        public DataMissingViewHolder(View itemView) {
            super(itemView);

            mTitle = (TextView) itemView.findViewById(R.id.title);
        }

        @Override
        public void bind(SparseArray<String> item) {
            mTitle.setText(item.get(R.id.title));
        }
    }

    private class DataAdapter extends RecyclerView.Adapter<AbstractDataViewHolder> {
        private Cursor mCursor;

        public DataAdapter() {
            setHasStableIds(true);
        }

        @Override
        public AbstractDataViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 0) {
                View view = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.list_item_data_missing, parent, false);
                return new DataMissingViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.list_item_data, parent, false);
                return new DataViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(AbstractDataViewHolder holder, int position) {
            if (mCursor != null) {
                mCursor.moveToPosition(position);
                SparseArray<String> item = getItemData(mCursor);
                holder.bind(item);
            }
        }

        @Override
        public int getItemCount() {
            if (mCursor != null) {
                return mCursor.getCount();
            } else {
                return 0;
            }
        }

        @Override
        public long getItemId(int position) {
            if (mCursor != null) {
                mCursor.moveToPosition(position);
                return mCursor.getLong(mCursor.getColumnIndex(BaseColumns._ID));
            } else {
                return 0;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (mCursor != null) {
                mCursor.moveToPosition(position);
                return isMissingData(mCursor) ? 0 : 1;
            } else {
                return 0;
            }
        }

        public void setCursor(Cursor cursor) {
            if (cursor != mCursor) {
                if (mCursor != null) {
                    mCursor.close();
                }

                mCursor = cursor;
                notifyDataSetChanged();
            }
        }
    }

    private class DataModalMultiSelector extends ModalMultiSelectorCallback {
        private ActionMode mActionMode;

        public DataModalMultiSelector(MultiSelector multiSelector) {
            super(multiSelector);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_delete:
                    String message = String.format(getString(getAlertDeleteManyMessage()),
                            mMultiSelector.getSelectedPositions().size());
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
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            super.onCreateActionMode(actionMode, menu);

            mActionMode = actionMode;
            mDataListCallback.onItemUnselected();

            actionMode.getMenuInflater().inflate(R.menu.view_data_cab, menu);

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            mActionMode = null;
            mMultiSelector.clearSelections();
            mMultiSelector.setSelectable(mActivateOnClick);
        }

        public void notifyMultiSelectionChanged() {
            int count = mMultiSelector.getSelectedPositions().size();
            mActionMode.setTitle(String.format(getString(R.string.cab_title_selected), count));
        }

        public void finish() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }

        public boolean isActive() {
            return mActionMode != null;
        }
    }
}