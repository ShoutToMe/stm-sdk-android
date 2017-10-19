package me.shoutto.sdk.internal.http;

import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.TopicPreference;
import me.shoutto.sdk.User;

/**
 * URL provider for user topic preferences
 */

public class TopicUrlProvider implements StmUrlProvider {

    private static final String TAG = TopicUrlProvider.class.getSimpleName();
    private String baseApiUrl;
    private User user;

    public TopicUrlProvider(String baseApiUrl, User user) {
        this.baseApiUrl = baseApiUrl;
        this.user = user;
    }

    @Override
    public String getUrl(StmBaseEntity entity, HttpMethod httpMethod) {
        String url = String.format("%s%s/%s%s", baseApiUrl, user.getBaseEndpoint(), user.getId(), entity.getBaseEndpoint());
        if (httpMethod.equals(HttpMethod.DELETE) || httpMethod.equals(HttpMethod.PUT)
                || httpMethod.equals(HttpMethod.GET)) {
            String encodedTopic = "";
            try {
                encodedTopic = new URI(null, null, ((TopicPreference) entity).getTopic(), null, null).toString();
            } catch (URISyntaxException e) {
                Log.e(TAG, "Unable to URL encode topic " + ((TopicPreference) entity).getTopic(), e);
            }

            url = url.concat(String.format("/%s", encodedTopic));
        }
        return url;
    }
}
