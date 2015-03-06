package io.daydev.vkrdo.external;

import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.ResultTuple;

/**
 * Created by dmitry on 06.03.15.
 */
public interface SongSuplier {

    /**
     * returns to callback URL of resolved song or error
     * @param song song to resolve
     * @param callback callback function
     */
    public void getSongUriAsync(final SongInfo song, final Callback<ResultTuple<String>> callback);
}
