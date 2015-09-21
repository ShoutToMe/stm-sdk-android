package me.shoutto.sdk;

/**
 * Created by tracyrojas on 9/20/15.
 */
public class StmResponse<T> {

    private T t;

    public StmResponse(T t) {
        this.t = t;
    }

    public T get() {
        return t;
    }

    public void set(T t) {
        this.t = t;
    }
}
