package me.shoutto.sdk.internal.http;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * BearerAuthHeaderProviderTest
 */
public class BearerAuthHeaderProviderTest {

    private static final String AUTH_TOKEN = "12345678";
    private static final String EXPECTED_RESPONSE = String.format("Bearer %s", AUTH_TOKEN);

    @Test
    public void generateHeaderValue_withNull_ShouldThrowException() throws Exception {
        try {
            BearerAuthHeaderProvider bearerAuthHeaderProvider = new BearerAuthHeaderProvider(null);
            bearerAuthHeaderProvider.getHeaderValue();
            fail();
        } catch (IllegalStateException ex) {
            // Pass
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void generateHeaderValue_WithValidInput_ShouldPass() throws Exception {
        try {
            BearerAuthHeaderProvider bearerAuthHeaderProvider = new BearerAuthHeaderProvider(AUTH_TOKEN);
            String response = bearerAuthHeaderProvider.getHeaderValue();
            assertEquals(response, EXPECTED_RESPONSE);
        } catch (Exception ex) {
            fail();
        }
    }
}