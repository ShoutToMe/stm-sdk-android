package me.shoutto.sdk.internal.http;

import me.shoutto.sdk.StmBaseEntity;

/**
 * Interface that defines a Shout to Me API service URL provider
 */

interface StmUrlProvider {

    String getUrl(StmBaseEntity entity, HttpMethod httpMethod);
}
