package me.shoutto.sdk;

import java.util.Date;

/**
 * Created by tracyrojas on 6/3/16.
 */
public class Message extends StmBaseEntity {

    private static final String TAG = "Message";
    public static final String BASE_ENDPOINT = "/messages";
    public static final String LIST_JSON_KEY = "messages";
    private String channelName;
    private String Message;
    private String senderName;
    private Date sentDate;

    public Message(StmService stmService) {
        super(stmService, TAG, BASE_ENDPOINT);
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public Date getSentDate() {
        return sentDate;
    }

    public void setSentDate(Date sentDate) {
        this.sentDate = sentDate;
    }
}
