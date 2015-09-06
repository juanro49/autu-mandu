package com.dropbox.client2;

/**
 * Contains the SDK version number.
 */
public final class SdkVersion {

    /** Returns the SDK version number. */
    public static String get() {
        return "1.6.3";  // Filled in by build process.
    }

    public static void main(String[] args) {
        System.out.println("Dropbox Java SDK, Version " + get());
    }
}
