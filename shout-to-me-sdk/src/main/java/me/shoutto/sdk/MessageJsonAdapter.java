package me.shoutto.sdk;

import android.util.Log;

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
        return message;
    }
}
