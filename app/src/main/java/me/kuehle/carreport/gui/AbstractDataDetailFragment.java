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

package me.kuehle.carreport.gui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.activeandroid.Model;
import com.rengwuxian.materialedittext.MaterialEditText;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.SupportMessageDialogFragment;
import me.kuehle.carreport.gui.dialog.SupportMessageDialogFragment.SupportMessageDialogFragmentListener;

public abstract class AbstractDataDetailFragment extends Fragment implements
        SupportMessageDialogFragmentListener {
    public interface OnItemActionListener {
        public void onItemCanceled();

        public void onItemDeleted();

        public void onItemSaved();
    }

    public static final String EXTRA_ID = "id";
    public static final long EXTRA_ID_DEFAULT = -1;
    public static final String EXTRA_CAR_ID = "car_id";
    public static final String EXTRA_ALLOW_CANCEL = "allow_cancel";
    public static final boolean EXTRA_ALLOW_CANCEL_DEFAULT = true;

    protected OnItemActionListener onItemActionListener;
    protected Model editItem = null;

    private static final int DELETE_REQUEST_CODE = 0;

    private CharSequence savedABTitle;
    private boolean allowCancel;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBar actionBar = MainActivity.getSupportActionBar(this);
        actionBar.setDisplayHomeAsUpEnabled(allowCancel);

        savedABTitle = actionBar.getTitle();
        if (isInEditMode()) {
            actionBar.setTitle(getTitleForEdit());
        } else {
            actionBar.setTitle(getTitleForNew());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            if (getParentFragment() != null) {
                onItemActionListener = (OnItemActionListener) getParentFragment();
            } else {
                onItemActionListener = (OnItemActionListener) activity;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnItemActionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long id = getArguments().getLong(EXTRA_ID, EXTRA_ID_DEFAULT);
        if (id != EXTRA_ID_DEFAULT) {
            editItem = getEditItem(id);
        }

        allowCancel = getArguments().getBoolean(EXTRA_ALLOW_CANCEL,
                EXTRA_ALLOW_CANCEL_DEFAULT);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.edit, menu);

        if (!isInEditMode()) {
            menu.removeItem(R.id.menu_delete);
        }

        if (!allowCancel) {
            menu.removeItem(R.id.menu_cancel);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(getLayout(), container, false);
        initFields(savedInstanceState, v);
        fillFields(savedInstanceState, v);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActionBar actionBar = MainActivity.getSupportActionBar(this);
        actionBar.setTitle(savedABTitle);
    }

    @Override
    public void onDialogNegativeClick(int requestCode) {
    }

    @Override
    public void onDialogPositiveClick(int requestCode) {
        if (requestCode == DELETE_REQUEST_CODE) {
            editItem.delete();

            Toast.makeText(getActivity(), getToastDeletedMessage(), Toast.LENGTH_SHORT).show();
            onItemActionListener.onItemDeleted();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                if (validate()) {
                    save();

                    Toast.makeText(getActivity(), getToastSavedMessage(), Toast.LENGTH_SHORT).show();
                    onItemActionListener.onItemSaved();

                    Application.dataChanged();
                }

                return true;
            case R.id.menu_cancel:
                onItemActionListener.onItemCanceled();
                return true;
            case R.id.menu_delete:
                SupportMessageDialogFragment.newInstance(this, DELETE_REQUEST_CODE,
                        R.string.alert_delete_title,
                        getString(getAlertDeleteMessage()), android.R.string.yes,
                        android.R.string.no).show(getFragmentManager(), null);
                return true;
            case android.R.id.home:
                onItemActionListener.onItemCanceled();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected double getDoubleFromEditText(EditText editText, double defaultValue) {
        String strDouble = editText.getText().toString();
        try {
            return Double.parseDouble(strDouble);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected int getIntegerFromEditText(EditText editText, int defaultValue) {
        String strInt = editText.getText().toString();
        try {
            return Integer.parseInt(strInt);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected boolean isInEditMode() {
        return editItem != null;
    }

    protected void addUnitToHint(EditText editText, CharSequence unit) {
        CharSequence hint = editText.getHint();
        CharSequence newHint = String.format("%s [%s]", hint, unit);

        editText.setHint(newHint);
        if (editText instanceof MaterialEditText) {
            ((MaterialEditText)editText).setFloatingLabelText(newHint);
        }
    }

    protected abstract void fillFields(Bundle savedInstanceState, View v);

    protected abstract int getAlertDeleteMessage();

    protected abstract Model getEditItem(long id);

    protected abstract int getLayout();

    protected abstract int getTitleForEdit();

    protected abstract int getTitleForNew();

    protected abstract int getToastDeletedMessage();

    protected abstract int getToastSavedMessage();

    protected abstract void initFields(Bundle savedInstanceState, View v);

    protected abstract void save();

    protected abstract boolean validate();
}
