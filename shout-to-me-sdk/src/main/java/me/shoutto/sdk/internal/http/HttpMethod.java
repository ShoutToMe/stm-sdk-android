package me.shoutto.sdk.internal.http;

/**
 * Supported HTTP methods for the Shout to Me API service
 */

public enum HttpMethod {
    DELETE("DELETE"),
    GET("GET"),
    POST("POST"),
    PUT("PUT");

    private final String httpMethod;

    HttpMethod(final String httpMethod) {
        this.httpMethod = httpMethod;
    }

    @Override
    public String toString() {
        return httpMethod;
    }
}
