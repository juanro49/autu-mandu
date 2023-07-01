<<<<<<< a7a91dd4ab86f0bd1a4879c4fd2c55350602a99c:app/src/main/java/me/kuehle/carreport/gui/dialog/SetupWebDavSyncDialogActivity.java
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
package me.kuehle.carreport.gui.dialog;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.security.cert.X509Certificate;

import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.util.FormFieldNotEmptyValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.util.sync.provider.WebDavSyncProvider;
import me.kuehle.carreport.util.webdav.CertificateHelper;
import me.kuehle.carreport.util.webdav.HttpException;
import me.kuehle.carreport.util.webdav.InvalidCertificateException;
import me.kuehle.carreport.util.webdav.UntrustedCertificateException;
import me.kuehle.carreport.util.webdav.WebDavClient;

public class SetupWebDavSyncDialogActivity extends Activity {
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_webdav_sync);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mEdtUrl = findViewById(R.id.edt_url);
        mEdtUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mTxtTrustCertificateDescription.setVisibility(View.GONE);
                mTxtTrustCertificate.setVisibility(View.GONE);
                mChkTrustCertificate.setChecked(false);
                mChkTrustCertificate.setVisibility(View.GONE);
            }
        });
        mEdtUserName = findViewById(R.id.edt_user_name);
        mEdtPassword = findViewById(R.id.edt_password);
        mTxtTrustCertificateDescription = findViewById(R.id.txt_trust_certificate_description);
        mTxtTrustCertificate = findViewById(R.id.txt_trust_certificate);
        mChkTrustCertificate = findViewById(R.id.chk_trust_certificate);

        mTxtTrustCertificateDescription.setVisibility(View.GONE);
        mTxtTrustCertificate.setVisibility(View.GONE);
        mChkTrustCertificate.setVisibility(View.GONE);

        mBtnOk = findViewById(R.id.btn_ok);
        mBtnOk.setOnClickListener(v -> onOkClick());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    private void onOkClick() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldNotEmptyValidator(mEdtUrl));
        validator.add(new FormFieldNotEmptyValidator(mEdtUserName));
        validator.add(new FormFieldNotEmptyValidator(mEdtPassword));

        if (validator.validate()) {
            new AsyncTask<Void, Void, TestLoginStatus>() {
                private String url;
                private String userName;
                private String password;
                private X509Certificate trustedCertificate;
                private HttpException exception;

                @Override
                protected void onPreExecute() {
                    url = mEdtUrl.getText().toString();
                    userName = mEdtUserName.getText().toString();
                    password = mEdtPassword.getText().toString();
                    trustedCertificate = mChkTrustCertificate.isChecked() ? mTrustCertificate : null;
                    exception = null;

                    mBtnOk.setEnabled(false);
                }

                @Override
                protected TestLoginStatus doInBackground(Void... params) {
                    try {
                        WebDavClient client = new WebDavClient(url, userName, password, trustedCertificate);
                        client.testLogin();
                        return TestLoginStatus.OK;
                    } catch (UntrustedCertificateException e) {
                        mTrustCertificate = e.getCertificate();
                        return TestLoginStatus.UNTRUSTED_CERTIFICATE;
                    } catch (InvalidCertificateException e) {
                        return TestLoginStatus.INVALID_CERTIFICATE;
                    } catch (HttpException e) {
                        exception = e;
                        return TestLoginStatus.FAILED;
                    }
                }

                @Override
                protected void onPostExecute(TestLoginStatus status) {
                    if (status == TestLoginStatus.OK) {
                        Intent data = new Intent();
                        data.putExtra(AccountManager.KEY_ACCOUNT_NAME, mEdtUserName.getText().toString());
                        data.putExtra(AccountManager.KEY_PASSWORD, mEdtPassword.getText().toString());
                        data.putExtra(WebDavSyncProvider.KEY_WEB_DAV_URL, mEdtUrl.getText().toString());
                        if (mTrustCertificate != null && mChkTrustCertificate.isChecked()) {
                            data.putExtra(WebDavSyncProvider.KEY_WEB_DAV_CERTIFICATE, mTrustCertificate);
                        }

                        setResult(Activity.RESULT_OK, data);
                        finish();
                    } else if (status == TestLoginStatus.UNTRUSTED_CERTIFICATE) {
                        mTxtTrustCertificateDescription.setVisibility(View.VISIBLE);
                        mTxtTrustCertificate.setText(CertificateHelper.getShortDescription(mTrustCertificate, SetupWebDavSyncDialogActivity.this));
                        mTxtTrustCertificate.setVisibility(View.VISIBLE);
                        mChkTrustCertificate.setVisibility(View.VISIBLE);
                    } else if (status == TestLoginStatus.INVALID_CERTIFICATE) {
                        mChkTrustCertificate.setError(getString(R.string.validate_error_webdav_invalid_certificate));
                    } else {
                        mEdtUrl.setError(exception.getMessage());
                    }

                    mBtnOk.setEnabled(true);
                }
            }.execute();
        }
    }
}
=======
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.security.cert.X509Certificate;

