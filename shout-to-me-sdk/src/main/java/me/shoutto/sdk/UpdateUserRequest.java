package me.shoutto.sdk;

import java.util.List;

/**
 * Contains data which is used to update the Shout to Me user.
 */

public class UpdateUserRequest implements StmEntityActionRequest {

    private List<String> channelSubscriptions;
    private String handle;
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
        return true;
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

        if (handle != null) {
            user.setHandle(handle);
        }

        if (topicPreferences != null) {
            user.setTopicPreferences(topicPreferences);
        }

        return user;
    }
}
