package me.shoutto.sdk;

import java.util.List;

/**
 * Contains data which is used to update the Shout to Me user.
 */

public class UpdateUserRequest implements StmEntityActionRequest {

    private List<String> channelSubscriptions;
    private String email;
    private String gender;
    private String handle;
    private String phone;
    private String platformEndpointArn;
    private Boolean platformEndpointEnabled;
    private List<String> topicPreferences;

    /**
     * Gets a list of the channel IDs for all channels the user is subscribed to
     * @return The list of subscribed channel IDs
     */
    public List<String> getChannelSubscriptions() {
        return channelSubscriptions;
    }

    /**
     * Sets the list of channel IDs which will replace the existing user's channel subscriptions
     * @param channelSubscriptions The list of channel IDs
     */
    public void setChannelSubscriptions(List<String> channelSubscriptions) {
        this.channelSubscriptions = channelSubscriptions;
    }

    /**
     * Gets the user's email address
     * @return The user's email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address
     * @param email The user's email address
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the gender that will be sent in the update user request
     * @return The gender
     */
    public String getGender() {
        return gender;
    }

    /**
     * Sets the gender that will be sent in the update user request. There is no enum for this
     * value as client developers can specify values based on their organization's conventions
     * @param gender
     */
    public void setGender(String gender) {
        if (gender == null) {
            this.gender = "";
        } else {
            this.gender = gender;
        }
    }

    /**
     * Gets the user's handle
     * @return The user's handle
     */
    public String getHandle() {
        return handle;
    }

    /**
     * Sets the user's handle
     * @param handle A string that represents the user's handle
     */
    public void setHandle(String handle) {
        this.handle = handle;
    }

    /**
     * Gets the user's phone number
     * @return The user's phone number
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the user's phone number
     * @param phone The user's phone number
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPlatformEndpointArn() {
        return platformEndpointArn;
    }

    public void setPlatformEndpointArn(String platformEndpointArn) {
        this.platformEndpointArn = platformEndpointArn;
    }

    public Boolean getPlatformEndpointEnabled() {
        return platformEndpointEnabled;
    }

    public void setPlatformEndpointEnabled(Boolean platformEndpointEnabled) {
        this.platformEndpointEnabled = platformEndpointEnabled;
    }

    /**
     * Gets the list of topics that the user is following. If a user is following topics, they will
     * only receive notifications for topics in the list.
     * @return A list of topics
     */
    public List<String> getTopicPreferences() {
        return topicPreferences;
    }

    /**
     * Sets the list of topics the user wishes to follow. This list will replace the list in the
     * Shout to Me system.
     * @param topicPreferences A list of topics
     */
    public void setTopicPreferences(List<String> topicPreferences) {
        this.topicPreferences = topicPreferences;
    }

    /**
     * Returns the status of whether the current object contains the minimum required data for
     * the request and if that data is valid
     * @return true if the current object contains valid data
     */
    @Override
    public boolean isValid() {

        return !(channelSubscriptions == null
                && email == null
                && gender == null
                && handle == null
                && phone == null
                && platformEndpointArn == null
                && platformEndpointEnabled == null
                && topicPreferences == null);
    }

    /**
     * Convers the current object to an {@link StmBaseEntity} object
     * @return A {@link StmBaseEntity} object with non-null properties set
     */
    @Override
    public StmBaseEntity adaptToBaseEntity() {
        User user = new User();

        if (channelSubscriptions != null) {
            user.setChannelSubscriptions(channelSubscriptions);
        }

        if (email != null) {
            user.setEmail(email);
        }

        if (handle != null) {
            user.setHandle(handle);
        }

        if (phone != null) {
            user.setPhone(phone);
        }

        if (platformEndpointArn != null) {
            user.setPlatformEndpointArn(platformEndpointArn);
        }

        if (platformEndpointEnabled != null) {
            user.setPlatformEndpointEnabled(platformEndpointEnabled);
        }

        if (topicPreferences != null) {
            user.setTopicPreferences(topicPreferences);
        }

        if (gender != null) {
            User.MetaInfo metaInfo = new User.MetaInfo();
            metaInfo.setGender(gender);
            user.setMetaInfo(metaInfo);
        }

        return user;
    }
}
