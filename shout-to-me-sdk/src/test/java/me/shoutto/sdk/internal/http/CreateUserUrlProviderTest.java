package me.shoutto.sdk.internal.http;

import org.junit.Test;

import me.shoutto.sdk.User;

import static org.junit.Assert.*;

/**
 * CreateUserUrlProviderTest
 */
public class CreateUserUrlProviderTest {

    @Test
    public void getUrl_ReturnsValidUrl() {

        String baseApiUrl = "https://app.shoutto.me/api/v1";
        CreateUserUrlProvider createUserUrlProvider = new CreateUserUrlProvider(baseApiUrl);
        assertEquals(createUserUrlProvider.getUrl(new User(), HttpMethod.POST),
                String.format("%s/users/skip", baseApiUrl));
    }
}