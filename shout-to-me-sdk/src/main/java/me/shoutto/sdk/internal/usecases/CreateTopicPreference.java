package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.TopicPreference;
import me.shoutto.sdk.internal.StmObservableResults;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmEntityRequestProcessor;

/**
 * Adds a topic preference to the user's record
 */

public class CreateTopicPreference extends BaseUseCase<Void> {

    private static final String TAG = CreateTopicPreference.class.getSimpleName();

    public CreateTopicPreference(StmEntityRequestProcessor stmEntityRequestProcessor) {
        super(stmEntityRequestProcessor);
    }

    public void create(String topic, StmCallback<Void> callback) {

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

        stmEntityRequestProcessor.processRequest(HttpMethod.POST, topicPreference);
    }
}
