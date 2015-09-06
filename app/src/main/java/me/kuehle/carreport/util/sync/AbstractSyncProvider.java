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

package me.kuehle.carreport.util.sync;

import android.accounts.Account;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;

import org.json.JSONObject;

import java.io.File;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.gui.AuthenticatorAddAccountActivity;
import me.kuehle.carreport.provider.DataSQLiteOpenHelper;

public abstract class AbstractSyncProvider {
    public abstract long getId();

    public abstract @DrawableRes int getIcon();

    public abstract String getName();

    public abstract void setup(@Nullable Account account, @Nullable String password, @Nullable String authToken, @Nullable JSONObject settings);

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

    public abstract String getRemoteFileRev() throws Exception;

    public abstract String uploadFile() throws Exception;

    public abstract void downloadFile() throws Exception;

    protected File getLocalFile() {
        return new File(DataSQLiteOpenHelper.getInstance(Application.getContext()).getReadableDatabase().getPath());
    }
}
