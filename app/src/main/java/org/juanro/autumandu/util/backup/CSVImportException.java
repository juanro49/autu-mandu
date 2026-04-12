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
package org.juanro.autumandu.util.backup;

import java.io.Serial;

/**
 * Excepción personalizada para errores durante la exportación o importación de CSV.
 */
public class CSVImportException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public CSVImportException(String message) {
        super(message);
    }

    public CSVImportException(Throwable cause) {
        super(cause);
    }

    public CSVImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
