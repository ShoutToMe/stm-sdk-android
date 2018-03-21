package me.shoutto.sdk.internal.http;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.User;

/**
 * Url provider to update the user's location
 */

public class UserLocationUrlProvider implements StmUrlProvider {

    private String baseApiUrl;
    private User user;

    public UserLocationUrlProvider(String baseApiUrl, User user) {
        this.baseApiUrl = baseApiUrl;
        this.user = user;
    }

    @Override
    public String getUrl(StmBaseEntity entity, HttpMethod httpMethod) {
        return String.format("%s%s/%s/locations", baseApiUrl, user.getBaseEndpoint(), user.getId());
    }
}
