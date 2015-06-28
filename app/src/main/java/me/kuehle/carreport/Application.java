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

package me.kuehle.carreport;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import java.util.Date;

import me.kuehle.carreport.provider.DataProvider;
import me.kuehle.carreport.provider.DataSQLiteOpenHelper;
import me.kuehle.carreport.util.reminder.ReminderEnablerReceiver;
import me.kuehle.carreport.util.reminder.ReminderService;
import me.kuehle.carreport.util.sync.Authenticator;

public class Application extends android.app.Application {
    private static Application instance;

    public static void dataChanged() {
        if (instance != null) {
            ReminderService.updateNotification(instance);
        }
    }

    public static Context getContext() {
        return instance;
    }

    public static void reinitializeDatabase() {
        if (instance != null) {
            DataSQLiteOpenHelper.getInstance(instance).close();
        }
    }

    private AccountManager mAccountManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        mAccountManager = AccountManager.get(this);

        System.setProperty("org.joda.time.DateTimeZone.Provider",
                "org.joda.time.tz.UTCProvider");

        ReminderEnablerReceiver.scheduleAlarms(this);

        upgradeOldSyncServiceToNewSyncAdapterWithAccounts();
        setupSyncOnAnyDataChange();
    }

    private void upgradeOldSyncServiceToNewSyncAdapterWithAccounts() {
        Preferences prefs = new Preferences(this);

        String syncProvider = prefs.getDeprecatedSynchronizationProvider();
        if (syncProvider != null) {
            Account account = null;
            String authToken = null;
            if (syncProvider.equals("me.kuehle.carreport.util.backup.DropboxSynchronizationProvider")) {
                account = new Account(prefs.getDeprecatedDropboxAccount(), Authenticator.ACCOUNT_TYPE);
                authToken = prefs.getDeprecatedDropboxAccessToken();

                prefs.setSyncLocalFileRev(prefs.getDeprecatedDropboxLocalRev());
            } else if (syncProvider.equals("me.kuehle.carreport.util.backup.GoogleDriveSynchronizationProvider")) {
                account = new Account(prefs.getDeprecatedGoogleDriveAccount(), Authenticator.ACCOUNT_TYPE);
                authToken = null;

                Date modifiedDate = prefs.getDeprecatedGoogleDriveLocalModifiedDate();
                String rev = modifiedDate != null ? String.valueOf(modifiedDate.getTime()) : null;
                prefs.setSyncLocalFileRev(rev);
            }

            if (account != null) {
                AccountManager accountManager = AccountManager.get(this);
                accountManager.addAccountExplicitly(account, null, null);
                accountManager.setAuthToken(account, Authenticator.AUTH_TOKEN_TYPE, authToken);
                accountManager.setUserData(account, Authenticator.KEY_SYNC_PROVIDER,
                        String.valueOf(Authenticator.getSyncProviderByAccount(account).getId()));
            }

            prefs.removeDeprecatedSyncSettings();
        }
    }

    private void setupSyncOnAnyDataChange() {
        ContentObserver contentObserver = new ContentObserver(new Handler()) {
            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }

            @Override
            public void onChange(boolean selfChange) {
                Account[] accounts = mAccountManager.getAccountsByType(Authenticator.ACCOUNT_TYPE);
                if (accounts.length > 0) {
                    ContentResolver.requestSync(accounts[0], DataProvider.AUTHORITY, new Bundle());
                }
            }
        };

        ContentResolver contentResolver = getContentResolver();
        contentResolver.registerContentObserver(Uri.parse(DataProvider.CONTENT_URI_BASE), true,
                contentObserver);
    }
}
