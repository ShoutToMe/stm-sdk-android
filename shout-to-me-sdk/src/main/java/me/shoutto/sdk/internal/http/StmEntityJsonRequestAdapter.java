package me.shoutto.sdk.internal.http;

import me.shoutto.sdk.StmBaseEntity;

/**
 * Interface for adapting a Shout to Me entity to a JSON object for HTTP calls
 */

interface StmEntityJsonRequestAdapter {

    String adapt(StmBaseEntity entity);
}
