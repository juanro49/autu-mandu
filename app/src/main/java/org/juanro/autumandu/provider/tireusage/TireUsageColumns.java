/*
 * Copyright 2025 Juanro49
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
package org.juanro.autumandu.provider.tireusage;

import android.net.Uri;
import android.provider.BaseColumns;

import org.juanro.autumandu.provider.DataProvider;
import org.juanro.autumandu.provider.tirelist.TireListColumns;

/**
 * A station.
 */
@Deprecated
public class TireUsageColumns implements BaseColumns {
    public static final String TABLE_NAME = "tire_usage";
    public static final Uri CONTENT_URI = Uri.parse(DataProvider.CONTENT_URI_BASE + "/" + TABLE_NAME);

    /**
     * Primary key.
     */
    public static final String _ID = BaseColumns._ID;

    public static final String DISTANCE_MOUNT = "distance_mount";
    public static final String DATE_MOUNT = "date_mount";
    public static final String DISTANCE_UMOUNT = "distance_umount";
    public static final String DATE_UMOUNT = "date_umount";
    public static final String TIRE_ID = "tire_id";

    public static final String DEFAULT_ORDER = TABLE_NAME + "." +_ID;

    // @formatter:off
    public static final String[] ALL_COLUMNS = new String[] {
        _ID,
        DISTANCE_MOUNT,
        DATE_MOUNT,
        DISTANCE_UMOUNT,
        DATE_UMOUNT,
        TIRE_ID
    };
    // @formatter:on

    public static boolean hasColumns(String[] projection) {
        if (projection == null) return true;
        for (String c : projection) {
            if (c.equals(_ID) || c.contains("." + _ID)) return true;
        }
        return false;
    }

    public static final String PREFIX_TIRE_LIST = TABLE_NAME + "__" + TireListColumns.TABLE_NAME;
}
