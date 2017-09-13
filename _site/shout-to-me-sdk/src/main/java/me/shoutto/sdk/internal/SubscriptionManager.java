package me.shoutto.sdk.internal;

import android.content.Context;

import java.util.List;

import me.shoutto.sdk.Subscription;
import me.shoutto.sdk.internal.http.StmEntityListRequestSync;

/**
 * SubscriptionManager provides methods to access Subscriptions.
 */

class SubscriptionManager {

    private StmPreferenceManager stmPreferenceManager;

    SubscriptionManager(Context context) {
        this.stmPreferenceManager = new StmPreferenceManager(context);
    }

    List<Subscription> getSubscriptions() {
        StmEntityListRequestSync<Subscription> subscriptionRequest = new StmEntityListRequestSync<>();
        return subscriptionRequest.process("GET", stmPreferenceManager.getAuthToken(),
                stmPreferenceManager.getServerUrl() + Subscription.BASE_ENDPOINT, null, Subscription.getListSerializationType(),
                Subscription.LIST_SERIALIZATION_KEY);
    }
}
