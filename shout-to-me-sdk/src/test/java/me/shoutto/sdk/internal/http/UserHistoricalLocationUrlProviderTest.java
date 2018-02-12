package me.shoutto.sdk.internal.http;

import org.junit.Test;

import me.shoutto.sdk.User;
import me.shoutto.sdk.UserLocation;

import static junit.framework.Assert.assertEquals;

/**
 * Test for UserHistoricalLocationUrlProvider
 */

public class UserHistoricalLocationUrlProviderTest {

    @Test
    public void getUrl() {
        User user = new User();
        user.setId("userId");

        String baseUrl = "http://url";

        UserHistoricalLocationUrlProvider userHistoricalLocationUrlProvider
                = new UserHistoricalLocationUrlProvider(baseUrl, user);
        String url = userHistoricalLocationUrlProvider.getUrl(new UserLocation(), HttpMethod.POST);

        assertEquals(url, "http://url/users/userId/historical-locations/batch");
    }
}
