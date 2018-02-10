package me.shoutto.sdk.internal.http;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.User;

/**
 * A URL provider for user historical location processing
 */

public class UserHistoricalLocationUrlProvider implements StmUrlProvider {

    private String baseApiUrl;
    private User user;

    public UserHistoricalLocationUrlProvider(String baseApiUrl, User user) {
        this.baseApiUrl = baseApiUrl;
        this.user = user;
    }

    @Override
    public String getUrl(StmBaseEntity entity, HttpMethod httpMethod) {
        return String.format("%s%s/%s%s", baseApiUrl, user.getBaseEndpoint(), user.getId(), "/historical-locations/batch");
    }
}
