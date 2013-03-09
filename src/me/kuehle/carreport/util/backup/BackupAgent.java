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

import java.io.IOException;

import me.kuehle.carreport.db.Helper;
import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

public class BackupAgent extends BackupAgentHelper {
	public static final String DBS_BACKUP_KEY = "dbs";
	public static final String PREFS_BACKUP_KEY = "prefs";

	@Override
	public void onCreate() {
		FileBackupHelper dbs = new FileBackupHelper(this, "../databases/"
				+ Helper.DB_NAME);
		addHelper(DBS_BACKUP_KEY, dbs);

		SharedPreferencesBackupHelper prefs = new SharedPreferencesBackupHelper(
				this, getPackageName() + "_preferences");
		addHelper(PREFS_BACKUP_KEY, prefs);
	}

	@Override
	public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data,
			ParcelFileDescriptor newState) throws IOException {
		synchronized (Helper.dbLock) {
			super.onBackup(oldState, data, newState);
		}
	}

	@Override
	public void onRestore(BackupDataInput data, int appVersionCode,
			ParcelFileDescriptor newState) throws IOException {
		synchronized (Helper.dbLock) {
			super.onRestore(data, appVersionCode, newState);
			Helper.getInstance().reinitialize();
		}
	}
}
