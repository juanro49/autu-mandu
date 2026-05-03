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

package org.juanro.autumandu.gui.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.juanro.autumandu.R;

public abstract class AbstractEditDialogFragment extends DialogFragment {
    public static final String RESULT_ACTION = "action";
    public static final String RESULT_REQUEST_CODE = "request_code";

    public static final int ACTION_POSITIVE = 1;
    public static final int ACTION_NEGATIVE = 2;

    protected static final String ARG_ID = "item_id";
    protected static final String ARG_REQUEST_CODE = "request_code";

    protected EditText mEdtName;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(requireActivity());
        @SuppressLint("InflateParams")
        View view = getLayoutInflater().inflate(getLayout(), null);
        dialog.setContentView(view);

        mEdtName = view.findViewById(R.id.edt_name);

        long currentId = getArguments() != null ? getArguments().getLong(ARG_ID, 0) : 0;
        dialog.setTitle(currentId == 0 ? getAddTitle() : getEditTitle());

        initFields(view, savedInstanceState);

        Button btnOk = view.findViewById(R.id.btn_ok);
        btnOk.setOnClickListener(v -> {
            if (save()) {
                sendResult(ACTION_POSITIVE);
                dialog.dismiss();
            }
        });

        Button btnCancel = view.findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> {
            sendResult(ACTION_NEGATIVE);
            dialog.dismiss();
        });

        return dialog;
    }

    protected abstract int getLayout();
    protected abstract int getAddTitle();
    protected abstract int getEditTitle();
    protected abstract void initFields(View view, Bundle savedInstanceState);
    protected abstract boolean save();
    protected abstract String getRequestKey();

    protected void sendResult(int action) {
        Bundle result = new Bundle();
        result.putInt(RESULT_ACTION, action);
        result.putInt(RESULT_REQUEST_CODE, getArguments() != null ? getArguments().getInt(ARG_REQUEST_CODE) : 0);
        getParentFragmentManager().setFragmentResult(getRequestKey(), result);
    }
}
