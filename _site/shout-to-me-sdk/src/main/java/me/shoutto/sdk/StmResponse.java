package me.shoutto.sdk;

/**
 * This class is used in the onSuccess method of the {@link Callback} class.  A client will normally
 * only use one method, the get() method which will return the typed response object.
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
