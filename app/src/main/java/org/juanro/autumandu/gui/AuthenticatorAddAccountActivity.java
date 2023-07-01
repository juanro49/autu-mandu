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

package org.juanro.autumandu.gui;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.IOException;

import org.juanro.autumandu.Application;
import org.juanro.autumandu.R;
import org.juanro.autumandu.provider.DataProvider;
import org.juanro.autumandu.util.sync.AbstractSyncProvider;
import org.juanro.autumandu.util.sync.Authenticator;
import org.juanro.autumandu.util.sync.SyncProviders;

public class AuthenticatorAddAccountActivity extends AccountAuthenticatorActivity implements
        AdapterView.OnItemClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private ListView mListView;
    private SyncProviderAdapter mSyncProviderAdapter;
    private View mProgressView;
    private TextView mProgressMessage;
    private View mFirstSyncView;
    private View mFirstSyncErrorView;
    private TextView mFirstSyncErrorMessage;

    private AbstractSyncProvider mSelectedSyncProvider;
    private Account mAuthenticatedAccount;
    private String mAuthenticatedAccountPassword;
    private String mAuthenticatedAccountAuthToken;
    private JSONObject mAuthenticatedAccountSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator_add_account);

        mSyncProviderAdapter = new SyncProviderAdapter();

        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setAdapter(mSyncProviderAdapter);
        mListView.setOnItemClickListener(this);

        mProgressView = findViewById(android.R.id.progress);
        mProgressView.setVisibility(View.GONE);
        mProgressMessage = (TextView) findViewById(R.id.progress_message);

        mFirstSyncView = findViewById(R.id.first_sync);
        mFirstSyncView.setVisibility(View.GONE);
        findViewById(R.id.first_sync_btn_download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFirstSync(true);
            }
        });
        findViewById(R.id.first_sync_btn_upload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFirstSync(false);
            }
        });

        mFirstSyncErrorView = findViewById(R.id.first_sync_error);
        mFirstSyncErrorView.setVisibility(View.GONE);
        mFirstSyncErrorMessage = (TextView) findViewById(R.id.first_sync_error_message);
        findViewById(R.id.first_sync_error_btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mSelectedSyncProvider = mSyncProviderAdapter.getItem(position);
        try {
            mSelectedSyncProvider.setup(null, null, null, null);
            mSelectedSyncProvider.startAuthentication(this);

            mListView.setVisibility(View.GONE);
            mProgressView.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            handleFirstSyncError(e);
        }
    }

    public void onAuthenticationResult(String accountName, String password, String authToken, JSONObject settings) {
        if (accountName != null) {
            Intent data = new Intent();
            data.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
            data.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Authenticator.ACCOUNT_TYPE);
            data.putExtra(AccountManager.KEY_PASSWORD, password);
            data.putExtra(AccountManager.KEY_AUTHTOKEN, authToken);

            mAuthenticatedAccount = new Account(accountName, Authenticator.ACCOUNT_TYPE);
            mAuthenticatedAccountPassword = password;
            mAuthenticatedAccountAuthToken = authToken;
            mAuthenticatedAccountSettings = settings;
            AccountManager accountManager = AccountManager.get(this);
            accountManager.addAccountExplicitly(mAuthenticatedAccount, password, null);
            accountManager.setAuthToken(mAuthenticatedAccount, Authenticator.AUTH_TOKEN_TYPE, authToken);
            accountManager.setUserData(mAuthenticatedAccount, Authenticator.KEY_SYNC_PROVIDER,
                    String.valueOf(mSelectedSyncProvider.getId()));
            SyncProviders.setSyncProviderSettings(mAuthenticatedAccount, settings);

            setAccountAuthenticatorResult(data.getExtras());
            setResult(Activity.RESULT_OK, data);

            startFirstSync();
        } else {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSelectedSyncProvider != null) {
            mSelectedSyncProvider.continueAuthentication(this, 0, 0, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mSelectedSyncProvider != null) {
            mSelectedSyncProvider.continueAuthentication(this, requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (mSelectedSyncProvider != null) {
            int resultCode = Activity.RESULT_OK;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    resultCode = Activity.RESULT_CANCELED;
                }
            }

            mSelectedSyncProvider.continueAuthentication(this, requestCode, resultCode, null);
        }
    }

    private void startFirstSync() {
        mProgressMessage.setText(R.string.alert_sync_performing_first_sync);
        new AsyncTask<Void, Void, Boolean>() {
            private Exception mException;

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    mSelectedSyncProvider.setup(mAuthenticatedAccount,
                            mAuthenticatedAccountPassword,
                            mAuthenticatedAccountAuthToken,
                            mAuthenticatedAccountSettings);
                    String remoteRev = mSelectedSyncProvider.getRemoteFileRev();
                    return remoteRev != null;
                } catch (Exception e) {
                    mException = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Boolean remoteDataAvailable) {
                if (remoteDataAvailable == null) {
                    handleFirstSyncError(mException);
                } else if (remoteDataAvailable) {
                    mProgressView.setVisibility(View.GONE);
                    mFirstSyncView.setVisibility(View.VISIBLE);
                } else {
                    performFirstSync(false);
                }
            }
        }.execute();
    }

    private void performFirstSync(final boolean download) {
        mProgressMessage.setText(R.string.alert_sync_performing_first_sync);
        mFirstSyncView.setVisibility(View.GONE);
        mProgressView.setVisibility(View.VISIBLE);

        new AsyncTask<Void, Void, Boolean>() {
            private Exception mException;

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    String remoteRev;
                    Application.closeDatabases();
                    if (download) {
                        mSelectedSyncProvider.downloadFile();
                        remoteRev = mSelectedSyncProvider.getRemoteFileRev();
                    } else {
                        remoteRev = mSelectedSyncProvider.uploadFile();
                    }

                    mSelectedSyncProvider.setLocalFileRev(remoteRev);
                    return true;
                } catch (Exception e) {
                    mException = e;
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    // Now we can start to synchronize on every change.
                    ContentResolver.setSyncAutomatically(mAuthenticatedAccount, DataProvider.AUTHORITY, true);
                    finish();
                } else {
                    handleFirstSyncError(mException);
                }
            }
        }.execute();
    }

    private void handleFirstSyncError(Exception e) {
        if (mAuthenticatedAccount != null) {
            AccountManager accountManager = AccountManager.get(this);
            accountManager.removeAccount(mAuthenticatedAccount, new AccountManagerCallback<Boolean>() {
                @Override
                public void run(AccountManagerFuture<Boolean> future) {
                    try {
                        future.getResult();
                    } catch (OperationCanceledException | IOException | AuthenticatorException ignored) {
                    }
                }
            }, null);
        }

        setAccountAuthenticatorResult(null);
        setResult(Activity.RESULT_CANCELED, null);

        mProgressView.setVisibility(View.GONE);
        mFirstSyncErrorView.setVisibility(View.VISIBLE);
        mFirstSyncErrorMessage.setText(e.getMessage());
    }

    private class SyncProviderAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return SyncProviders.getSyncProviders(AuthenticatorAddAccountActivity.this).length;
        }

        @Override
        public AbstractSyncProvider getItem(int position) {
            return SyncProviders.getSyncProviders(AuthenticatorAddAccountActivity.this)[position];
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getId();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AbstractSyncProvider provider = getItem(position);
            if (convertView == null) {
                convertView = AuthenticatorAddAccountActivity.this.getLayoutInflater().inflate(
                        android.R.layout.simple_list_item_1, parent, false);
            }

            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            text1.setText(provider.getName());
            text1.setCompoundDrawablesWithIntrinsicBounds(provider.getIcon(), 0, 0, 0);
            text1.setCompoundDrawablePadding(16);

            return convertView;
        }
    }
}
