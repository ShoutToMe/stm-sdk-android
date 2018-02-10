package me.shoutto.sdk.internal.usecases;

import android.util.Log;

import me.shoutto.sdk.StmBaseEntity;
import me.shoutto.sdk.StmCallback;
import me.shoutto.sdk.StmError;
import me.shoutto.sdk.TopicPreference;
import me.shoutto.sdk.internal.http.HttpMethod;
import me.shoutto.sdk.internal.http.StmRequestProcessor;

/**
 * Adds a topic preference to the user's record
 */

public class CreateTopicPreference extends BaseUseCase<StmBaseEntity, Void> {

    private static final String TAG = CreateTopicPreference.class.getSimpleName();

    public CreateTopicPreference(StmRequestProcessor<StmBaseEntity> stmRequestProcessor) {
        super(stmRequestProcessor);
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
            stmRequestProcessor.deleteObserver(this);
            return;
        }

        this.callback = callback;

        TopicPreference topicPreference = new TopicPreference();
        topicPreference.setTopic(topic);

        stmRequestProcessor.processRequest(HttpMethod.POST, topicPreference);
    }
}