import org.juanro.autumandu.R;
import org.juanro.autumandu.gui.util.FormFieldNotEmptyValidator;
import org.juanro.autumandu.gui.util.FormValidator;
import org.juanro.autumandu.util.sync.provider.WebDavSyncProvider;
import org.juanro.autumandu.util.webdav.CertificateHelper;
import org.juanro.autumandu.util.webdav.HttpException;
import org.juanro.autumandu.util.webdav.InvalidCertificateException;
import org.juanro.autumandu.util.webdav.UntrustedCertificateException;
import org.juanro.autumandu.util.webdav.WebDavClient;

public class SetupWebDavSyncDialogActivity extends Activity {
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_webdav_sync);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mEdtUrl = findViewById(R.id.edt_url);
        mEdtUrl.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mTxtTrustCertificateDescription.setVisibility(View.GONE);
                mTxtTrustCertificate.setVisibility(View.GONE);
                mChkTrustCertificate.setChecked(false);
                mChkTrustCertificate.setVisibility(View.GONE);
            }
        });
        mEdtUserName = findViewById(R.id.edt_user_name);
        mEdtPassword = findViewById(R.id.edt_password);
        mTxtTrustCertificateDescription = findViewById(R.id.txt_trust_certificate_description);
        mTxtTrustCertificate = findViewById(R.id.txt_trust_certificate);
        mChkTrustCertificate = findViewById(R.id.chk_trust_certificate);

        mTxtTrustCertificateDescription.setVisibility(View.GONE);
        mTxtTrustCertificate.setVisibility(View.GONE);
        mChkTrustCertificate.setVisibility(View.GONE);

        mBtnOk = findViewById(R.id.btn_ok);
        mBtnOk.setOnClickListener(v -> onOkClick());
        findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });
    }

    private void onOkClick() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldNotEmptyValidator(mEdtUrl));
        validator.add(new FormFieldNotEmptyValidator(mEdtUserName));
        validator.add(new FormFieldNotEmptyValidator(mEdtPassword));

        if (validator.validate()) {
            new AsyncTask<Void, Void, TestLoginStatus>() {
                private String url;
                private String userName;
                private String password;
                private X509Certificate trustedCertificate;
                private HttpException exception;

                @Override
                protected void onPreExecute() {
                    url = mEdtUrl.getText().toString();
                    userName = mEdtUserName.getText().toString();
                    password = mEdtPassword.getText().toString();
                    trustedCertificate = mChkTrustCertificate.isChecked() ? mTrustCertificate : null;
                    exception = null;

                    mBtnOk.setEnabled(false);
                }

                @Override
                protected TestLoginStatus doInBackground(Void... params) {
                    try {
                        WebDavClient client = new WebDavClient(url, userName, password, trustedCertificate);
                        client.testLogin();
                        return TestLoginStatus.OK;
                    } catch (UntrustedCertificateException e) {
                        mTrustCertificate = e.getCertificate();
                        return TestLoginStatus.UNTRUSTED_CERTIFICATE;
                    } catch (InvalidCertificateException e) {
                        return TestLoginStatus.INVALID_CERTIFICATE;
                    } catch (HttpException e) {
                        exception = e;
                        return TestLoginStatus.FAILED;
                    }
                }

                @Override
                protected void onPostExecute(TestLoginStatus status) {
                    if (status == TestLoginStatus.OK) {
                        Intent data = new Intent();
                        data.putExtra(AccountManager.KEY_ACCOUNT_NAME, mEdtUserName.getText().toString());
                        data.putExtra(AccountManager.KEY_PASSWORD, mEdtPassword.getText().toString());
                        data.putExtra(WebDavSyncProvider.KEY_WEB_DAV_URL, mEdtUrl.getText().toString());
                        if (mTrustCertificate != null && mChkTrustCertificate.isChecked()) {
                            data.putExtra(WebDavSyncProvider.KEY_WEB_DAV_CERTIFICATE, mTrustCertificate);
                        }

                        setResult(Activity.RESULT_OK, data);
                        finish();
                    } else if (status == TestLoginStatus.UNTRUSTED_CERTIFICATE) {
                        CertificateHelper certificate = new CertificateHelper();
                        mTxtTrustCertificateDescription.setVisibility(View.VISIBLE);
                        mTxtTrustCertificate.setText(certificate.getShortDescription(mTrustCertificate, SetupWebDavSyncDialogActivity.this));
                        mTxtTrustCertificate.setVisibility(View.VISIBLE);
                        mChkTrustCertificate.setVisibility(View.VISIBLE);
                    } else if (status == TestLoginStatus.INVALID_CERTIFICATE) {
                        mChkTrustCertificate.setError(getString(R.string.validate_error_webdav_invalid_certificate));
                    } else {
                        mEdtUrl.setError(exception.getMessage());
                    }

                    mBtnOk.setEnabled(true);
                }
            }.execute();
        }
    }
}
>>>>>>> Refactorización y mejoras en migración versiones BBDD:app/src/main/java/org/juanro/autumandu/gui/dialog/SetupWebDavSyncDialogActivity.java
