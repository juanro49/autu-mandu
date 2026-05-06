package org.juanro.autumandu.util.backup;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class CSVImportProcessingRuntimeExceptionTest {

    @Test
    public void testExceptionMessageAndCause() {
        String message = "Test message";
        Throwable cause = new RuntimeException("Original cause");
        CSVImportProcessingRuntimeException exception = new CSVImportProcessingRuntimeException(message, cause);

        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
