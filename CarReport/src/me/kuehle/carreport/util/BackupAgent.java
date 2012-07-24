package me.kuehle.carreport.util;

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
		}
	}
}
