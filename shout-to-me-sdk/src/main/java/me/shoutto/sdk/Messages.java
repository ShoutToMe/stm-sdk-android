package me.shoutto.sdk;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tracyrojas on 6/3/16.
 */
public class Messages extends StmBaseEntityList<Message> {

    private static final String TAG = "Messages";
    private List<Message> messages;

    public Messages(StmService stmService) {
        super(stmService, Message.BASE_ENDPOINT);
    }

    public Messages() {}

    public void getMessages(StmCallback<List<Message>> callback, boolean unreadOnly) {
        String url = getStmService().getServerUrl() + getBaseEndpoint();

        Map<String, String> queryStringParams = new HashMap<>();
        if (unreadOnly) {
            queryStringParams.put("unread", "true");
        }

        getListAsync(callback, url, queryStringParams);
    }

    public void getMessageCount(StmCallback<Integer> callback, boolean unreadOnly) {
        String url = getStmService().getServerUrl() + getBaseEndpoint();

        Map<String, String> queryStringParams = new HashMap<>();
        if (unreadOnly) {
            queryStringParams.put("unread", "true");
        }

        getCountAsync(callback, url, queryStringParams);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    public Type getSerializationListType() {
        return new TypeToken<List<Message>>(){}.getType();
    }
}
