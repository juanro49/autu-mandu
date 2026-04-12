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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.juanro.autumandu.Application;
import org.juanro.autumandu.R;
import org.juanro.autumandu.util.sync.AbstractSyncProvider;
import org.juanro.autumandu.util.sync.AuthenticationFinishedListener;
import org.juanro.autumandu.util.sync.Authenticator;
import org.juanro.autumandu.util.sync.SyncManager;
import org.juanro.autumandu.util.sync.SyncProviders;
import org.juanro.autumandu.util.sync.provider.WebDavSyncProvider;

/**
 * Modernized activity for adding sync accounts.
 */
public class AuthenticatorAddAccountActivity extends AppCompatActivity implements AuthenticationFinishedListener {
    private RecyclerView mRecyclerView;
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

    private AccountAuthenticatorResponse mAccountAuthenticatorResponse;
    private Bundle mResultBundle;

    private static final String STATE_SELECTED_SYNC_PROVIDER_ID = "selected_sync_provider_id";
    private static final String STATE_AUTHENTICATED_ACCOUNT = "authenticated_account";
    private static final String STATE_AUTHENTICATED_ACCOUNT_PASSWORD = "authenticated_account_password";
    private static final String STATE_AUTHENTICATED_ACCOUNT_AUTH_TOKEN = "authenticated_account_auth_token";
    private static final String STATE_AUTHENTICATED_ACCOUNT_SETTINGS = "authenticated_account_settings";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> mSyncSetupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (mAccountAuthenticatorResponse != null) {
                    mAccountAuthenticatorResponse.onRequestContinued();
                }

                if (mSelectedSyncProvider instanceof WebDavSyncProvider) {
                    WebDavSyncProvider.handleAuthenticationResult(this, result.getResultCode(), result.getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticator_add_account);

        // Manual setup for AccountAuthenticatorResponse as we no longer extend AccountAuthenticatorActivity
        mAccountAuthenticatorResponse = getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }

        mRecyclerView = findViewById(R.id.sync_provider_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mRecyclerView.setAdapter(new SyncProviderAdapter());

        mProgressView = findViewById(android.R.id.progress);
        mProgressView.setVisibility(View.GONE);
        mProgressMessage = findViewById(R.id.progress_message);

        mFirstSyncView = findViewById(R.id.first_sync);
        mFirstSyncView.setVisibility(View.GONE);
        findViewById(R.id.first_sync_btn_download).setOnClickListener(v -> performFirstSync(true));
        findViewById(R.id.first_sync_btn_upload).setOnClickListener(v -> performFirstSync(false));

        mFirstSyncErrorView = findViewById(R.id.first_sync_error);
        mFirstSyncErrorView.setVisibility(View.GONE);
        mFirstSyncErrorMessage = findViewById(R.id.first_sync_error_message);
        findViewById(R.id.first_sync_error_btn_ok).setOnClickListener(v -> finish());

        if (savedInstanceState != null) {
            long providerId = savedInstanceState.getLong(STATE_SELECTED_SYNC_PROVIDER_ID, -1);
            if (providerId != -1) {
                for (AbstractSyncProvider provider : SyncProviders.getSyncProviders(this)) {
                    if (provider.getId() == providerId) {
                        mSelectedSyncProvider = provider;
                        break;
                    }
                }
            }

            mAuthenticatedAccount = savedInstanceState.getParcelable(STATE_AUTHENTICATED_ACCOUNT);
            mAuthenticatedAccountPassword = savedInstanceState.getString(STATE_AUTHENTICATED_ACCOUNT_PASSWORD);
            mAuthenticatedAccountAuthToken = savedInstanceState.getString(STATE_AUTHENTICATED_ACCOUNT_AUTH_TOKEN);
            String settingsStr = savedInstanceState.getString(STATE_AUTHENTICATED_ACCOUNT_SETTINGS);
            if (settingsStr != null) {
                try {
                    mAuthenticatedAccountSettings = new JSONObject(settingsStr);
                } catch (Exception ignored) {
                }
            }

            if (mAuthenticatedAccount != null) {
                mRecyclerView.setVisibility(View.GONE);
                startFirstSync();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedSyncProvider != null) {
            outState.putLong(STATE_SELECTED_SYNC_PROVIDER_ID, mSelectedSyncProvider.getId());
        }
        outState.putParcelable(STATE_AUTHENTICATED_ACCOUNT, mAuthenticatedAccount);
        outState.putString(STATE_AUTHENTICATED_ACCOUNT_PASSWORD, mAuthenticatedAccountPassword);
        outState.putString(STATE_AUTHENTICATED_ACCOUNT_AUTH_TOKEN, mAuthenticatedAccountAuthToken);
        if (mAuthenticatedAccountSettings != null) {
            outState.putString(STATE_AUTHENTICATED_ACCOUNT_SETTINGS, mAuthenticatedAccountSettings.toString());
        }
    }

    public final void setAccountAuthenticatorResult(Bundle result) {
        mResultBundle = result;
    }

    @Override
    public void finish() {
        if (mAccountAuthenticatorResponse != null) {
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            } else {
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED, "canceled");
            }
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }

