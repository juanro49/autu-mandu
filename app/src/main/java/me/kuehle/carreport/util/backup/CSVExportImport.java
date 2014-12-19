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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
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

	private File externalStorageDir;
	private File exportDir;

	public CSVExportImport() {
		externalStorageDir = Environment.getExternalStorageDirectory();
		exportDir = new File(externalStorageDir, DIRECTORY);
	}

	public boolean export() {
		if (!exportDir.isDirectory()) {
			if (!exportDir.mkdir()) {
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
					Object value;
					try {
						value = field.get(entry);
					} catch (IllegalArgumentException e) {
						return false;
					} catch (IllegalAccessException e) {
						return false;
					}

					if (value != null
							&& Model.class.isAssignableFrom(field.getType())) {
						line.add(((Model) value).id);
					} else {
						line.add(value);
					}
				}

				csv.writeLine(line.toArray());
			}

			csv.toFile(new File(exportDir, table.getTableName() + ".csv"));
		}

		return true;
	}

	public boolean canExport() {
		return externalStorageDir.canWrite();
	}

	public boolean canImport() {
		return allExportFilesExist();
	}

	public boolean allExportFilesExist() {
		Collection<TableInfo> tables = Cache.getTableInfos();
		for (TableInfo table : tables) {
			File file = new File(exportDir, table.getTableName() + ".csv");
			if (!file.isFile()) {
				return false;
			}
		}

		return true;
	}

	public boolean anyExportFileExist() {
		Collection<TableInfo> tables = Cache.getTableInfos();
		for (TableInfo table : tables) {
			File file = new File(exportDir, table.getTableName() + ".csv");
			if (file.isFile()) {
				return true;
			}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	public boolean import_() {
		if (!allExportFilesExist()) {
			return false;
		}

		TableInfo[] tables = Cache.getTableInfos().toArray(new TableInfo[0]);
		sortByReferences(tables);

		for (TableInfo table : tables) {
			Collection<Field> fields = table.getFields();

			CSVReader csv = CSVReader.fromFile(
					new File(exportDir, table.getTableName() + ".csv"), true);

			ActiveAndroid.beginTransaction();
			try {
				for (int i = 0; i < csv.getRowCount(); i++) {
					Long id = csv.getLong(i, "Id");

					Model entry = null;
					if (id != null) {
						entry = Model.load(table.getType(), id);
					}

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
							value = csv.getBoolean(i,
									table.getColumnName(field));
						} else if (fieldType.equals(Integer.class)
								|| fieldType.equals(int.class)) {
							value = csv.getInteger(i,
									table.getColumnName(field));
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
						} else if (Model.class.isAssignableFrom(fieldType)) {
							value = Model.load(
									(Class<? extends Model>) fieldType,
									csv.getLong(i, table.getColumnName(field)));
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

	/**
	 * Get table infos and sort them, so tables with foreign key constraints get
	 * handled after their parent table. Therefore assign every table a number,
	 * which indicates at which position it can be imported. The position will
	 * be max(ref1.pos, ref2.pos, ...) + 1. The tables which do not have any
	 * foreign keys have position 0 and can be imported first.
	 */
	private void sortByReferences(TableInfo[] tables) {
		// Get position for every table.
		final HashMap<Class<? extends Model>, Integer> order = new HashMap<Class<? extends Model>, Integer>();
		while (order.size() < tables.length) {
			for (TableInfo table : tables) {
				// Do not process tables, which already have a position.
				if (order.containsKey(table.getType())) {
					continue;
				}

				int position = 0;
				boolean skip = false;

				// For every foreign key check the position of the referenced
				// table. If any referenced table does not have a position yet,
				// skip the current table. It will be handled in one of the next
				// loops.
				Collection<Field> fields = table.getFields();
				for (Field field : fields) {
					if (Model.class.isAssignableFrom(field.getType())) {
						if (order.containsKey(field.getType())) {
							position = Math.max(position,
									order.get(field.getType()) + 1);
						} else {
							skip = true;
							break;
						}
					}
				}

				if (!skip) {
					order.put(table.getType(), position);
				}
			}
		}

		// Sort the tables by the assigned positions.
		Arrays.sort(tables, new Comparator<TableInfo>() {
			@Override
			public int compare(TableInfo lhs, TableInfo rhs) {
				return order.get(lhs.getType()).compareTo(
						order.get(rhs.getType()));
			}
		});
	}
}
