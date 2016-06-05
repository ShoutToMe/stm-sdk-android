package me.shoutto.sdk;

import android.os.AsyncTask;

import java.util.List;

/**
 * Created by tracyrojas on 6/3/16.
 */
public class Messages extends StmBaseEntityList<Message> {

    private static final String TAG = "Messages";
    private List<Message> messages;

    public Messages(StmService stmService) {
        super(stmService, Message.BASE_ENDPOINT);
    }

    public void getMessages(StmCallback<List<Message>> callback) {
        String url = getStmService().getServerUrl() + getBaseEndpoint() + "?recipient_id=" + stmService.getUser().getId();
        getListAsync(callback, new MessageJsonAdapter(stmService), url);
    }

    @Override
    protected String getTag() {
        return TAG;
    }
}
