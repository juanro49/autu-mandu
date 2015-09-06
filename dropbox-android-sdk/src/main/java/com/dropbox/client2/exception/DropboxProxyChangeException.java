package com.dropbox.client2.exception;

public class DropboxProxyChangeException extends DropboxIOException {
    public DropboxProxyChangeException() {
        super("Proxy may have been updated, try request again.");
    }
}
