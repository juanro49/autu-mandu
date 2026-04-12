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
package org.juanro.autumandu.gui.dialog;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.util.FormFieldNotEmptyValidator;
import org.juanro.autumandu.gui.util.FormValidator;
import org.juanro.autumandu.util.sync.provider.WebDavSyncProvider;
import org.juanro.autumandu.util.webdav.CertificateHelper;
import org.juanro.autumandu.util.webdav.HttpException;
import org.juanro.autumandu.util.webdav.InvalidCertificateException;
import org.juanro.autumandu.util.webdav.UntrustedCertificateException;
import org.juanro.autumandu.util.webdav.WebDavClient;

public class SetupWebDavSyncDialogActivity extends AppCompatActivity {
    enum TestLoginStatus {
        OK,
        UNTRUSTED_CERTIFICATE,
        INVALID_CERTIFICATE,
        FAILED
    }

    private EditText mEdtUrl;
    private EditText mEdtUserName;
    private EditText mEdtPassword;
    private TextView mTxtTrustCertificateDescription;
    private TextView mTxtTrustCertificate;
    private CheckBox mChkTrustCertificate;
    private X509Certificate mTrustCertificate;
    private Button mBtnOk;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_webdav_sync);
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        mEdtUrl = findViewById(R.id.edt_url);
        mEdtUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                resetCertificateView();
            }
        });

        mEdtUserName = findViewById(R.id.edt_user_name);
        mEdtPassword = findViewById(R.id.edt_password);
        mTxtTrustCertificateDescription = findViewById(R.id.txt_trust_certificate_description);
        mTxtTrustCertificate = findViewById(R.id.txt_trust_certificate);
        mChkTrustCertificate = findViewById(R.id.chk_trust_certificate);

        resetCertificateView();

        mBtnOk = findViewById(R.id.btn_ok);
        mBtnOk.setOnClickListener(v -> onOkClick());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    private void resetCertificateView() {
        mTxtTrustCertificateDescription.setVisibility(View.GONE);
        mTxtTrustCertificate.setVisibility(View.GONE);
        mChkTrustCertificate.setChecked(false);
        mChkTrustCertificate.setVisibility(View.GONE);
        mTrustCertificate = null;
    }

    private void onOkClick() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldNotEmptyValidator(mEdtUrl));
        validator.add(new FormFieldNotEmptyValidator(mEdtUserName));
        validator.add(new FormFieldNotEmptyValidator(mEdtPassword));

        if (validator.validate()) {
            final String url = mEdtUrl.getText().toString();
            final String userName = mEdtUserName.getText().toString();
            final String password = mEdtPassword.getText().toString();
            final X509Certificate trustedCertificate = mChkTrustCertificate.isChecked() ? mTrustCertificate : null;

            mBtnOk.setEnabled(false);

            mExecutor.execute(() -> {
                TestLoginStatus status;
                HttpException httpException = null;
                X509Certificate newUntrustedCert = null;

                try {
                    WebDavClient client = new WebDavClient(url, userName, password, trustedCertificate);
                    client.testLogin();
                    status = TestLoginStatus.OK;
                } catch (UntrustedCertificateException e) {
                    newUntrustedCert = e.getCertificate();
                    status = TestLoginStatus.UNTRUSTED_CERTIFICATE;
                } catch (InvalidCertificateException e) {
                    status = TestLoginStatus.INVALID_CERTIFICATE;
                } catch (HttpException e) {
                    httpException = e;
                    status = TestLoginStatus.FAILED;
                }

                final TestLoginStatus finalStatus = status;
                final HttpException finalException = httpException;
                final X509Certificate finalCert = newUntrustedCert;

                mHandler.post(() -> handleLoginResult(finalStatus, finalException, finalCert, url, userName, password));
            });
        }
    }

    private void handleLoginResult(TestLoginStatus status, HttpException exception, X509Certificate untrustedCert,
                                  String url, String userName, String password) {
        mBtnOk.setEnabled(true);

        switch (status) {
            case OK:
                Intent data = new Intent();
                data.putExtra(AccountManager.KEY_ACCOUNT_NAME, userName);
                data.putExtra(AccountManager.KEY_PASSWORD, password);
                data.putExtra(WebDavSyncProvider.KEY_WEB_DAV_URL, url);
                if (mTrustCertificate != null && mChkTrustCertificate.isChecked()) {
                    data.putExtra(WebDavSyncProvider.KEY_WEB_DAV_CERTIFICATE, mTrustCertificate);
                }
                setResult(Activity.RESULT_OK, data);
                finish();
                break;

            case UNTRUSTED_CERTIFICATE:
                mTrustCertificate = untrustedCert;
                mTxtTrustCertificateDescription.setVisibility(View.VISIBLE);
                mTxtTrustCertificate.setText(CertificateHelper.getShortDescription(mTrustCertificate, this));
                mTxtTrustCertificate.setVisibility(View.VISIBLE);
                mChkTrustCertificate.setVisibility(View.VISIBLE);
                break;

            case INVALID_CERTIFICATE:
                mChkTrustCertificate.setError(getString(R.string.validate_error_webdav_invalid_certificate));
                break;

            case FAILED:
                mEdtUrl.setError(exception != null ? exception.getMessage() : getString(R.string.alert_error_title));
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }
}
