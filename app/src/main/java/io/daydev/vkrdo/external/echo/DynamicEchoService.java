package io.daydev.vkrdo.external.echo;

import android.os.AsyncTask;
import android.util.Log;
import com.echonest.api.v4.*;
import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;


public class DynamicEchoService extends AbstractEchoService {

    private volatile DynamicPlaylistSession session;
    private volatile String title;


    public void getNextAsync(final int count, final Callback<Tuple<String, Collection<SongInfo>>> callback) {
        try {

            AsyncTask<String, String, Tuple<String, Collection<SongInfo>>> task = new AsyncTask<String, String, Tuple<String, Collection<SongInfo>>>() {
                @Override
                protected Tuple<String, Collection<SongInfo>> doInBackground(String... args) {
                    try {
                        if (session != null) {
                            Collection<SongInfo> ret = new ArrayList<>();
                            Playlist playlist = session.next(count, 0);
                            for (Song song : playlist.getSongs()) {
                                SongInfo info = new SongInfo(song.getArtistName(), song.getTitle(), null, null);
                                info.setCoverArt(song.getCoverArt());
                                ret.add(info);
                            }
                            return new Tuple<>(title, ret);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "getNextAsync", e);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Tuple<String, Collection<SongInfo>> o) {
                    callback.callback(o);
                }
            };
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (Exception e) {
            Log.e(TAG, "getNextAsync", e);
        }
    }


    @Override
    protected RadioInfo initPlayList(RadioInfo radioInfo, PlaylistParams params) throws EchoNestException {
        session = echoNest.createDynamicPlaylist(params);
        title = radioInfo.getTitle();
        return radioInfo;
    }

}
