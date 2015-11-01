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

package me.kuehle.carreport.util.backup;

import android.content.Context;
import android.os.Environment;

import java.io.File;

import me.kuehle.carreport.Application;
import me.kuehle.carreport.provider.DataSQLiteOpenHelper;
import me.kuehle.carreport.util.FileCopyUtil;

public class Backup {
    public static final String FILE_NAME = "carreport.backup";

    private File dbFile;
    private File backupFile;

    public Backup(Context context) {
        File dir = Environment.getExternalStorageDirectory();
        dbFile = new File(DataSQLiteOpenHelper.getInstance(context).getReadableDatabase().getPath());
        backupFile = new File(dir, FILE_NAME);
    }

    public boolean backup() {
        return FileCopyUtil.copyFile(dbFile, backupFile);
    }

    public boolean backupFileExists() {
        return backupFile.isFile();
    }

    public boolean restore() {
        boolean result = FileCopyUtil.copyFile(backupFile, dbFile);
        if (result) {
            Application.reinitializeDatabase();
        }

        return result;
    }
}
