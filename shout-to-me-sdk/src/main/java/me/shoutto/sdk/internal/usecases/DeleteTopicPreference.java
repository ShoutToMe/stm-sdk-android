package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.TopicPreference;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Deletes a topic preference from the user's record
 */

public class DeleteTopicPreference extends BaseUseCase<Void> {

    private static final String TAG = DeleteTopicPreference.class.getSimpleName();

    public DeleteTopicPreference(StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
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
            stmEntityRequestProcessor.deleteObserver(this);
            return;
        }

        this.callback = callback;

        TopicPreference topicPreference = new TopicPreference();
        topicPreference.setTopic(topic);

        stmEntityRequestProcessor.processRequest(HttpMethod.DELETE, topicPreference);
    }
}
