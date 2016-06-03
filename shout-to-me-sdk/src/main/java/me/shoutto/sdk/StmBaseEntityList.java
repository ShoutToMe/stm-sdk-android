package me.shoutto.sdk;

import android.util.Log;

import java.util.List;

/**
 * Created by tracyrojas on 6/3/16.
 */
public abstract class StmBaseEntityList<T> {

    private List<T> list;
    protected StmService stmService;
    private StmCallback<List<T>> callback;
    private String baseEndpoint;

    public StmBaseEntityList(StmService stmService, String baseEndpoint) {
        this.stmService = stmService;
        this.baseEndpoint = baseEndpoint;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public void setListAndCallCallback(List<T> list, StmError stmError) {
        this.list = list;
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

    public void getListAsync(StmCallback<List<T>> callback, JsonAdapter<T> jsonAdapter, String url) {
        this.callback = callback;

        try {
            new GetApiObjectsAsyncTask<T>(this, jsonAdapter).execute(url);
        } catch (Exception ex) {
            Log.e(getTag(), "Could not load channels due to problem with user auth token", ex);
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
