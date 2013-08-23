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

package me.kuehle.carreport;

import com.activeandroid.ActiveAndroid;

import me.kuehle.carreport.util.backup.Dropbox;

public class Application extends android.app.Application {
	private static Application instance;

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;

		System.setProperty("org.joda.time.DateTimeZone.Provider",
				"org.joda.time.tz.UTCProvider");
		ActiveAndroid.initialize(this);
		Dropbox.init(this);

		if (new Preferences(this).isSyncOnStart()) {
			Dropbox.getInstance().synchronize();
		}
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		ActiveAndroid.dispose();
	}

	public static void reinitializeDatabase() {
		if (instance != null) {
			ActiveAndroid.dispose();
			ActiveAndroid.initialize(instance);
		}
	}

	public static void dataChanged() {
		if (instance != null) {
			if (new Preferences(instance).isSyncOnChange()) {
				Dropbox.getInstance().synchronize();
			}
		}
	}
}
