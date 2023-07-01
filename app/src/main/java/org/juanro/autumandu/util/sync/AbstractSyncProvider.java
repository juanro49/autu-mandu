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
import android.content.Intent;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.File;

import org.juanro.autumandu.Application;
import org.juanro.autumandu.Preferences;
import org.juanro.autumandu.gui.AuthenticatorAddAccountActivity;
import org.juanro.autumandu.provider.DataSQLiteOpenHelper;

public abstract class AbstractSyncProvider {
    public abstract long getId();

    @DrawableRes
    public abstract int getIcon();

    public abstract String getName();

    public abstract void setup(@Nullable Account account, @Nullable String password, @Nullable String authToken, @Nullable JSONObject settings) throws SyncAuthException, SyncIoException, SyncParseException;

    public abstract void startAuthentication(AuthenticatorAddAccountActivity activity);

    public abstract void continueAuthentication(AuthenticatorAddAccountActivity activity, int requestCode, int resultCode, @Nullable Intent data);

    public String getLocalFileRev() {
        Preferences prefs = new Preferences(Application.getContext());
        return prefs.getSyncLocalFileRev();
    }

    public void setLocalFileRev(String rev) {
        Preferences prefs = new Preferences(Application.getContext());
        prefs.setSyncLocalFileRev(rev);
    }

    public abstract String getRemoteFileRev() throws SyncAuthException, SyncIoException, SyncParseException;

    public abstract String uploadFile() throws SyncAuthException, SyncIoException, SyncParseException;

    public abstract void downloadFile() throws SyncAuthException, SyncIoException, SyncParseException;

    protected File getLocalFile() {
        return new File(DataSQLiteOpenHelper.getInstance(Application.getContext()).getReadableDatabase().getPath());
    }
}
