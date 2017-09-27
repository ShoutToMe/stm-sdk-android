package me.shoutto.sdk.internal.http;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.TopicPreference;
import me.shoutto.sdk.User;

/**
 * URL provider for user topic preferences
 */

public class TopicUrlProvider implements StmUrlProvider {

    private String baseApiUrl;
    private User user;

    public TopicUrlProvider(String baseApiUrl, User user) {
        this.baseApiUrl = baseApiUrl;
        this.user = user;
    }

    @Override
    public String getUrl(StmBaseEntity entity, HttpMethod httpMethod) {
        String url = String.format("%s%s/%s%s", baseApiUrl, user.getBaseEndpoint(), user.getId(), entity.getBaseEndpoint());
        if (httpMethod.equals(HttpMethod.DELETE) || httpMethod.equals(HttpMethod.PUT)) {
            url = url.concat(String.format("/%s", ((TopicPreference)entity).getTopic()));
        }
        return url;
    }
}
