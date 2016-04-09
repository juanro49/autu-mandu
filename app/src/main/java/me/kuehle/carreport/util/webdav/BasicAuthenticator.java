/*
 * Copyright 2016 Jan Kühle
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
package me.kuehle.carreport.util.webdav;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Challenge;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class BasicAuthenticator implements Authenticator {
    private String mAuthHeader;

    public BasicAuthenticator(String userName, String password) {
        mAuthHeader = Credentials.basic(userName, password);
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (responseCount(response) >= 3) {
            return null; // If we've failed 3 times, give up.
        }

        for (Challenge challenge : response.challenges()) {
            if (challenge.scheme().equals("Basic")) {
                return response.request().newBuilder()
                        .header("Authorization", mAuthHeader)
                        .build();
            }
        }

        // No supported authentication scheme. Give up.
        return null;
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }

        return result;
    }
}

