package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.Message;
import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

/**
 * Use case for getting a single message
 */

public class GetMessage extends BaseUseCase<StmBaseEntity, Message> {

    private static final String TAG = GetMessage.class.getSimpleName();

    public GetMessage(StmRequestProcessor<StmBaseEntity> stmRequestProcessor) {
        super(stmRequestProcessor);
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

        stmRequestProcessor.processRequest(HttpMethod.GET, message);
    }
}
