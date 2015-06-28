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
import android.accounts.NetworkErrorException;
import android.content.Intent;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;

import com.google.api.services.drive.model.App;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Date;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.Preferences;
import me.kuehle.carreport.gui.AuthenticatorAddAccountActivity;
import me.kuehle.carreport.provider.DataSQLiteOpenHelper;

public abstract class AbstractSyncProvider {
    public abstract long getId();

    public abstract @DrawableRes int getIcon();

    public abstract String getName();

    public abstract void setup(@Nullable Account account, @Nullable String authToken);

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

    protected boolean copyFile(File from, File to) {
        try {
            FileInputStream inStream = new FileInputStream(from);
            FileOutputStream outStream = new FileOutputStream(to);
            FileChannel src = inStream.getChannel();
            FileChannel dst = outStream.getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            outStream.close();
            inStream.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean copyFile(InputStream from, OutputStream to) {
        try {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = from.read(buffer)) > 0) {
                to.write(buffer, 0, len);
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
