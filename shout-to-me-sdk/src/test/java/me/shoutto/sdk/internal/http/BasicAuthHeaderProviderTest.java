package me.shoutto.sdk.internal.http;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * BasicAuthHeaderProviderTest
 */
public class BasicAuthHeaderProviderTest {

    private static final String AUTH_TOKEN = "12345678";
    private static final String EXPECTED_RESPONSE = String.format("Basic %s", AUTH_TOKEN);

    @Test
    public void generateHeaderValue_withNull_ShouldThrowException() throws Exception {
        try {
            BasicAuthHeaderProvider basicAuthHeaderProvider = new BasicAuthHeaderProvider(null);
            basicAuthHeaderProvider.getHeaderValue();
            fail();
        } catch (IllegalStateException ex) {
            // Pass
        } catch (Exception ex) {
            fail();
        }
    }

    @Test
    public void generateHeaderValue_withValidInput_ShouldPass() throws Exception {
        try {
            BasicAuthHeaderProvider basicAuthHeaderProvider = new BasicAuthHeaderProvider(AUTH_TOKEN);
            String response = basicAuthHeaderProvider.getHeaderValue();
            assertEquals(response, EXPECTED_RESPONSE);
        } catch (Exception ex) {
            fail();
        }
    }
}