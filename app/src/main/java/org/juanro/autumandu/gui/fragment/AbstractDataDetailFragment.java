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

package org.juanro.autumandu.gui.fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.appcompat.app.ActionBar;

import com.google.android.material.textfield.TextInputLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.widget.EditText;

import java.text.NumberFormat;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.dialog.MessageDialogFragment;
import org.juanro.autumandu.gui.util.FragmentUtils;

public abstract class AbstractDataDetailFragment extends Fragment {
    public interface OnItemActionListener {
        void onItemCanceled();

        void onItemDeleted();

        void onItemSaved(long newId);

        default void onItemSavedAsync(long newId) {
            onItemSaved(newId);
        }

        default void onItemDeletedAsync() {
            onItemDeleted();
        }
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
        if (FragmentUtils.shouldDisableAndDecrement()) {
            Animation a = new Animation() {};
            a.setDuration(0);
            return a;
        }

        return super.onCreateAnimation(transit, enter, nextAnim);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActionBar actionBar = ((androidx.appcompat.app.AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

            mSavedABTitle = actionBar.getTitle();
            if (isInEditMode()) {
                actionBar.setTitle(getTitleForEdit());
            } else {
                actionBar.setTitle(getTitleForNew());
            }
        }

        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.edit, menu);

                if (!isInEditMode()) {
                    menu.removeItem(R.id.menu_delete);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.menu_save) {
                    if (validate()) {
                        saveAsync();
                    }
                    return true;
                } else if (id == R.id.menu_cancel) {
                    mOnItemActionListener.onItemCanceled();
                    return true;
                } else if (id == R.id.menu_delete) {
                    var message = getString(getAlertDeleteMessage());
                    if (getAlertDeleteMessage() == R.string.alert_delete_tire_message) {
                        message = getString(R.string.alert_delete_tire_message, 1);
                    }
                    MessageDialogFragment.newInstance(DELETE_REQUEST_CODE,
                            R.string.alert_delete_title,
                            message, android.R.string.yes,
                            android.R.string.no).show(getParentFragmentManager(), null);
                    return true;
                } else if (id == android.R.id.home) {
                    mOnItemActionListener.onItemCanceled();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    protected void saveAsync() {
        long newId = save();
        mOnItemActionListener.onItemSaved(newId);
    }

    protected void deleteAsync() {
        delete();
        mOnItemActionListener.onItemDeleted();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            if (getParentFragment() != null) {
                mOnItemActionListener = (OnItemActionListener) getParentFragment();
            } else {
                mOnItemActionListener = (OnItemActionListener) context;
            }
        } catch (ClassCastException e) {
            throw new ClassCastException(context.getClass().getSimpleName()
                    + " or parent fragment must implement OnItemActionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mId = args.getLong(EXTRA_ID, EXTRA_ID_DEFAULT);
        } else {
            mId = EXTRA_ID_DEFAULT;
        }

        getParentFragmentManager().setFragmentResultListener(MessageDialogFragment.REQUEST_KEY, this, (requestKey, bundle) -> {
            int action = bundle.getInt(MessageDialogFragment.RESULT_ACTION);
            int requestCode = bundle.getInt(MessageDialogFragment.RESULT_REQUEST_CODE);
            if (action == MessageDialogFragment.ACTION_POSITIVE && requestCode == DELETE_REQUEST_CODE) {
                deleteAsync();
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(getLayout(), container, false);
        initFields(savedInstanceState, v);
        fillFields(savedInstanceState, v);
        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ActionBar actionBar = ((androidx.appcompat.app.AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(mSavedABTitle);
        }
    }

    protected double getDoubleFromEditText(EditText editText) {
        String strDouble = editText.getText().toString();
        try {
            return Double.parseDouble(strDouble);
        } catch (NumberFormatException e) {
            try {
                Number number = NumberFormat.getNumberInstance().parse(strDouble);
                return (number != null) ? number.doubleValue() : 0;
            } catch (Exception f) {
                return 0;
            }
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

        ViewParent parent = editText.getParent().getParent();
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
