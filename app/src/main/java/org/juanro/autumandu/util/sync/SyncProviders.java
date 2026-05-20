/*
 * Copyright 2015 Jan Kühle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.juanro.autumandu.util.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.juanro.autumandu.R;

/**
 * Utility class for managing registered synchronization providers.
 */
public final class SyncProviders {
    private static final String TAG = "SyncProviders";
    private static AbstractSyncProvider[] sSyncProviders = null;

    private SyncProviders() {
        // Utility class
    }

    /**
     * Returns all registered sync providers.
     */
    public static AbstractSyncProvider[] getSyncProviders(Context context) {
        if (sSyncProviders == null) {
            String[] classes = getRegisteredSyncProviderClassNames(context);
            Log.i(TAG, "Found " + classes.length + " registered sync provider classes.");

            sSyncProviders = new AbstractSyncProvider[classes.length];
            for (int i = 0; i < classes.length; i++) {
                sSyncProviders[i] = newAbstractSyncProviderInstance(classes[i]);
                if (sSyncProviders[i] != null) {
                    Log.i(TAG, "Initialized provider: " + sSyncProviders[i].getName() + " with ID " + sSyncProviders[i].getId());
                }
            }
        }

        return sSyncProviders;
    }

    /**
     * Returns a sync provider by its ID.
     */
    public static AbstractSyncProvider getSyncProviderById(Context context, long id) {
        for (AbstractSyncProvider provider : getSyncProviders(context)) {
            if (provider != null) {
                Log.d(TAG, "Checking provider: " + provider.getName() + " with ID " + provider.getId() + " against requested ID " + id);
                if (provider.getId() == id) {
                    return provider;
                }
            }
        }

        return null;
    }

    /**
     * Returns the sync provider associated with an account.
     */
    public static AbstractSyncProvider getSyncProviderByAccount(Context context, Account account) {
        AccountManager accountManager = AccountManager.get(context);
        String providerIdStr = accountManager.getUserData(account, Authenticator.KEY_SYNC_PROVIDER);
        Log.i(TAG, "Getting provider for account " + account.name + " (" + account.type + "). Found provider ID string: " + providerIdStr);
        if (providerIdStr == null) {
            Log.e(TAG, "No sync provider ID found in account user data for " + account.name);
            return null;
        }

        try {
            long providerId = Long.parseLong(providerIdStr);
            AbstractSyncProvider provider = getSyncProviderById(context, providerId);
            if (provider == null) {
                Log.e(TAG, "No sync provider found with ID " + providerId);
            }
            return provider;
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid sync provider ID found in account user data: " + providerIdStr);
            return null;
        }
    }

    /**
     * Returns the settings for a sync provider account.
     */
    public static JSONObject getSyncProviderSettings(Context context, Account account) {
        AccountManager accountManager = AccountManager.get(context);
        String settings = accountManager.getUserData(account, Authenticator.KEY_SYNC_PROVIDER_SETTINGS);
        if (settings == null) return null;

        try {
            return new JSONObject(settings);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing sync provider settings JSON.", e);
            return null;
        }
    }

    private static String[] getRegisteredSyncProviderClassNames(Context context) {
        ArrayList<String> items = new ArrayList<>();
        try {
            XmlPullParser xpp = context.getResources().getXml(R.xml.sync_providers);
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = xpp.getName();
                    if ("sync-provider".equals(tagName)) {
                        String className = xpp.getAttributeValue(null, "class");
                        Log.i(TAG, "Found sync-provider tag with class: " + className);
                        if (className != null) {
                            items.add(className);
                        }
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "Error parsing sync_providers.xml file.", e);
        }

        if (items.isEmpty()) {
            Log.e(TAG, "No sync providers found in sync_providers.xml!");
        }
        return items.toArray(new String[0]);
    }

    @SuppressWarnings("unchecked")
    private static AbstractSyncProvider newAbstractSyncProviderInstance(String className) {
        if (className == null) return null;
        try {
            Class<? extends AbstractSyncProvider> clazz = (Class<? extends AbstractSyncProvider>) Class.forName(className);
            Constructor<? extends AbstractSyncProvider> constructor = clazz.getConstructor();
            return constructor.newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalArgumentException |
                InstantiationException | InvocationTargetException | IllegalAccessException e) {
            Log.e(TAG, "Error creating sync provider: " + className, e);
            return null;
        }
    }
}
