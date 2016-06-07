package me.shoutto.sdk;

import android.util.Log;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tracyrojas on 6/3/16.
 */
public class MessageJsonAdapter implements JsonAdapter<Message> {

    private static final String TAG = "MessageJsonAdapter";
    private StmService stmService;

    public MessageJsonAdapter(StmService stmService) {
        this.stmService = stmService;
    }

    @Override
    public Message adapt(JSONObject jsonObject) throws JSONException {
        Message message = new Message(stmService);

        message.setId(jsonObject.getString("id"));
        message.setMessage(jsonObject.getString("message"));

        String createdDate = jsonObject.getString("created_date");
        DateTime dateTime = new DateTime(createdDate);
        message.setSentDate(dateTime.toDate());

        JSONObject channel = jsonObject.getJSONObject("channel");
        message.setChannelName(channel.getString("name"));

        JSONObject sender = jsonObject.getJSONObject("sender");
        String senderName = "";
        try {
            senderName = sender.getString("handle");
        } catch (JSONException ex) {
            senderName = "Anonymous";
        }
        message.setSenderName(senderName);

        return message;
    }
}
