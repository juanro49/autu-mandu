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

package org.juanro.autumandu.gui.pref;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.R;
import org.juanro.autumandu.data.report.AbstractReport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreferencesReportOrderFragment extends PreferenceFragmentCompat {
    private ItemTouchHelper mItemTouchHelper;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        // Este fragmento no usa un XML de preferencias directamente para su contenido principal
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_prefs_report_order, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ReportOrderAdapter adapter = new ReportOrderAdapter();

        RecyclerView recyclerView = view.findViewById(android.R.id.list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        ItemTouchHelper.Callback callback = new ReportOrderItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private class ReportOrderViewHolder extends RecyclerView.ViewHolder implements View.OnTouchListener {
        private final TextView mTitle;

        public ReportOrderViewHolder(View itemView) {
            super(itemView);

            mTitle = itemView.findViewById(android.R.id.text1);
            itemView.findViewById(R.id.drag_handle).setOnTouchListener(this);
        }

        public void bind(AbstractReport report) {
            mTitle.setText(report.getTitle());
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                mItemTouchHelper.startDrag(this);
            }

            return false;
        }
    }

    private class ReportOrderAdapter extends RecyclerView.Adapter<ReportOrderViewHolder> {
        private final Preferences mPrefs;
        private final List<AbstractReport> mReports;

        public ReportOrderAdapter() {
            mReports = new ArrayList<>();
            mPrefs = new Preferences(requireContext());

            List<Class<? extends AbstractReport>> reportClasses = mPrefs.getReportOrder();
            for (Class<? extends AbstractReport> reportClass : reportClasses) {
                AbstractReport report = AbstractReport.newInstance(reportClass, requireContext());
                if (report != null) {
                    mReports.add(report);
                }
            }
        }

        @NonNull
        @Override
        public ReportOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_drag,
                    parent, false);
            return new ReportOrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportOrderViewHolder holder, int position) {
            AbstractReport report = mReports.get(position);
            holder.bind(report);
        }

        public void onItemMove(int fromPosition, int toPosition) {
            Collections.swap(mReports, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);

            saveReportOrder();
        }

        @Override
        public int getItemCount() {
            return mReports.size();
        }

        private void saveReportOrder() {
            List<Class<? extends AbstractReport>> reportClasses = new ArrayList<>(mReports.size());
            for (AbstractReport report : mReports) {
                reportClasses.add(report.getClass());
            }

            mPrefs.setReportOrder(reportClasses);
        }
    }

    private class ReportOrderItemTouchHelperCallback extends ItemTouchHelper.Callback {
        private final ReportOrderAdapter mAdapter;

        public ReportOrderItemTouchHelperCallback(ReportOrderAdapter adapter) {
            mAdapter = adapter;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder source,
                              @NonNull RecyclerView.ViewHolder target) {
            mAdapter.onItemMove(source.getAbsoluteAdapterPosition(), target.getAbsoluteAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        }
    }
}
