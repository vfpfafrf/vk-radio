package io.daydev.vkrdo.util;

/**
 * Created by dmitry on 22.02.15.
 */
public interface Callback<V> {

    /**
     * calls callback
     * @param obj V result
     */
    public void callback(V obj);
}
