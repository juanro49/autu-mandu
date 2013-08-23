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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import me.kuehle.carreport.util.CSVReader;
import me.kuehle.carreport.util.CSVWriter;
import me.kuehle.carreport.util.Recurrence;
import android.os.Environment;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.TableInfo;
import com.activeandroid.query.Select;

public class CSVExportImport {
	public static final String DIRECTORY = "Car Report CSV";

	private File dir;

	public CSVExportImport() {
		this.dir = new File(Environment.getExternalStorageDirectory(),
				DIRECTORY);
	}

	public boolean export() {
		if (!this.dir.isDirectory()) {
			if (!this.dir.mkdir()) {
				return false;
			}
		}

		Collection<TableInfo> tables = Cache.getTableInfos();
		for (TableInfo table : tables) {
			Collection<Field> fields = table.getFields();
			List<Model> entries = new Select().from(table.getType()).execute();

			List<Object> header = new ArrayList<Object>();
			for (Field field : fields) {
				header.add(table.getColumnName(field));
			}

			CSVWriter csv = new CSVWriter();
			csv.writeLine(header.toArray());
			for (Model entry : entries) {
				List<Object> line = new ArrayList<Object>();
				for (Field field : fields) {
					try {
						line.add(field.get(entry));
					} catch (IllegalArgumentException e) {
						return false;
					} catch (IllegalAccessException e) {
						return false;
					}
				}

				csv.writeLine(line.toArray());
			}

			csv.toFile(new File(dir, table.getTableName() + ".csv"));
		}

		return true;
	}

	public boolean canExport() {
		return dir.canWrite();
	}

	public boolean canImport() {
		return allExportFilesExist();
	}

	public boolean allExportFilesExist() {
		Collection<TableInfo> tables = Cache.getTableInfos();
		for (TableInfo table : tables) {
			File file = new File(dir, table.getTableName() + ".csv");
			if (!file.isFile()) {
				return false;
			}
		}

		return true;
	}

	public boolean anyExportFileExist() {
		Collection<TableInfo> tables = Cache.getTableInfos();
		for (TableInfo table : tables) {
			File file = new File(dir, table.getTableName() + ".csv");
			if (file.isFile()) {
				return true;
			}
		}

		return false;
	}

	public boolean import_() {
		if (!allExportFilesExist()) {
			return false;
		}

		Collection<TableInfo> tables = Cache.getTableInfos();
		for (TableInfo table : tables) {
			Collection<Field> fields = table.getFields();

			CSVReader csv = CSVReader.fromFile(
					new File(dir, table.getTableName() + ".csv"), true);

			ActiveAndroid.beginTransaction();
			try {
				for (int i = 0; i < csv.getRowCount(); i++) {
					long id = csv.getLong(i, "Id");

					Model entry = Model.load(table.getType(), id);
					if (entry == null) {
						Constructor<?> constructor = table.getType()
								.getConstructor();
						entry = (Model) constructor.newInstance();
					}

					for (Field field : fields) {
						Object value = null;

						Class<?> fieldType = field.getType();
						if (fieldType.equals(Boolean.class)
								|| fieldType.equals(boolean.class)) {
							value = csv
									.getBoolen(i, table.getColumnName(field));
						} else if (fieldType.equals(Integer.class)
								|| fieldType.equals(int.class)) {
							value = csv.getInt(i, table.getColumnName(field));
						} else if (fieldType.equals(Long.class)
								|| fieldType.equals(long.class)) {
							value = csv.getLong(i, table.getColumnName(field));
						} else if (fieldType.equals(Float.class)
								|| fieldType.equals(float.class)) {
							value = csv.getFloat(i, table.getColumnName(field));
						} else if (fieldType.equals(Date.class)) {
							value = csv.getDate(i, table.getColumnName(field));
						} else if (fieldType.equals(Recurrence.class)) {
							value = csv.getRecurrence(i,
									table.getColumnName(field));
						} else if (fieldType.equals(String.class)) {
							value = csv
									.getString(i, table.getColumnName(field));
						}

						field.set(entry, value);
					}

					entry.save();
				}

				ActiveAndroid.setTransactionSuccessful();
			} catch (NoSuchMethodException e) {
				return false;
			} catch (IllegalArgumentException e) {
				return false;
			} catch (InstantiationException e) {
				return false;
			} catch (IllegalAccessException e) {
				return false;
			} catch (InvocationTargetException e) {
				return false;
			} finally {
				ActiveAndroid.endTransaction();
			}
		}

		return true;
	}
}
