package me.shoutto.sdk.internal.http;

import org.junit.Test;
import org.mockito.Spy;

import me.shoutto.sdk.Message;

import static junit.framework.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * MessageCountUrlProviderTest
 */

public class MessageCountUrlProviderTest {

    @Test
    public void getUrl_WithUnreadOnlyFalse_ShouldReturnUrlWithoutUnreadFlag() {
        String baseUrl = "http://app.shoutto.me/api/v1";
        Message messageSpy = spy(new Message());
        doReturn("/messages").when(messageSpy).getBaseEndpoint();

        String fullUrl = baseUrl + "/messages?count_only=true";
        MessageCountUrlProvider messageCountUrlProvider = new MessageCountUrlProvider(baseUrl, false);
        String response = messageCountUrlProvider.getUrl(messageSpy, HttpMethod.GET);

        assertEquals(response, fullUrl);
    }

    @Test
    public void getUrl_WithUnreadOnlyTrue_ShouldReturnUrlWithUnreadFlag() {
        String baseUrl = "http://app.shoutto.me/api/v1";
        Message messageSpy = spy(new Message());
        doReturn("/messages").when(messageSpy).getBaseEndpoint();

        String fullUrl = baseUrl + "/messages?count_only=true&unread_only=true";
        MessageCountUrlProvider messageCountUrlProvider = new MessageCountUrlProvider(baseUrl, true);
        String response = messageCountUrlProvider.getUrl(messageSpy, HttpMethod.GET);

        assertEquals(response, fullUrl);
    }
}
