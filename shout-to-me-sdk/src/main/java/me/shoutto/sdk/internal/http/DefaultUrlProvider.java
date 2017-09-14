package me.shoutto.sdk.internal.http;

import me.shoutto.sdk.StmBaseEntity;

/**
 * Implementation of URL provider for Shout to Me API service endpoints
 */

public class DefaultUrlProvider implements StmUrlProvider {

    private String baseApiUrl;

    public DefaultUrlProvider(String baseApiUrl) {
        this.baseApiUrl = baseApiUrl;
    }

    @Override
    public String getUrl(StmBaseEntity entity, HttpMethod httpMethod) {
        String url = String.format("%s%s", baseApiUrl, entity.getBaseEndpoint());
        if (httpMethod.equals(HttpMethod.DELETE) || httpMethod.equals(HttpMethod.PUT)) {
            url = url.concat(String.format("/%s", entity.getId()));
        }
        return url;
    }
}
