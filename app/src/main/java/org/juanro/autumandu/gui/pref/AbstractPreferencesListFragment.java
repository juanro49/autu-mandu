/*
 * Copyright 2026 Juanro49
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

import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.dialog.MessageDialogFragment;
import org.juanro.autumandu.gui.util.AbstractPreferenceActivity;

public abstract class AbstractPreferencesListFragment extends ListFragment implements
        AbstractPreferenceActivity.OptionsMenuListener {

    protected static final int REQUEST_DELETE = 1;
    protected static final int REQUEST_ADD = 2;
    protected static final int REQUEST_EDIT = 3;

    protected class ListMultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {
        private ActionMode mActionMode;
        private long[] mSelectedIds;

        public void finishActionMode() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.menu_delete) {
                mSelectedIds = getListView().getCheckedItemIds();
                checkUsageAndConfirmDelete(mSelectedIds);
                return true;
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(getCabMenu(), menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            int count = getListView().getCheckedItemCount();
            mode.setTitle(String.format(getString(R.string.cab_title_selected), count));
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        public long[] getSelectedIds() {
            return mSelectedIds;
        }
    }

    protected ListMultiChoiceModeListener mMultiChoiceModeListener;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMultiChoiceModeListener = new ListMultiChoiceModeListener();
        getListView().setMultiChoiceModeListener(mMultiChoiceModeListener);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        getParentFragmentManager().setFragmentResultListener(
                MessageDialogFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
                    int requestCode = result.getInt(MessageDialogFragment.RESULT_REQUEST_CODE);
                    int action = result.getInt(MessageDialogFragment.RESULT_ACTION);
                    if (requestCode == REQUEST_DELETE && action == MessageDialogFragment.ACTION_POSITIVE) {
                        deleteSelected(mMultiChoiceModeListener.getSelectedIds());
                        mMultiChoiceModeListener.finishActionMode();
                    }
                });

        getParentFragmentManager().setFragmentResultListener(getEditRequestKey(), getViewLifecycleOwner(), (requestKey, bundle) -> {
            // Nothing to do here by default, but child can override
        });
    }

    protected abstract int getCabMenu();
    protected abstract String getEditRequestKey();
    protected abstract void checkUsageAndConfirmDelete(long[] ids);
    protected abstract void deleteSelected(long[] ids);

    @Override
    public void onStop() {
        super.onStop();
        if (mMultiChoiceModeListener != null) {
            mMultiChoiceModeListener.finishActionMode();
        }
    }
}
