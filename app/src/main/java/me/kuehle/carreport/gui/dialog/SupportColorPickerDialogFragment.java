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

package me.kuehle.carreport.gui.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;

import com.android.colorpicker.ColorPickerDialog;
import com.android.colorpicker.ColorPickerPalette;

import me.kuehle.carreport.R;

public class SupportColorPickerDialogFragment extends DialogFragment {
    public interface SupportColorPickerDialogFragmentListener {
        void onDialogNegativeClick(int requestCode);

        void onDialogPositiveClick(int requestCode, int color);
    }

    public static SupportColorPickerDialogFragment newInstance(Fragment parent,
                                                               int requestCode, Integer title, int color) {
        SupportColorPickerDialogFragment f = new SupportColorPickerDialogFragment();
        f.setTargetFragment(parent, requestCode);

        Bundle args = new Bundle();
        args.putInt("color", color);
        args.putInt("positive", android.R.string.ok);
        args.putInt("negative", android.R.string.cancel);
        if (title != null) {
            args.putInt("title", title);
        }

        f.setArguments(args);
        return f;
    }

    private int mSelectedColor;

    @NonNull
    @Override
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_color_picker, null);

        final ColorPickerPalette colorPicker = view.findViewById(R.id.palette);
        mSelectedColor = (savedInstanceState != null ? savedInstanceState : args).getInt("color");
        Resources resources = getResources();
        final int[] colors = resources.getIntArray(R.array.selectable_colors);
        final int colorsPerRow = resources.getInteger(R.integer.color_picker_colors_per_row);
        colorPicker.init(ColorPickerDialog.SIZE_LARGE, colorsPerRow,
                color -> {
                    SupportColorPickerDialogFragment.this.setColor(color);
                    colorPicker.drawPalette(colors, color);
                });
        colorPicker.drawPalette(colors, mSelectedColor);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setPositiveButton(args.getInt("positive"),
                (dialog, which) -> getListener().onDialogPositiveClick(getTargetRequestCode(),
                        mSelectedColor));
        builder.setNegativeButton(args.getInt("negative"),
                (dialog, which) -> getListener().onDialogNegativeClick(getTargetRequestCode()));
        if (args.containsKey("title")) {
            builder.setTitle(args.getInt("title"));
        }

        return builder.create();
    }

    public void setColor(int color) {
        mSelectedColor = color;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("color", mSelectedColor);
    }

    private SupportColorPickerDialogFragmentListener getListener() {
        return (SupportColorPickerDialogFragmentListener) getTargetFragment();
    }
}
