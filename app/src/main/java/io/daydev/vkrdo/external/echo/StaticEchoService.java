package io.daydev.vkrdo.external.echo;

import android.os.AsyncTask;
import android.util.Log;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.Song;
import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * echonest service for static play list
 */
public class StaticEchoService extends AbstractEchoService {

    private volatile String title;
    private PlaylistParams playlistParams;
    private volatile boolean generating = false;


    private Queue<Song> songQueue = new ConcurrentLinkedQueue<>();

    @Override
    protected RadioInfo initPlayList(RadioInfo radioInfo, PlaylistParams params) throws EchoNestException {
        this.playlistParams = params;
        this.playlistParams.setResults(30);

        title = radioInfo.getTitle();
        songQueue.clear();

        pregenerate(title);

        return radioInfo;
    }

    private void pregenerate(final String radioTitle) {
        if (!generating) {
            try {
                final AsyncTask<PlaylistParams, String, String> task = new AsyncTask<PlaylistParams, String, String>() {
                    @Override
                    protected String doInBackground(PlaylistParams... params) {
                        try {
                            if (params[0] != null) {
                                generating = true;
                                Playlist playlist = echoNest.createStaticPlaylist(params[0]);
                                for (Song song : playlist.getSongs()) {
                                    if (radioTitle.equals(title)) {
                                        Log.i(TAG, "add " + song.getArtistName() + " " + song.getTitle());
                                        songQueue.add(song);
                                    } else {
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "while pregenerate static list", e);
                        } finally {
                            generating = false;
                        }

                        return null;
                    }
                };
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, playlistParams);
            } catch (Exception e) {
                Log.e(TAG, "pregenerate", e);
            }
        }
    }

    @Override
    public void getNextAsync(int count, Callback<Tuple<String, Collection<SongInfo>>> callback) {
        if (songQueue.size() < 5) {
            pregenerate(title);
        }
        Collection<SongInfo> ret = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Song song = songQueue.poll();
            if (song != null) {
                SongInfo info = new SongInfo(song.getArtistName(), song.getTitle(), null, null);
                info.setCoverArt(song.getCoverArt());
                ret.add(info);
            }
        }
        callback.callback(new Tuple<>(title, ret));
    }
}
