package org.juanro.autumandu.gui.util;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;

/**
 * Source: <a href="https://stackoverflow.com/a/3755608">https://stackoverflow.com/a/3755608</a>
 * This Preference allows to save integers directly.
 * Migrated to androidx.preference for modern compatibility.
 */
public class IntEditTextPreference extends EditTextPreference {
    public IntEditTextPreference(@NonNull Context context) {
        super(context);
    }

    public IntEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public IntEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public IntEditTextPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        return String.valueOf(getPersistedInt(-1));
    }

    @Override
    protected boolean persistString(String value) {
        return persistInt(Integer.parseInt(value));
    }
}
