package me.shoutto.sdk.internal.http;

/**
 * Returns the HTTP Authorization value using the 'Basic' type
 */

public class BasicAuthHeaderProvider implements HttpAuthHeaderProvider {

    private String authToken;

    public BasicAuthHeaderProvider(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public String getHeaderValue() throws IllegalStateException {
        if (authToken == null) {
            throw new IllegalStateException("'authToken' argument cannot be null");
        }
        return String.format("Basic %s", authToken);
    }
}
