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

package org.juanro.autumandu;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.util.reminder.ReminderEnablerReceiver;
import org.juanro.autumandu.util.sync.Authenticator;
import org.juanro.autumandu.util.sync.SyncManager;

/**
 * Main application class.
 * Manages global state, themes, and Material You integration.
 */
public class Application extends android.app.Application implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "Application";
    private static Application instance;
    private final List<Activity> activities = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Register for preference changes to handle theme switching globally
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        // Keep track of all activities to handle recreations
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                activities.add(activity);
            }
            @Override
            public void onActivityStarted(@NonNull Activity activity) {}
            @Override
            public void onActivityResumed(@NonNull Activity activity) {}
            @Override
            public void onActivityPaused(@NonNull Activity activity) {}
            @Override
            public void onActivityStopped(@NonNull Activity activity) {}
            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                activities.remove(activity);
            }
        });

        // Initialize Dynamic Colors (Material You) ONCE
        // The precondition will be checked on every activity creation/recreation
        DynamicColors.applyToActivitiesIfAvailable(
                this,
                new DynamicColorsOptions.Builder()
                        .setPrecondition((activity, themeResId) -> new Preferences(activity).isDynamicColorEnabled())
                        .build()
        );

        // Apply theme configuration
        applyThemeConfiguration();

        // Schedule alarms for reminders
        ReminderEnablerReceiver.scheduleAlarms(this);

        // Check for sync accounts and schedule periodic sync if necessary
        var accountManager = AccountManager.get(this);
        var accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
        if (accounts.length > 0) {
            SyncManager.schedulePeriodicSync(this);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("ui_theme".equals(key) || "ui_dynamic_color".equals(key)) {
            Log.d(TAG, "UI Preference changed: " + key + ". Refreshing theme...");
            applyThemeConfiguration();

            // Recreate all activities to apply changes everywhere (including background ones)
            synchronized (activities) {
                for (Activity activity : new ArrayList<>(activities)) {
                    activity.recreate();
                }
            }
        }
    }

    /**
     * Applies the theme (Light/Dark/System) based on user preferences.
     */
    private void applyThemeConfiguration() {
        Preferences preferences = new Preferences(this);

        // Set Night Mode based on preference
        String theme = preferences.getTheme();
        int mode = switch (theme) {
            case "light" -> AppCompatDelegate.MODE_NIGHT_NO;
            case "dark" -> AppCompatDelegate.MODE_NIGHT_YES;
            default -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        };

        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode);
        }
    }

    /**
     * @return the global application context.
     */
    public static Context getContext() {
        return instance;
    }

    /**
     * Resets the database singleton instances (Room).
     */
    public static void closeDatabases() {
        if (instance != null) {
            Log.v(TAG, "Closing Database via Room abstraction.");
            AutuManduDatabase.resetInstance();
        }
    }
}
