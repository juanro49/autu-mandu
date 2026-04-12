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

package org.juanro.autumandu.gui.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

import java.util.ArrayList;
import java.util.List;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.dialog.MessageDialogFragment;
import org.juanro.autumandu.gui.util.DividerItemDecoration;

public abstract class AbstractDataListFragment<T> extends
        Fragment implements DataListListener {
    public static final String EXTRA_ACTIVATE_ON_CLICK = "activate_on_click";
    public static final boolean EXTRA_ACTIVATE_ON_CLICK_DEFAULT = false;
    public static final String EXTRA_CAR_ID = "car_id";

    private static final String STATE_SELECTED_ITEMS = "selected_items";
    private static final int REQUEST_DELETE = 0;

    protected long carId;

    private DataAdapter listAdapter;
    private final MultiSelector multiSelector = new MultiSelector();
    private final DataModalMultiSelector modalMultiSelector = new DataModalMultiSelector(multiSelector);
    private DataListCallback dataListCallback;
    private boolean activateOnClick = EXTRA_ACTIVATE_ON_CLICK_DEFAULT;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            if (getParentFragment() != null) {
                dataListCallback = (DataListCallback) getParentFragment();
            } else {
                dataListCallback = (DataListCallback) context;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass().getSimpleName() +
                    " or parent fragment must implement OnItemSelectionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        var args = getArguments();
        if (args != null) {
            activateOnClick = args.getBoolean(EXTRA_ACTIVATE_ON_CLICK,
                    EXTRA_ACTIVATE_ON_CLICK_DEFAULT);
            carId = args.getLong(EXTRA_CAR_ID);
        }

        multiSelector.setSelectable(activateOnClick);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        var v = inflater.inflate(R.layout.fragment_data_list, container, false);

        listAdapter = new DataAdapter();

        var list = (RecyclerView) v.findViewById(android.R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        var context = getContext();
        if (context != null) {
            list.addItemDecoration(new DividerItemDecoration(context));
        }
        list.setAdapter(listAdapter);
        dataListCallback.onViewCreated(list);

        getLiveData().observe(getViewLifecycleOwner(), data -> {
            listAdapter.setData(data);
            if (multiSelector.getSelectedPositions().size() == 1) {
                var pos = multiSelector.getSelectedPositions().get(0);
                dataListCallback.onItemSelected(getExtraEdit(), listAdapter.getItemId(pos));
            }
        });

        if (savedInstanceState != null) {
            var savedSelections = savedInstanceState.getBundle(STATE_SELECTED_ITEMS);
            if (savedSelections != null) {
                multiSelector.restoreSelectionStates(savedSelections);
            }
        }

        getChildFragmentManager().setFragmentResultListener(MessageDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            var action = result.getInt(MessageDialogFragment.RESULT_ACTION);
            var requestCode = result.getInt(MessageDialogFragment.RESULT_REQUEST_CODE);
            if (action == MessageDialogFragment.ACTION_POSITIVE && requestCode == REQUEST_DELETE) {
                var positions = new ArrayList<>(multiSelector.getSelectedPositions());
                var idsToDelete = new ArrayList<Long>();
                for (var position : positions) {
                    idsToDelete.add(listAdapter.getItemId(position));
                }

                for (var id : idsToDelete) {
                    deleteItem(id);
                }

                modalMultiSelector.finish();
            }
        });

        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        modalMultiSelector.finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBundle(STATE_SELECTED_ITEMS, multiSelector.saveSelectionStates());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void unselectItem(boolean finishActionMode) {
        if (finishActionMode) {
            modalMultiSelector.finish();
        }

        multiSelector.clearSelections();
    }

    protected abstract int getAlertDeleteManyMessage();

    protected abstract int getExtraEdit();

    protected abstract SparseArray<String> getItemData(T item);

    protected abstract boolean isMissingData(T item);

    protected abstract void deleteItem(long id);

    protected abstract long getItemId(T item);

    protected abstract LiveData<List<T>> getLiveData();

    private abstract class AbstractDataViewHolder extends SwappingHolder {
        public AbstractDataViewHolder(View itemView) {
            super(itemView, multiSelector);
        }

        public abstract void bind(SparseArray<String> item);
    }

    private class DataViewHolder extends AbstractDataViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final int[] fields = {R.id.title, R.id.subtitle, R.id.date,
                R.id.data1, R.id.data1_calculated, R.id.data2,
                R.id.data2_calculated, R.id.data3, R.id.data3_calculated};
        private final SparseArray<TextView> textViews;
        private final TextView dataInvalid;

        public DataViewHolder(View itemView) {
            super(itemView);

            textViews = new SparseArray<>(fields.length);
            for (var field : fields) {
                var textView = (TextView) itemView.findViewById(field);
                textViews.append(field, textView);
            }

            dataInvalid = itemView.findViewById(R.id.data_invalid);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void bind(SparseArray<String> item) {
            for (var field : fields) {
                var textView = textViews.get(field);
                var value = item.get(field);
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
                dataInvalid.setVisibility(View.VISIBLE);
            } else {
                dataInvalid.setVisibility(View.GONE);
            }
        }

        @Override
        public void onClick(View v) {
            var id = listAdapter.getItemId(getBindingAdapterPosition());
            if (multiSelector.tapSelection(this)) {
                // Selection is on, so tapSelection() toggled item selection.
                if (modalMultiSelector.isActive()) {
                    modalMultiSelector.notifyMultiSelectionChanged();
                } else {
                    multiSelector.clearSelections();
                    multiSelector.setSelected(this, true);
                    dataListCallback.onItemSelected(getExtraEdit(), id);
                }
            } else {
                // Selection is off; handle normal item click here.
                dataListCallback.onItemSelected(getExtraEdit(), id);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            var activity = (AppCompatActivity) getActivity();
            if (activity != null) {
                activity.startSupportActionMode(modalMultiSelector);
            }
            multiSelector.setSelected(this, true);
            modalMultiSelector.notifyMultiSelectionChanged();
            return true;
        }
    }

    private class DataMissingViewHolder extends AbstractDataViewHolder {
        private final TextView title;

        public DataMissingViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
        }

        @Override
        public void bind(SparseArray<String> item) {
            title.setText(item.get(R.id.title));
        }
    }

    private class DataAdapter extends RecyclerView.Adapter<AbstractDataViewHolder> {
        private List<T> data = new ArrayList<>();

        public DataAdapter() {
            setHasStableIds(true);
        }

        @NonNull
        @Override
        public AbstractDataViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                var view = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.list_item_data_missing, parent, false);
                return new DataMissingViewHolder(view);
            } else {
                var view = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.list_item_data, parent, false);
                return new DataViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull AbstractDataViewHolder holder, int position) {
            if (data != null && position < data.size()) {
                var item = data.get(position);
                var itemData = getItemData(item);
                holder.bind(itemData);
            }
        }

        @Override
        public int getItemCount() {
            return data == null ? 0 : data.size();
        }

        @Override
        public long getItemId(int position) {
            if (data != null && position < data.size()) {
                return AbstractDataListFragment.this.getItemId(data.get(position));
            } else {
                return 0;
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (data != null && position < data.size()) {
                return isMissingData(data.get(position)) ? 0 : 1;
            } else {
                return 0;
            }
        }

        public void setData(List<T> data) {
            this.data = data;
            notifyDataSetChanged();
        }
    }

    private class DataModalMultiSelector extends ModalMultiSelectorCallback {
        private ActionMode actionMode;

        public DataModalMultiSelector(MultiSelector multiSelector) {
            super(multiSelector);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            var itemId = item.getItemId();
            if (itemId == R.id.menu_delete) {
                var message = String.format(getString(getAlertDeleteManyMessage()),
                        multiSelector.getSelectedPositions().size());
                MessageDialogFragment.newInstance(
                        REQUEST_DELETE, R.string.alert_delete_title, message,
                        android.R.string.yes, android.R.string.no).show(
                        getChildFragmentManager(), null);
                return true;
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            super.onCreateActionMode(actionMode, menu);

            this.actionMode = actionMode;
            dataListCallback.onItemUnselected();

            actionMode.getMenuInflater().inflate(R.menu.view_data_cab, menu);

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            super.onDestroyActionMode(actionMode);
            this.actionMode = null;
            multiSelector.clearSelections();
            multiSelector.setSelectable(activateOnClick);
        }

        public void notifyMultiSelectionChanged() {
            var count = multiSelector.getSelectedPositions().size();
            actionMode.setTitle(String.format(getString(R.string.cab_title_selected), count));
        }

        public void finish() {
            if (actionMode != null) {
                actionMode.finish();
            }
        }

        public boolean isActive() {
            return actionMode != null;
        }
    }
}
