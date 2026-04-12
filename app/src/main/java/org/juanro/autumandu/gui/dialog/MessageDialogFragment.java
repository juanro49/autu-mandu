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

package org.juanro.autumandu.gui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class MessageDialogFragment extends DialogFragment {
    public static final String REQUEST_KEY = "org.juanro.autumandu.MESSAGE_DIALOG_REQUEST";
    public static final String RESULT_ACTION = "action";
    public static final String RESULT_REQUEST_CODE = "request_code";

    public static final int ACTION_POSITIVE = 1;
    public static final int ACTION_NEGATIVE = 2;

    private static final String ARG_MESSAGE = "message";
    private static final String ARG_POSITIVE = "positive";
    private static final String ARG_TITLE = "title";
    private static final String ARG_NEGATIVE = "negative";
    private static final String ARG_REQUEST_CODE = "request_code";

    public static MessageDialogFragment newInstance(int requestCode, Integer title, String message, int positive,
                                                    Integer negative) {
        MessageDialogFragment f = new MessageDialogFragment();

        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        args.putInt(ARG_POSITIVE, positive);
        args.putInt(ARG_REQUEST_CODE, requestCode);
        if (title != null) {
            args.putInt(ARG_TITLE, title);
        }
        if (negative != null) {
            args.putInt(ARG_NEGATIVE, negative);
        }

        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        int requestCode = args.getInt(ARG_REQUEST_CODE);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setMessage(args.getString(ARG_MESSAGE));
        builder.setPositiveButton(args.getInt(ARG_POSITIVE), (dialog, which) -> sendResult(ACTION_POSITIVE, requestCode));

        if (args.containsKey(ARG_TITLE)) {
            builder.setTitle(args.getInt(ARG_TITLE));
        }
        if (args.containsKey(ARG_NEGATIVE)) {
            builder.setNegativeButton(args.getInt(ARG_NEGATIVE), (dialog, which) -> sendResult(ACTION_NEGATIVE, requestCode));
        }

        return builder.create();
    }

    private void sendResult(int action, int requestCode) {
        Bundle result = new Bundle();
        result.putInt(RESULT_ACTION, action);
        result.putInt(RESULT_REQUEST_CODE, requestCode);
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }
}
