package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.TopicPreference;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

/**
 * Deletes a topic preference from the user's record
 */

public class DeleteTopicPreference extends BaseUseCase<StmBaseEntity, Void> {

    private static final String TAG = DeleteTopicPreference.class.getSimpleName();

    public DeleteTopicPreference(StmRequestProcessor<StmBaseEntity> stmRequestProcessor) {
        super(stmRequestProcessor);
    }

    public void delete(String topic, StmCallback<Void> callback) {
        if (topic == null || "".equals(topic)) {
            String validationErrorMessage = "topic is required for creating a topic preference";
            if (callback != null) {
                StmError error = new StmError(validationErrorMessage, false, StmError.SEVERITY_MINOR);
                callback.onError(error);
            } else {
                Log.w(TAG, validationErrorMessage);
            }
            stmRequestProcessor.deleteObserver(this);
            return;
        }

        this.callback = callback;

        TopicPreference topicPreference = new TopicPreference();
        topicPreference.setTopic(topic);

        stmRequestProcessor.processRequest(HttpMethod.DELETE, topicPreference);
    }
}
