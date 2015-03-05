package io.daydev.vkrdo.external.echo;

import android.os.AsyncTask;
import android.util.Log;
import com.echonest.api.v4.*;
import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.external.ConfigurationHolder;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;


public class EchoService {

    private static final String TAG = "ECHO";

    private volatile EchoNestAPI echoNest;
    private volatile DynamicPlaylistSession session;
    private volatile String title;


    public void getNextAsync(final int count, final Callback<Tuple<String, Collection<SongInfo>>> callback) {
        try {

            AsyncTask<String, String, Tuple<String, Collection<SongInfo>>> task = new AsyncTask<String, String, Tuple<String, Collection<SongInfo>>>() {
                @Override
                protected Tuple<String, Collection<SongInfo>> doInBackground(String... args) {
                    try {
                        if (session != null) {
                            Collection<SongInfo> ret = new ArrayList<SongInfo>();
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

    public void openSessionAsync( RadioInfo radioInfo, final Callback<RadioInfo> callback) {
        if (echoNest == null) {
            synchronized (this) {
                if (echoNest == null) {
                    echoNest = new EchoNestAPI(ConfigurationHolder.getInstance().getEchoKey());
                    echoNest.setTraceSends(false);
                }
            }
        }

        try {
            AsyncTask<RadioInfo, String, RadioInfo> task = new AsyncTask<RadioInfo, String, RadioInfo>() {
                @Override
                protected RadioInfo doInBackground(RadioInfo... args) {
                    RadioInfo radio = args[0];
                    try {
                        PlaylistParams params = new PlaylistParams();

                        params.includeAudioSummary();
                        if (radio.isEmpty()){
                            params.addGenre("pop");
                            params.setType(PlaylistParams.PlaylistType.GENRE_RADIO);
                        } else {


                            if (radio.getArtist() != null && !radio.getArtist().isEmpty()) {
                                params.addArtist(radio.getArtist());

                                if (radio.getMood() != null && !radio.getMood().isEmpty()) {
                                    params.addMood(radio.getMood());
                                }

                                if (radio.getArtistLinkType() != null) {
                                    if (radio.getArtistLinkType().equals(RadioInfo.ArtistLinkType.LIMIT)) {
                                        params.setType(PlaylistParams.PlaylistType.ARTIST);
                                    } else {
                                        params.setType(PlaylistParams.PlaylistType.ARTIST_RADIO);
                                    }
                                }
                            } else if (radio.getGenre() != null && !radio.getGenre().isEmpty()) {
                                params.addGenre(radio.getGenre());
                                params.setType(PlaylistParams.PlaylistType.GENRE_RADIO);
                            }

                            if (radio.getYearFrom() != null){
                                params.setArtistStartYearAfter(radio.getYearFrom());
                            }

                            if (radio.getYearTo() != null){
                                params.setArtistEndYearBefore(radio.getYearTo());
                            }
                        }
                        session = echoNest.createDynamicPlaylist(params);
                        title = radio.getTitle();
                        return radio;
                    } catch (Exception e) {
                        Log.e(TAG, "openSessionAsync", e);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(RadioInfo s) {
                    callback.callback(s);
                }
            };
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, radioInfo);
        } catch (Exception e) {
            Log.e(TAG, "openSessionAsync", e);
        }
    }

}
