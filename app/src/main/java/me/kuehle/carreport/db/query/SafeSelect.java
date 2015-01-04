/*
 * Copyright 2013 Jan Kühle
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

package me.kuehle.carreport.db.query;

import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.TableInfo;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;

import java.lang.reflect.Field;
import java.util.Collection;

public class SafeSelect {
    public static From from(Class<? extends Model> table) {
        return new Select(getColumnList(table)).from(table);
    }

    private static Select.Column[] getColumnList(Class<? extends Model> table) {
        TableInfo tableInfo = Cache.getTableInfo(table);
        String tableName = tableInfo.getTableName();
        Collection<Field> fields = tableInfo.getFields();

        Select.Column[] columns = new Select.Column[fields.size()];
        int columnIndex = 0;
        for (Field field : fields) {
            String columnName = tableInfo.getColumnName(field);
            columns[columnIndex] = new Select.Column(tableName + "." + columnName, columnName);
            columnIndex++;
        }

        return columns;
    }
}