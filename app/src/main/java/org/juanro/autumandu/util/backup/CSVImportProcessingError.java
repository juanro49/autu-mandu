package org.juanro.autumandu.util.backup;

public class CSVImportProcessingError extends RuntimeException {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    public CSVImportProcessingError(String message, Throwable cause) {
        super(message, cause);
    }
}
