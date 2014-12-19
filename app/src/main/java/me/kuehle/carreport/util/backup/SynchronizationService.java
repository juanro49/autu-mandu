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

package me.kuehle.carreport.util.backup;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class SynchronizationService extends IntentService {
	public static final String EXTRA_OPTION = "me.kuehle.carreport.util.backup.SynchronizationService.OPTION";
	public static final String EXTRA_STATUS = "me.kuehle.carreport.util.backup.SynchronizationService.STATUS";
	public static final String EXTRA_RESULT = "me.kuehle.carreport.util.backup.SynchronizationService.RESULT";
	public static final String BROADCAST_ACTION = "me.kuehle.carreport.util.backup.SynchronizationService.BROADCAST";

	public static final int STATUS_STARTED = 0;
	public static final int STATUS_FINISHED = 1;

	public SynchronizationService() {
		super("Synchronization Service");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		int option = intent.getIntExtra(EXTRA_OPTION, AbstractSynchronizationProvider.SYNC_NORMAL);

		AbstractSynchronizationProvider provider = AbstractSynchronizationProvider
				.getCurrent(getApplicationContext());

		sendSynchronizationStatus(STATUS_STARTED);
		boolean result = provider.onSynchronize(option);
		sendSynchronizationStatus(STATUS_FINISHED, result);
	}

	private void sendSynchronizationStatus(int status) {
		Intent intent = new Intent(BROADCAST_ACTION);
		intent.putExtra(EXTRA_STATUS, status);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	private void sendSynchronizationStatus(int status, boolean result) {
		Intent intent = new Intent(BROADCAST_ACTION);
		intent.putExtra(EXTRA_STATUS, status);
		intent.putExtra(EXTRA_RESULT, result);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}
}
