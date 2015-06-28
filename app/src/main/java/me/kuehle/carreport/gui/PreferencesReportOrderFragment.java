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

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.R;
import me.kuehle.carreport.data.report.AbstractReport;

public class PreferencesReportOrderFragment extends Fragment {
    private ItemTouchHelper mItemTouchHelper;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_prefs_report_order, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ReportOrderAdapter adapter = new ReportOrderAdapter();

        RecyclerView recyclerView = (RecyclerView) view.findViewById(android.R.id.list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        ItemTouchHelper.Callback callback = new ReportOrderItemTouchHelperCallback(adapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private class ReportOrderViewHolder extends RecyclerView.ViewHolder implements View.OnTouchListener {
        private TextView mTitle;

        public ReportOrderViewHolder(View itemView) {
            super(itemView);

            mTitle = (TextView) itemView.findViewById(android.R.id.text1);
            itemView.findViewById(R.id.drag_handle).setOnTouchListener(this);
        }

        public void bind(AbstractReport report) {
            mTitle.setText(report.getTitle());
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                mItemTouchHelper.startDrag(this);
            }

            return false;
        }
    }

    private class ReportOrderAdapter extends RecyclerView.Adapter<ReportOrderViewHolder> {
        private Preferences mPrefs;
        private List<AbstractReport> mReports;

        public ReportOrderAdapter() {
            mReports = new ArrayList<>();
            mPrefs = new Preferences(getActivity());

            List<Class<? extends AbstractReport>> reportClasses = mPrefs.getReportOrder();
            for (Class<? extends AbstractReport> reportClass : reportClasses) {
                AbstractReport report = AbstractReport.newInstance(reportClass, getActivity());
                if (report != null) {
                    mReports.add(report);
                }
            }
        }

        @Override
        public ReportOrderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_drag,
                    parent, false);
            return new ReportOrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ReportOrderViewHolder holder, int position) {
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
        private ReportOrderAdapter mAdapter;

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
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source,
                              RecyclerView.ViewHolder target) {
            mAdapter.onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        }
    }
}
