package me.shoutto.sdk;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class StmRequestQueue {

    private static final String TAG = "StmRequestQueue";
    private static StmRequestQueue instance;
    private RequestQueue requestQueue;
    private static Context context;

    public StmRequestQueue(Context context) {
        this.context = context;
        requestQueue = getRequestQueue();
    }
    private StmRequestQueue() {}

    public static synchronized void setInstance(Context context) {
        if (instance == null) {
            instance = new StmRequestQueue(context);
        }
    }

    public static synchronized StmRequestQueue getInstance() {
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
