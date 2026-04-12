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
import androidx.activity.result.ActivityResultLauncher;
import android.content.Intent;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.File;

import org.juanro.autumandu.Application;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.gui.AuthenticatorAddAccountActivity;
import org.juanro.autumandu.model.AutuManduDatabase;

/**
 * Base class for synchronization providers.
 * Manages local file metadata and defines the sync contract.
 */
public abstract class AbstractSyncProvider {

    public abstract long getId();

    @DrawableRes
    public abstract int getIcon();

    @NonNull
    public abstract String getName();

    /**
     * Initializes the sync provider with account credentials and settings.
     */
    public abstract void setup(@Nullable Account account,
                               @Nullable String password,
                               @Nullable String authToken,
                               @Nullable JSONObject settings)
            throws SyncAuthException, SyncIoException, SyncParseException;

    /**
     * Starts the authentication process for this provider using the modern ActivityResultLauncher.
     */
    public abstract void startAuthentication(@NonNull AuthenticatorAddAccountActivity activity,
                                            @NonNull ActivityResultLauncher<Intent> launcher);

    /**
     * Returns the revision of the local file from preferences.
     */
    @Nullable
    public String getLocalFileRev() {
        return new Preferences(Application.getContext()).getSyncLocalFileRev();
    }

    /**
     * Updates the local file revision in preferences.
     */
    public void setLocalFileRev(@Nullable String rev) {
        new Preferences(Application.getContext()).setSyncLocalFileRev(rev);
    }

    /**
     * Fetches the revision of the remote file.
     */
    @Nullable
    public abstract String getRemoteFileRev() throws SyncAuthException, SyncIoException, SyncParseException;

    /**
     * Uploads the local database file to the remote storage.
     * @return The new remote revision.
     */
    @Nullable
    public abstract String uploadFile() throws SyncAuthException, SyncIoException, SyncParseException;

    /**
     * Downloads the remote database file to the local storage.
     */
    public abstract void downloadFile() throws SyncAuthException, SyncIoException, SyncParseException;

    /**
     * Returns the local path to the Room database file.
     */
    @NonNull
    protected File getLocalFile() {
        return Application.getContext().getDatabasePath(AutuManduDatabase.DATABASE_NAME);
    }
}
