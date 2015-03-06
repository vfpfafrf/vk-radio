package io.daydev.vkrdo.external;

import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.Tuple;

import java.util.Collection;

/**
 * Created by dmitry on 06.03.15.
 */
public interface PlaylistSuplier {

    public void getNextAsync(final int count, final Callback<Tuple<String, Collection<SongInfo>>> callback);

    public void openSessionAsync(RadioInfo radioInfo, final Callback<RadioInfo> callback);
}
