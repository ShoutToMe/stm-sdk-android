package me.shoutto.sdk;

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

    public void getMessages(StmCallback<List<Message>> callback, boolean unreadOnly) {
        String url = getStmService().getServerUrl() + getBaseEndpoint();

        Map<String, String> queryStringParams = new HashMap<>();
        if (unreadOnly) {
            queryStringParams.put("unread", "true");
        }

        getListAsync(callback, new MessageJsonAdapter(stmService), url, queryStringParams);
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
}
