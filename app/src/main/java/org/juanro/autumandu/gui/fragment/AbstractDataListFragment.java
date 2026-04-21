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
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    private static final String STATE_SELECTION = "selection_state";
    private static final int REQUEST_DELETE = 0;

    protected long carId;

    private DataAdapter listAdapter;
    private SelectionTracker<Long> selectionTracker;
    private ActionMode actionMode;
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

        selectionTracker = new SelectionTracker.Builder<>(
                "data-selection-" + carId,
                list,
                new DataKeyProvider(),
                new DataItemDetailsLookup(list),
                StorageStrategy.createLongStorage())
                .build();

        selectionTracker.addObserver(new SelectionTracker.SelectionObserver<>() {
            @Override
            public void onSelectionChanged() {
                if (selectionTracker.hasSelection() && actionMode == null) {
                    var activity = (AppCompatActivity) getActivity();
                    if (activity != null) {
                        actionMode = activity.startSupportActionMode(new ActionModeCallback());
                    }
                } else if (!selectionTracker.hasSelection() && actionMode != null) {
                    actionMode.finish();
                }

                if (actionMode != null) {
                    var count = selectionTracker.getSelection().size();
                    actionMode.setTitle(String.format(getString(R.string.cab_title_selected), count));
                }

                if (activateOnClick && selectionTracker.getSelection().size() == 1) {
                    long id = selectionTracker.getSelection().iterator().next();
                    dataListCallback.onItemSelected(getExtraEdit(), id);
                }
            }
        });

        getLiveData().observe(getViewLifecycleOwner(), listAdapter::setData);

        if (savedInstanceState != null) {
            selectionTracker.onRestoreInstanceState(savedInstanceState.getBundle(STATE_SELECTION));
        }

        getChildFragmentManager().setFragmentResultListener(MessageDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            var action = result.getInt(MessageDialogFragment.RESULT_ACTION);
            var requestCode = result.getInt(MessageDialogFragment.RESULT_REQUEST_CODE);
            if (action == MessageDialogFragment.ACTION_POSITIVE && requestCode == REQUEST_DELETE) {
                var idsToDelete = new ArrayList<Long>();
                for (var id : selectionTracker.getSelection()) {
                    idsToDelete.add(id);
                }

                for (var id : idsToDelete) {
                    deleteItem(id);
                }

                if (actionMode != null) {
                    actionMode.finish();
                }
            }
        });

        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (selectionTracker != null) {
            var selectionBundle = new Bundle();
            selectionTracker.onSaveInstanceState(selectionBundle);
            outState.putBundle(STATE_SELECTION, selectionBundle);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void unselectItem(boolean finishActionMode) {
        if (finishActionMode && actionMode != null) {
            actionMode.finish();
        }

        if (selectionTracker != null) {
            selectionTracker.clearSelection();
        }
    }

    protected abstract int getAlertDeleteManyMessage();

    protected abstract int getExtraEdit();

    protected abstract SparseArray<String> getItemData(T item);

    protected abstract boolean isMissingData(T item);

    protected abstract void deleteItem(long id);

    protected abstract long getItemId(T item);

    protected abstract LiveData<List<T>> getLiveData();

    private abstract static class AbstractDataViewHolder extends RecyclerView.ViewHolder {
        public AbstractDataViewHolder(View itemView) {
            super(itemView);
        }

        public abstract void bind(SparseArray<String> item, boolean isSelected);

        public abstract ItemDetailsLookup.ItemDetails<Long> getItemDetails();
    }

    private class DataViewHolder extends AbstractDataViewHolder implements View.OnClickListener {
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
        }

        @Override
        public void bind(SparseArray<String> item, boolean isSelected) {
            itemView.setActivated(isSelected);

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
        public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            return new ItemDetailsLookup.ItemDetails<>() {
                @Override
                public int getPosition() {
                    return getBindingAdapterPosition();
                }

                @Override
                public Long getSelectionKey() {
                    return listAdapter.getItemId(getBindingAdapterPosition());
                }
            };
        }

        @Override
        public void onClick(View v) {
            if (!selectionTracker.hasSelection()) {
                var id = listAdapter.getItemId(getBindingAdapterPosition());
                dataListCallback.onItemSelected(getExtraEdit(), id);
            }
        }
    }

    private class DataMissingViewHolder extends AbstractDataViewHolder {
        private final TextView title;

        public DataMissingViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.title);
        }

        @Override
        public void bind(SparseArray<String> item, boolean isSelected) {
            itemView.setActivated(isSelected);
            title.setText(item.get(R.id.title));
        }

        @Override
        public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            return new ItemDetailsLookup.ItemDetails<>() {
                @Override
                public int getPosition() {
                    return getBindingAdapterPosition();
                }

                @Override
                public Long getSelectionKey() {
                    return listAdapter.getItemId(getBindingAdapterPosition());
                }
            };
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
                boolean isSelected = selectionTracker != null && selectionTracker.isSelected(getItemId(position));
                holder.bind(itemData, isSelected);
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
                return RecyclerView.NO_ID;
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

        public void setData(List<T> newData) {
            this.data = newData;
            notifyDataSetChanged();
        }

        public int getPosition(long key) {
            for (int i = 0; i < data.size(); i++) {
                if (getItemId(i) == key) {
                    return i;
                }
            }
            return RecyclerView.NO_POSITION;
        }
    }

    private class DataKeyProvider extends ItemKeyProvider<Long> {
        public DataKeyProvider() {
            super(SCOPE_MAPPED);
        }

        @Nullable
        @Override
        public Long getKey(int position) {
            return listAdapter.getItemId(position);
        }

        @Override
        public int getPosition(@NonNull Long key) {
            return listAdapter.getPosition(key);
        }
    }

    private static class DataItemDetailsLookup extends ItemDetailsLookup<Long> {
        private final RecyclerView recyclerView;

        public DataItemDetailsLookup(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        @Nullable
        @Override
        public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (view != null) {
                RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
            if (holder instanceof AbstractDataViewHolder abstractDataViewHolder) {
                return abstractDataViewHolder.getItemDetails();
            }
            }
            return null;
        }
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            dataListCallback.onItemUnselected();
            mode.getMenuInflater().inflate(R.menu.view_data_cab, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            var itemId = item.getItemId();
            if (itemId == R.id.menu_delete) {
                var message = String.format(getString(getAlertDeleteManyMessage()),
                        selectionTracker.getSelection().size());
                MessageDialogFragment.newInstance(
                        REQUEST_DELETE, R.string.alert_delete_title, message,
                        android.R.string.yes, android.R.string.no).show(
                        getChildFragmentManager(), null);
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectionTracker.clearSelection();
            actionMode = null;
        }
    }
}
