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
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import me.kuehle.carreport.R;
import me.kuehle.carreport.gui.util.FormFieldNotEmptyValidator;
import me.kuehle.carreport.gui.util.FormValidator;
import me.kuehle.carreport.util.WebDavClient;
import me.kuehle.carreport.util.sync.provider.WebDavSyncProvider;

public class SetupWebDavSyncDialogActivity extends Activity {
    private EditText mEdtUrl;
    private EditText mEdtUserName;
    private EditText mEdtPassword;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_webdav_sync);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mEdtUrl = (EditText) findViewById(R.id.edt_url);
        mEdtUserName = (EditText) findViewById(R.id.edt_user_name);
        mEdtPassword = (EditText) findViewById(R.id.edt_password);

        findViewById(R.id.btn_ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOkClick();
            }
        });
        findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
    }

    private void onOkClick() {
        FormValidator validator = new FormValidator();
        validator.add(new FormFieldNotEmptyValidator(mEdtUrl));
        validator.add(new FormFieldNotEmptyValidator(mEdtUserName));
        validator.add(new FormFieldNotEmptyValidator(mEdtPassword));

        if (validator.validate()) {
            new AsyncTask<Void, Void, Boolean>() {
                private String url;
                private String userName;
                private String password;

                @Override
                protected void onPreExecute() {
                    url = mEdtUrl.getText().toString();
                    userName = mEdtUserName.getText().toString();
                    password = mEdtPassword.getText().toString();
                }

                @Override
                protected Boolean doInBackground(Void... params) {
                    WebDavClient client = new WebDavClient(url, userName, password);
                    return client.testLogin();
                }

                @Override
                protected void onPostExecute(Boolean dataIsValid) {
                    if (dataIsValid) {
                        Intent data = new Intent();
                        data.putExtra(AccountManager.KEY_ACCOUNT_NAME, mEdtUserName.getText().toString());
                        data.putExtra(AccountManager.KEY_PASSWORD, mEdtPassword.getText().toString());
                        data.putExtra(WebDavSyncProvider.KEY_WEB_DAV_URL, mEdtUrl.getText().toString());
                        setResult(Activity.RESULT_OK, data);
                        finish();
                    } else {
                        mEdtUrl.setError(getString(R.string.validate_error_webdav_login));
                    }
                }
            }.execute();
        }
    }
}
