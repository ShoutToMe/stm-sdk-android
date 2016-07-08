package me.shoutto.sdk;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class represents a Shout to Me Message.
 */
public class Message extends StmBaseEntity implements Comparable<Message> {

    private static final String TAG = "Message";
    public static final String BASE_ENDPOINT = "/messages";
    public static final String LIST_JSON_KEY = "messages";
    private Channel channel;
    private String channelId;
    private String conversationId;
    private String message = "";
    private String recipientId;
    private String senderName = "";
    private Date sentDate;

    public Message(StmService stmService) {
        super(stmService, TAG, BASE_ENDPOINT);
        try {
            sentDate = new SimpleDateFormat("yyyy-MM-dd").parse("1970-01-01");
        } catch (ParseException ex) {
            Log.w(TAG, "Could not parse sentDate default.");
        }
    }

    public Message(StmService stmService, String channelId, String conversationId, String message, String recipientId) {
        super(stmService, TAG, BASE_ENDPOINT);
        this.channelId = channelId;
        this.conversationId = conversationId;
        this.message = message;
        this.recipientId = recipientId;
    }

    public Channel getChannel() {
        return channel;
    }

    void setChannel(Channel channel) {
        this.channel = channel;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    void setMessage(String message) {
        this.message = message;
    }

    public String getSenderName() {
        return senderName;
    }

    void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Date getSentDate() {
        return sentDate;
    }

    void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }

    @Override
    public int compareTo(Message message) {
        return message.getSentDate().compareTo(this.getSentDate());
    }

    @Override
    protected void adaptFromJson(JSONObject jsonObject) {
        // Stubbed
    }

    void create(final StmCallback<Message> callback) {
        Response.Listener<JSONObject> responseListener = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    MessageJsonAdapter adapter = new MessageJsonAdapter(stmService);
                    Message message = adapter.adapt(response);
                    callback.onResponse(message);
                } catch (JSONException ex) {
                    Log.w(TAG, "Could not parse create message response.", ex);
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                StmError stmError = new StmError();
                stmError.setSeverity(StmError.SEVERITY_MAJOR);
                stmError.setBlockingError(false);
                try {
                    JSONObject responseData = new JSONObject(new String(error.networkResponse.data));
                    stmError.setMessage(responseData.getString("message"));
                } catch (JSONException ex) {
                    Log.e(TAG, "Error parsing JSON from create message response");
                    stmError.setMessage("An error occurred trying to create a message.");
                }
                if (callback != null) {
                    callback.onError(stmError);
                }
            }
        };

        JSONObject data = new JSONObject();
        try {
            data.put("channel_id", channelId);
        } catch (JSONException ex) {
            Log.w(TAG, "Message channel_id field not included. " + ex.getMessage());
        }

        try {
            data.put("conversation_id", conversationId);
        } catch (JSONException ex) {
            Log.w(TAG, "Message conversation_id field not included. " + ex.getMessage());
        }

        try {
            data.put("message", message);
        } catch (JSONException ex) {
            Log.w(TAG, "Message message field not included. " + ex.getMessage());
        }

        try {
            data.put("recipient_id", recipientId);
        } catch (JSONException ex) {
            Log.w(TAG, "Message recipient not included. " + ex.getMessage());
        }

        sendAuthorizedPostRequest(BASE_ENDPOINT, data, responseListener, errorListener);
    }

    public static class Builder {

        private String nestedChannelId;
        private String nestedConversationId;
        private String nestedMessage;
        private String nestedRecipientId;
        private StmService nestedStmService;

        public Builder(StmService stmService) {
            this.nestedStmService = stmService;
        }

        public Builder channelId(String channelId) {
            this.nestedChannelId = channelId;
            return this;
        }

        public Builder conversationId(String conversationId) {
            this.nestedConversationId = conversationId;
            return this;
        }

        public Builder message(String message) {
            this.nestedMessage = message;
            return this;
        }

        public Builder recipientId(String recipientId) {
            this.nestedRecipientId = recipientId;
            return this;
        }

        public void create(StmCallback<Message> callback) {
            Message message = new Message(nestedStmService, nestedChannelId, nestedConversationId,
                    nestedMessage, nestedRecipientId);
            message.create(callback);
        }
    }
}
