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

package me.kuehle.carreport.db.serializer;

import me.kuehle.carreport.util.Recurrence;
import me.kuehle.carreport.util.RecurrenceInterval;

import com.activeandroid.serializer.TypeSerializer;

public class RecurrenceSerializer extends TypeSerializer {
	@Override
	public Class<?> getDeserializedType() {
		return Recurrence.class;
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

		Recurrence recurrence = (Recurrence) data;

		String interval = recurrence.getInterval().toString();
		int multiplier = recurrence.getMultiplier();

		return interval + " " + multiplier;
	}

	@Override
	public Recurrence deserialize(Object data) {
		if (data == null) {
			return null;
		}

		String[] values = ((String) data).split(" ");

		String interval = values[0];
		int multiplier = Integer.parseInt(values[1]);

		return new Recurrence(RecurrenceInterval.valueOf(interval), multiplier);
	}
}
