package me.shoutto.sdk.internal.http;

import org.junit.Test;

import me.shoutto.sdk.User;
import me.shoutto.sdk.UserLocation;

import static junit.framework.Assert.assertEquals;

/**
 * Test for UserLocationUrlProviderTest
 */

public class UserLocationUrlProviderTest {

    @Test
    public void getUrl() {
        User user = new User();
        user.setId("userId");

        String baseUrl = "http://url";

        UserLocationUrlProvider userLocationUrlProvider
                = new UserLocationUrlProvider(baseUrl, user);
        String url = userLocationUrlProvider.getUrl(new UserLocation(), HttpMethod.PUT);

        assertEquals(url, "http://url/users/userId/locations");
    }
}