    /**
     * Callback called by the sync provider when the authentication process has finished.
     */
    public void onAuthenticationFinished(Account account, String password, String authToken, JSONObject settings) {
        mAuthenticatedAccount = account;
        mAuthenticatedAccountPassword = password;
        mAuthenticatedAccountAuthToken = authToken;
        mAuthenticatedAccountSettings = settings;

        if (mAuthenticatedAccount != null) {
            startFirstSync();
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mProgressView.setVisibility(View.GONE);
        }
    }

    private void startFirstSync() {
        mProgressMessage.setText(R.string.alert_sync_performing_first_sync);
        mExecutor.execute(() -> {
            try {
                mSelectedSyncProvider.setup(mAuthenticatedAccount,
                        mAuthenticatedAccountPassword,
                        mAuthenticatedAccountAuthToken,
                        mAuthenticatedAccountSettings);
                final String remoteRev = mSelectedSyncProvider.getRemoteFileRev();
                final boolean remoteDataAvailable = remoteRev != null;
                runOnUiThread(() -> {
                    if (remoteDataAvailable) {
                        mProgressView.setVisibility(View.GONE);
                        mFirstSyncView.setVisibility(View.VISIBLE);
                    } else {
                        performFirstSync(false);
                    }
                });
            } catch (final Exception e) {
                runOnUiThread(() -> handleFirstSyncError(e));
            }
        });
    }

    private void performFirstSync(final boolean download) {
        mProgressMessage.setText(R.string.alert_sync_performing_first_sync);
        mFirstSyncView.setVisibility(View.GONE);
        mProgressView.setVisibility(View.VISIBLE);

        mExecutor.execute(() -> {
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
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;

                    SyncManager.schedulePeriodicSync(AuthenticatorAddAccountActivity.this);

                    // Add account to AccountManager
                    AccountManager accountManager = AccountManager.get(AuthenticatorAddAccountActivity.this);
                    boolean accountAdded = accountManager.addAccountExplicitly(mAuthenticatedAccount, mAuthenticatedAccountPassword, null);
                    if (accountAdded) {
                        // Add provider and settings as user data
                        accountManager.setUserData(mAuthenticatedAccount, Authenticator.KEY_SYNC_PROVIDER, String.valueOf(mSelectedSyncProvider.getId()));
                        if (mAuthenticatedAccountSettings != null) {
                            accountManager.setUserData(mAuthenticatedAccount, Authenticator.KEY_SYNC_PROVIDER_SETTINGS, mAuthenticatedAccountSettings.toString());
                        }
                    }

                    // Create result bundle for AccountManager
                    Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_ACCOUNT_NAME, mAuthenticatedAccount.name);
                    result.putString(AccountManager.KEY_ACCOUNT_TYPE, mAuthenticatedAccount.type);
                    result.putString(AccountManager.KEY_PASSWORD, mAuthenticatedAccountPassword);
                    if (mAuthenticatedAccountAuthToken != null) {
                        result.putString(AccountManager.KEY_AUTHTOKEN, mAuthenticatedAccountAuthToken);
                        accountManager.setAuthToken(mAuthenticatedAccount, Authenticator.AUTH_TOKEN_TYPE, mAuthenticatedAccountAuthToken);
                    }

                    setAccountAuthenticatorResult(result);
                    setResult(Activity.RESULT_OK);

                    if (download) {
                        // Restart the app to ensure all ViewModels and LiveDatas are recreated with the new database.
                        Intent intent = new Intent(AuthenticatorAddAccountActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }

                    finish();
                });
            } catch (final Exception e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        handleFirstSyncError(e);
                    }
                });
            }
        });
    }

    private void handleFirstSyncError(Exception e) {
        if (mAuthenticatedAccount != null) {
            AccountManager accountManager = AccountManager.get(this);
            accountManager.removeAccount(mAuthenticatedAccount, this, future -> {
                try {
                    future.getResult();
                } catch (OperationCanceledException | IOException | AuthenticatorException ignored) {
                }
            }, null);
        }

        setAccountAuthenticatorResult(null);
        setResult(Activity.RESULT_CANCELED, null);

        mProgressView.setVisibility(View.GONE);
        mFirstSyncErrorView.setVisibility(View.VISIBLE);
        mFirstSyncErrorMessage.setText(e.getMessage());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdown();
    }

    private class SyncProviderAdapter extends RecyclerView.Adapter<SyncProviderAdapter.ViewHolder> {
        private final AbstractSyncProvider[] mProviders;

        public SyncProviderAdapter() {
            mProviders = SyncProviders.getSyncProviders(AuthenticatorAddAccountActivity.this);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AbstractSyncProvider provider = mProviders[position];
            holder.text1.setText(provider.getName());
            holder.text1.setCompoundDrawablesWithIntrinsicBounds(provider.getIcon(), 0, 0, 0);
            holder.text1.setCompoundDrawablePadding(16);
            holder.itemView.setOnClickListener(v -> {
                mSelectedSyncProvider = provider;
                try {
                    mSelectedSyncProvider.setup(null, null, null, null);
                    mSelectedSyncProvider.startAuthentication(AuthenticatorAddAccountActivity.this, mSyncSetupLauncher);
                    mRecyclerView.setVisibility(View.GONE);
                    mProgressView.setVisibility(View.VISIBLE);
                } catch (Exception e) {
                    handleFirstSyncError(e);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mProviders.length;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView text1;

            ViewHolder(View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
