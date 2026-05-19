/*
 * Copyright 2026 Juanro49
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

package org.juanro.autumandu.util.backup;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {
    private int successCount = 0;
    private int failedCount = 0;
    private final List<String> errors = new ArrayList<>();

    public void incrementSuccess() {
        successCount++;
    }

    public void addError(String error) {
        failedCount++;
        errors.add(error);
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public String getErrorSummary() {
        if (errors.isEmpty()) {
            return "";
        }
        return String.join("\n", errors);
    }
}
