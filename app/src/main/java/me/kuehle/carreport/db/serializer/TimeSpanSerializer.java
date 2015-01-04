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
package me.kuehle.carreport.db.serializer;

import com.activeandroid.serializer.TypeSerializer;

import me.kuehle.carreport.util.TimeSpan;
import me.kuehle.carreport.util.TimeSpanUnit;

public class TimeSpanSerializer extends TypeSerializer {
    @Override
    public Class<?> getDeserializedType() {
        return TimeSpan.class;
    }

    @Override
    public Class<?> getSerializedType() {
        return String.class;
    }

    @Override
    public String serialize(Object data) {
        if (data == null) {
            return null;
        }

        TimeSpan timeSpan = (TimeSpan) data;

        String unit = timeSpan.getUnit().toString();
        int count = timeSpan.getCount();

        return unit + " " + count;
    }

    @Override
    public TimeSpan deserialize(Object data) {
        if (data == null) {
            return null;
        }

        try {
            String[] values = ((String) data).split(" ");

            String unit = values[0];
            int count = Integer.parseInt(values[1]);

            return new TimeSpan(TimeSpanUnit.valueOf(unit), count);
        } catch (Exception e) {
            return null;
        }
    }
}
