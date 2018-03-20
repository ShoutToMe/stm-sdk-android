package me.shoutto.sdk.internal.http;

import me.shoutto.sdk.StmBaseEntity;

/**
 * Generates the URL for the anonymous user create endpoint
 */

public class CreateUserUrlProvider implements StmUrlProvider {

    private static final String ANONYMOUS_USER_PATH = "/users/skip";
    private String baseApiUrl;

    public CreateUserUrlProvider(String baseApiUrl) {
        this.baseApiUrl = baseApiUrl;
    }

    @Override
    public String getUrl(StmBaseEntity entity, HttpMethod httpMethod) {
        return String.format("%s%s", baseApiUrl, ANONYMOUS_USER_PATH);
    }
}
