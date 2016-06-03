package me.shoutto.sdk;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tracyrojas on 6/3/16.
 */
public interface JsonAdapter<T> {

    public T adapt(JSONObject jsonObject) throws JSONException;
}
