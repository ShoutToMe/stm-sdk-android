package me.shoutto.sdk;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Test class
 */

public class UpdateUserRequestTest {

    @Test
    public void isValid_AllNullProperties_ShouldReturnFalse() {
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        assertFalse(updateUserRequest.isValid());
    }

    @Test
    public void isValid_AtLeastOneNonNullProperty_ShouldReturnTrue() {
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setHandle("handle");
        assertTrue(updateUserRequest.isValid());
    }

    @Test
    public void adaptBaseEntity_ShouldReturnUserObjectWithAdaptedProperties() {
        String channel = "channel";
        String email = "email";
        String handle = "handle";
        String phone = "phone";
        String topic = "topic";
        String gender = "gender";
        List<String> channelSubscriptions = new ArrayList<>();
        channelSubscriptions.add(channel);
        List<String> topicPreferences = new ArrayList<>();
        topicPreferences.add(topic);

        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setChannelSubscriptions(channelSubscriptions);
        updateUserRequest.setEmail(email);
        updateUserRequest.setHandle(handle);
        updateUserRequest.setPhone(phone);
        updateUserRequest.setTopicPreferences(topicPreferences);
        updateUserRequest.setGender(gender);

        User user = (User)updateUserRequest.adaptToBaseEntity();
        assertEquals(email, user.getEmail());
        assertEquals(handle, user.getHandle());
        assertEquals(phone, user.getPhone());
        assertEquals(channel, user.getChannelSubscriptions().get(0));
        assertEquals(topic, user.getTopicPreferences().get(0));
        assertEquals(gender, user.getMetaInfo().getGender());
    }

    @Test
    public void adaptBaseEntity_ShouldReturnEmptyStringWhenFieldNullPassedIn() {
        UpdateUserRequest updateUserRequest = new UpdateUserRequest();
        updateUserRequest.setGender(null);

        User user = (User)updateUserRequest.adaptToBaseEntity();
        assertNotNull(user.getMetaInfo().getGender());
        assertEquals("", user.getMetaInfo().getGender());
    }
}
