package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Use case for getting a single message
 */

public class GetMessage extends BaseUseCase<Message> {

    private static final String TAG = GetMessage.class.getSimpleName();

    public GetMessage(StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
    }

    public void get(String messageId, StmCallback<Message> callback) {

        if (messageId == null || "".equals(messageId)) {
            if (callback != null) {
                StmError error = new StmError("Invalid message ID", false, StmError.SEVERITY_MINOR);
                callback.onError(error);
            } else {
                Log.w(TAG, "Invalid message ID");
            }
            return;
        }

        this.callback = callback;

        Message message = new Message();
        message.setId(messageId);

        stmEntityRequestProcessor.processRequest(HttpMethod.GET, message);
    }
}
