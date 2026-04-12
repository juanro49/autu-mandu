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
import android.content.Context;
import android.util.Log;

import net.danlew.android.joda.JodaTimeAndroid;

import org.juanro.autumandu.model.AutuManduDatabase;
import org.juanro.autumandu.util.reminder.ReminderEnablerReceiver;
import org.juanro.autumandu.util.sync.Authenticator;
import org.juanro.autumandu.util.sync.SyncManager;

/**
 * Main application class.
 */
public class Application extends android.app.Application {
    private static final String TAG = "Application";
    private static Application instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize Joda-Time. Manual init is deprecated as it's handled by a ContentProvider,
        // but keeping it here with suppression ensures it's ready for early access components.
        //noinspection deprecation
        JodaTimeAndroid.init(this);

        // Schedule alarms for reminders
        ReminderEnablerReceiver.scheduleAlarms(this);

        // Check for sync accounts and schedule periodic sync if necessary
        var accountManager = AccountManager.get(this);
        var accounts = accountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
        if (accounts.length > 0) {
            SyncManager.schedulePeriodicSync(this);
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
