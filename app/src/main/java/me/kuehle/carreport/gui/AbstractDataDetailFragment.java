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

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.widget.EditText;

import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.dialog.SupportMessageDialogFragment;
import me.kuehle.carreport.gui.dialog.SupportMessageDialogFragment.SupportMessageDialogFragmentListener;
import me.kuehle.carreport.gui.util.FragmentUtils;

public abstract class AbstractDataDetailFragment extends Fragment implements
        SupportMessageDialogFragmentListener {
    public interface OnItemActionListener {
        void onItemCanceled();

        void onItemDeleted();

        void onItemSaved(long newId);
    }

    public static final String EXTRA_ID = "id";
    public static final long EXTRA_ID_DEFAULT = -1;
    public static final String EXTRA_CAR_ID = "car_id";

    protected OnItemActionListener mOnItemActionListener;
    protected long mId;

    private static final int DELETE_REQUEST_CODE = 0;

    private CharSequence mSavedABTitle;

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (FragmentUtils.DISABLE_FRAGMENT_ANIMATIONS > 0) {
            FragmentUtils.DISABLE_FRAGMENT_ANIMATIONS--;

            Animation a = new Animation() {};
            a.setDuration(0);
            return a;
        }

        return super.onCreateAnimation(transit, enter, nextAnim);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ActionBar actionBar = MainActivity.getSupportActionBar(this);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

            mSavedABTitle = actionBar.getTitle();
            if (isInEditMode()) {
                actionBar.setTitle(getTitleForEdit());
            } else {
                actionBar.setTitle(getTitleForNew());
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            if (getParentFragment() != null) {
                mOnItemActionListener = (OnItemActionListener) getParentFragment();
            } else {
                mOnItemActionListener = (OnItemActionListener) context;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " or parent fragment must implement OnItemActionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mId = getArguments().getLong(EXTRA_ID, EXTRA_ID_DEFAULT);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.edit, menu);

        if (!isInEditMode()) {
            menu.removeItem(R.id.menu_delete);
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
        if (actionBar != null) {
            actionBar.setTitle(mSavedABTitle);
        }
    }

    @Override
    public void onDialogNegativeClick(int requestCode) {
    }

    @Override
    public void onDialogPositiveClick(int requestCode) {
        if (requestCode == DELETE_REQUEST_CODE) {
            delete();

            mOnItemActionListener.onItemDeleted();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                if (validate()) {
                    long newId = save();

                    mOnItemActionListener.onItemSaved(newId);
                }

                return true;
            case R.id.menu_cancel:
                mOnItemActionListener.onItemCanceled();
                return true;
            case R.id.menu_delete:
                SupportMessageDialogFragment.newInstance(this, DELETE_REQUEST_CODE,
                        R.string.alert_delete_title,
                        getString(getAlertDeleteMessage()), android.R.string.yes,
                        android.R.string.no).show(getFragmentManager(), null);
                return true;
            case android.R.id.home:
                mOnItemActionListener.onItemCanceled();
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
        return mId != EXTRA_ID_DEFAULT;
    }

    protected void addUnitToHint(EditText editText, int hintResource, CharSequence unit) {
        CharSequence newHint = String.format("%s [%s]", getString(hintResource), unit);

        ViewParent parent = editText.getParent();
        if (parent instanceof TextInputLayout) {
            ((TextInputLayout) parent).setHint(newHint);
        } else {
            editText.setHint(newHint);
        }
    }

    protected abstract void fillFields(Bundle savedInstanceState, View v);

    protected abstract int getAlertDeleteMessage();

    protected abstract int getLayout();

    protected abstract int getTitleForEdit();

    protected abstract int getTitleForNew();

    protected abstract void initFields(Bundle savedInstanceState, View v);

    protected abstract long save();

    protected abstract void delete();

    protected abstract boolean validate();
}
