package me.shoutto.sdk.internal.http;

import android.util.Log;

import me.shoutto.sdk.StmBaseEntity;

/**
 * Creates the URL for message counts
 */

public class MessageCountUrlProvider implements StmUrlProvider {

    private static final String TAG = MessageCountUrlProvider.class.getSimpleName();
    private String baseApiUrl;
    private Boolean unreadOnly = false;

    public MessageCountUrlProvider(String baseApiUrl, Boolean unreadOnly) {
        this.baseApiUrl = baseApiUrl;

        if (unreadOnly != null) {
            this.unreadOnly = unreadOnly;
        }
    }

    @Override
    public String getUrl(StmBaseEntity entity, HttpMethod httpMethod) {
        String url = String.format("%s%s?count_only=true", baseApiUrl, entity.getBaseEndpoint());

        if (unreadOnly) {
            url = url.concat("&unread_only=true");
        }

        if (!HttpMethod.GET.equals(httpMethod)) {
            Log.w(TAG, "MessageCountUrlProvider only supports HttpMethod.GET");
        }

        return url;
    }
}
