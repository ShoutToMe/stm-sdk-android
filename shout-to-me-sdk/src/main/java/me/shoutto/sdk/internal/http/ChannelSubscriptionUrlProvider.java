package me.shoutto.sdk.internal.http;

import me.shoutto.sdk.ChannelSubscription;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.User;

/**
 * Url Provider for channel subscriptions
 */

public class ChannelSubscriptionUrlProvider implements StmUrlProvider {

    private String baseApiUrl;
    private User user;

    public ChannelSubscriptionUrlProvider(String baseApiUrl, User user) {
        this.baseApiUrl = baseApiUrl;
        this.user = user;
    }
    @Override
    public String getUrl(StmBaseEntity entity, HttpMethod httpMethod) {
        String url = String.format("%s%s/%s%s", baseApiUrl, user.getBaseEndpoint(), user.getId(), entity.getBaseEndpoint());
        if (httpMethod.equals(HttpMethod.DELETE) || httpMethod.equals(HttpMethod.PUT)) {
            url = url.concat(String.format("/%s", ((ChannelSubscription)entity).getChannelId()));
        }
        return url;
    }
}
