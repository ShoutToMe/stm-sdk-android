package me.shoutto.sdk.internal.http;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import me.shoutto.sdk.TopicPreference;
import me.shoutto.sdk.User;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * TopicUrlProviderTest
 */

public class TopicUrlProviderTest {

    @Test
    public void getUrl_WithUnencodedTopic_ReturnsUrlEncodedTopic() {

        String unencodedString = "unencoded characters ~`#%^{}|$-_.+!*'(),;/?@=&";


        User user = new User();
        user.setId("userId");

        TopicPreference topicPreference = new TopicPreference();
        topicPreference.setTopic(unencodedString);

        TopicUrlProvider topicUrlProvider = new TopicUrlProvider("", user);
        String url = topicUrlProvider.getUrl(topicPreference, HttpMethod.GET);

        String encodedString = "";
        try {
            encodedString = new URI(null, null, unencodedString, null, null).toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail();
        }

        assertEquals(url, "/users/userId/topic_preference/" + encodedString);
    }
}
