/*
 * Copyright 2026 Jan Kühle
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
package org.juanro.autumandu.util.webdav;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Modernized exception for WebDAV HTTP errors.
 * Compatible with Android API 25+.
 */
public class HttpException extends Exception {
    private final int code;

    public HttpException(@NonNull Response response) {
        this(response.request(), response.code(), response.message(), null);
    }

    public HttpException(@NonNull Request request, @NonNull IOException exception) {
        this(request, -1, String.format(Locale.ROOT, "Connection failed: %s", exception.getMessage()), exception);
    }

    public HttpException(@NonNull Request request, @NonNull Exception responseParseException) {
        this(request, -2, String.format(Locale.ROOT, "Invalid response: %s", responseParseException.getMessage()), responseParseException);
    }

    public HttpException(@NonNull Request request, int code, @NonNull String message, @Nullable Exception innerException) {
        super(String.format(Locale.ROOT, "Error connecting to the WebDAV server: %d %s. Was trying to execute request: %s %s",
                        code, message, request.method(), request.url()),
                innerException);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public boolean isNetworkIssue() {
        return code == -1;
    }

    public boolean isUnauthorized() {
        return code == 401;
    }

    public boolean isNotFound() {
        return code == 404;
    }
}
