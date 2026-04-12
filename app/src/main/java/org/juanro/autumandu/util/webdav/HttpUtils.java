/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package org.juanro.autumandu.util.webdav;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for HTTP protocol handling.
 */
public final class HttpUtils {
    private static final Pattern AUTH_SCHEME_WITH_PARAM = Pattern.compile("^([^ \"]+) +(.*)$");

    private HttpUtils() {
        // Utility class
    }

    public static List<AuthScheme> parseWwwAuthenticate(String[] wwwAuths) {
        List<AuthScheme> schemes = new ArrayList<>();
        for (String wwwAuth : wwwAuths) {
            List<String> tokens = tokenizeWwwAuth(wwwAuth);

            AuthScheme scheme = null;
            for (String s : tokens) {
                s = s.trim();

                Matcher matcher = AUTH_SCHEME_WITH_PARAM.matcher(s);
                if (matcher.matches()) {
                    schemes.add(scheme = new AuthScheme(matcher.group(1)));
                    scheme.addRawParam(matcher.group(2));
                } else if (scheme != null) {
                    scheme.addRawParam(s);
                } else {
                    schemes.add(scheme = new AuthScheme(s));
                }
            }
        }
        return schemes;
    }

    private static List<String> tokenizeWwwAuth(String wwwAuth) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();

        boolean inQuotes = false;
        int len = wwwAuth.length();
        for (int i = 0; i < len; i++) {
            char c = wwwAuth.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (inQuotes && c == '\\' && i + 1 < len) {
                token.append(c);
                c = wwwAuth.charAt(++i);
            }

            if (c == ',' && !inQuotes) {
                tokens.add(token.toString());
                token = new StringBuilder();
            } else {
                token.append(c);
            }
        }
        if (token.length() != 0) {
            tokens.add(token.toString());
        }
        return tokens;
    }

    public static class AuthScheme {
        private static final Pattern NAME_VALUE = Pattern.compile("^([^=]+)=(.*)$");

        public final String name;
        public final Map<String, String> params = new HashMap<>();
        public final List<String> unnamedParams = new ArrayList<>();

        public AuthScheme(String name) {
            this.name = name;
        }

        public void addRawParam(String authParam) {
            Matcher m = NAME_VALUE.matcher(authParam);
            if (m.matches()) {
                String key = m.group(1);
                String value = m.group(2);
                if (value != null && value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1).replace("\\\"", "\"");
                }
                params.put(key, value);
            } else {
                unnamedParams.add(authParam);
            }
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append(name).append("(");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                s.append(entry.getKey()).append("=[").append(entry.getValue()).append("],");
            }
            s.append(")");
            return s.toString();
        }
    }
}
