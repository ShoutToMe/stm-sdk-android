package me.shoutto.sdk;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

/**
 * This class represents a Shout to Me Message object.
 */
public class Message extends StmBaseEntity implements Comparable<Message> {

    /**
     * The base endpoint of messages on the Shout to Me REST API.
     */
    public transient static final String BASE_ENDPOINT = "/messages";

    /**
     * The key used for JSON serialization of message objects.
     */
    public transient static final String SERIALIZATION_KEY = "message";

    /**
     * The key used for JSON serialization of message lists.
     */
    public transient static final String LIST_SERIALIZATION_KEY = SERIALIZATION_KEY + "s";

    @SuppressWarnings("unused")
    private Channel channel;

    @SuppressWarnings("unused")
    private String channelId;

    @SuppressWarnings("unused")
    private String conversationId;

    @SuppressWarnings("unused")
    private String message;

    @SuppressWarnings("unused")
    private String recipientId;

    @SuppressWarnings("unused")
    private Sender sender;

    @SuppressWarnings("unused")
    @SerializedName("created_date")
    private Date sentDate;

    /**
     * The default constructor.
     */
    public Message() {
        super(SERIALIZATION_KEY);
    }

    /**
     * Gets the Channel associated with the Message.
     * @return The Channel associated with the Message.
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Gets the conversation ID for the Conversation associated with the Message.
     * @return The conversation ID.
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     * Gets the message body.
     * @return The message body.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the Sender of the Message.
     * @return The Sender of the Message.
     */
    public Sender getSender() {
        return sender;
    }

    /**
     * Gets the date the Message was sent.
     * @return The date the Message was sent.
     */
    public Date getSentDate() {
        return sentDate;
    }

    /**
     * Implementaion of Comparable interface.
     * @param message The Message to compare to the instance.
     * @return The comparison result.
     */
    @Override
    public int compareTo(@NonNull Message message) {
        return message.getSentDate().compareTo(this.getSentDate());
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) {
        // Stubbed
    }

    /**
     * Gets the serialization type that is used in Gson parsing.
     * @return The serialization type to be used in Gson parsing.
     */
    public static Type getSerializationType() {
        return new TypeToken<Message>(){}.getType();
    }

    /**
     * Gets the serialization type of a message list that is used in Gson parsing.
     * @return The serialization type of a message list to be used in Gson parsing.
     */
    public static Type getSerializationListType() {
        return new TypeToken<List<Message>>(){}.getType();
    }

    /**
     * The class represents the user who sent the Message.
     */
    public class Sender {

        @SuppressWarnings("unused")
        private String handle;

        /**
         *  Gets the sender's handle.
         * @return The sender's handle.
         */
        public String getHandle() {
            return handle;
        }
    }
}
