package me.shoutto.sdk.internal.http;

/**
 * Provides the interface to generate the HTTP Authorization token
 */

public interface HttpAuthHeaderProvider {
    String getHeaderValue() throws IllegalStateException;
}
