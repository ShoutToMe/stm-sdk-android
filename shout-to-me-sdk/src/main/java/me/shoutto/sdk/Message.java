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

    public Message() {
        super(SERIALIZATION_KEY);
    }

    public Channel getChannel() {
        return channel;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getMessage() {
        return message;
    }

    public Sender getSender() {
        return sender;
    }

    public Date getSentDate() {
        return sentDate;
    }

    @Override
    public int compareTo(@NonNull Message message) {
        return message.getSentDate().compareTo(this.getSentDate());
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) {
        // Stubbed
    }

    public static Type getSerializationType() {
        return new TypeToken<Message>(){}.getType();
    }

    public static Type getSerializationListType() {
        return new TypeToken<List<Message>>(){}.getType();
    }

    public class Sender {

        @SuppressWarnings("unused")
        private String handle;

        public String getHandle() {
            return handle;
        }
    }
}
