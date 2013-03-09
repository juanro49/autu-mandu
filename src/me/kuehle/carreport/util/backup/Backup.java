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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import me.kuehle.carreport.db.Helper;
import android.os.Environment;

public class Backup {
	public static final String FILE_NAME = "carreport.backup";

	private File dir;
	private File dbFile;
	private File backupFile;

	public Backup() {
		dir = Environment.getExternalStorageDirectory();
		dbFile = new File(Helper.getInstance().getReadableDatabase().getPath());
		backupFile = new File(dir, FILE_NAME);
	}

	public boolean backup() {
		synchronized (Helper.dbLock) {
			return copyFile(dbFile, backupFile);
		}
	}

	public boolean backupFileExists() {
		return backupFile.isFile();
	}

	public boolean canBackup() {
		return dir.canWrite();
	}

	public boolean canRestore() {
		return backupFile.isFile();
	}

	public boolean restore() {
		synchronized (Helper.dbLock) {
			return copyFile(backupFile, dbFile);
		}
	}

	private boolean copyFile(File from, File to) {
		try {
			FileInputStream inStream = new FileInputStream(from);
			FileOutputStream outStream = new FileOutputStream(to);
			FileChannel src = inStream.getChannel();
			FileChannel dst = outStream.getChannel();
			dst.transferFrom(src, 0, src.size());
			src.close();
			dst.close();
			inStream.close();
			outStream.close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
