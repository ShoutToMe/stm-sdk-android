package me.shoutto.sdk;

import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tracyrojas on 6/3/16.
 */
public abstract class StmBaseEntityList<T> {

    private List<T> list;
    protected StmService stmService;
    private StmCallback<List<T>> callback;
    private StmCallback<Integer> countCallback;
    private String baseEndpoint;
    private int count;

    public StmBaseEntityList(StmService stmService, String baseEndpoint) {
        this.stmService = stmService;
        this.baseEndpoint = baseEndpoint;
    }

    public void setList(List<T> list) {
        if (this.list != null) {
            this.list.clear();
        }
        this.list = list;
    }

    public void executeListCallback(StmError stmError) {
        if (callback != null) {
            if (stmError != null) {
                callback.onError(stmError);
            } else {
                callback.onResponse(list);
            }
            callback = null;
        }
    }

    public List<T> getList() {
        return list;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public void executeCountCallback(StmError stmError) {
        if (countCallback != null) {
            if (stmError != null) {
                countCallback.onError(stmError);
            } else {
                countCallback.onResponse(count);
            }
        }
    }

    public void getListAsync(StmCallback<List<T>> callback, JsonAdapter<T> jsonAdapter, String url,
                             Map<String, String> queryStringParams) {
        this.callback = callback;

        if (queryStringParams.size() > 0) {
            url += "?";
            for (Map.Entry<String, String> entry : queryStringParams.entrySet()) {
                url += entry.getKey() + "=" + entry.getValue() + "&";
            }
            url = url.substring(0, url.length() - 1);
        }

        try {
            new GetApiObjectsAsyncTask<T>(this, jsonAdapter, false).execute(url);
        } catch (Exception ex) {
            Log.e(getTag(), "Could not load messages due to problem with user auth token", ex);
        }
    }

    public void getCountAsync(StmCallback<Integer> countCallback, String url, Map<String, String> queryStringParams) {

        this.countCallback = countCallback;

        queryStringParams.put("count_only", "true");
        if (queryStringParams.size() > 0) {
            url += "?";
            for (Map.Entry<String, String> entry : queryStringParams.entrySet()) {
                url += entry.getKey() + "=" + entry.getValue() + "&";
            }
            url = url.substring(0, url.length() - 1);
        }

        try {
            new GetApiObjectsAsyncTask<T>(this, null, true).execute(url);
        } catch (Exception ex) {
            Log.e(getTag(), "Could not load message count due to problem with user auth token", ex);
        }
    }

    public StmService getStmService() {
        return stmService;
    }

    public String getBaseEndpoint() {
        return baseEndpoint;
    }

    abstract String getTag();
}
