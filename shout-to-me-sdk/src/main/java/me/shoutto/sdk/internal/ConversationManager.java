package me.shoutto.sdk.internal;

import android.content.Context;

import java.util.List;

import me.shoutto.sdk.Conversation;
import me.shoutto.sdk.internal.http.StmEntityListRequestSync;

/**
 * Conversation provides methods to access and manage Conversations.
 */

class ConversationManager {

    private StmPreferenceManager stmPreferenceManager;

    ConversationManager(Context context) {
        stmPreferenceManager = new StmPreferenceManager(context);
    }

    List<Conversation> getActiveConversations(String channelId) {
        String conversationRequestUrl = stmPreferenceManager.getServerUrl() + Conversation.BASE_ENDPOINT
                + "?channel_id=" + channelId + "&date_field=expiration_date"
                + "&hours=0";
        StmEntityListRequestSync<Conversation> conversationRequest = new StmEntityListRequestSync<>();
        return conversationRequest.process("GET", stmPreferenceManager.getAuthToken(),
                conversationRequestUrl, null, Conversation.getListSerializationType(),
                Conversation.LIST_SERIALIZATION_KEY);
    }
}
