package me.shoutto.sdk.internal.http;

/**
 * Returns the HTTP Authorization value using the 'Bearer' type
 */

public class BearerAuthHeaderProvider implements HttpAuthHeaderProvider {

    private String authToken;

    public BearerAuthHeaderProvider(String authToken) {
        this.authToken = authToken;
    }

    @Override
    public String getHeaderValue() throws IllegalStateException {
        if (authToken == null) {
            throw new IllegalStateException("'authToken' argument cannot be null");
        }
        return String.format("Bearer %s", authToken);
    }
}
