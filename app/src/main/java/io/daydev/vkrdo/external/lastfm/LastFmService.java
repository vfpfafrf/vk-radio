package io.daydev.vkrdo.external.lastfm;

import android.os.AsyncTask;
import android.util.Log;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.external.ConfigurationHolder;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.Tuple;
import io.daydev.vkrdo.util.UserAgentList;
import de.umass.lastfm.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dmitry on 21.02.15.
 */
public class LastFmService {

    private Map<String,Tuple<String, String>> cache = new HashMap<>();
    public LastFmService() {
    }

    public void getArtistPhoto(SongInfo songInfo, final Callback<Tuple<String, String>> callback){

        if (cache.containsKey(songInfo.getArtist())){
            callback.callback(cache.get(songInfo.getArtist()));
            return;
        }
        try {
            AsyncTask<SongInfo, String, Tuple<String, String>> task = new AsyncTask<SongInfo, String, Tuple<String, String>>() {
                @Override
                protected Tuple<String, String> doInBackground(SongInfo... params) {
                    String photoUrl = null;
                    String albumTitle = null;
                    if (params[0] != null) {
                        String key = ConfigurationHolder.getInstance().getLastFmKey();
                        if (key != null && !key.isEmpty()) {
                            Caller.getInstance().setCache(null);
                            Caller.getInstance().setUserAgent(UserAgentList.next());

                            Track track = Track.getInfo(params[0].getArtist(), params[0].getTitle(), key);
                            if (track != null) {
                                albumTitle = track.getAlbum();
                                if (albumTitle != null) {
                                    // Log.e("omfg", albumTitle);
                                    Album album = Album.getInfo(params[0].getArtist(), albumTitle, key);
                                    //Log.e("omfg", album.toString());
                                    if (album != null) {
                                        photoUrl = album.getImageURL(ImageSize.MEGA);
                                        if (photoUrl == null) {
                                            photoUrl = album.getImageURL(ImageSize.EXTRALARGE);
                                            if (photoUrl == null) {
                                                photoUrl = album.getImageURL(ImageSize.HUGE);
                                            }
                                        }
                                    }
                                }
                            }

                            if (photoUrl == null) {
                                Artist artistInfo = Artist.getInfo(params[0].getArtist(), key);

                                if (artistInfo != null) {

                                    photoUrl = artistInfo.getImageURL(ImageSize.MEGA);
                                    if (photoUrl == null) {
                                        photoUrl = artistInfo.getImageURL(ImageSize.EXTRALARGE);
                                        if (photoUrl == null) {
                                            photoUrl = artistInfo.getImageURL(ImageSize.HUGE);
                                        }
                                    }
                                }
                                cache.put(params[0].getArtist(), new Tuple<String, String>(photoUrl, null));
                            }
                        }
                    }
                    return new Tuple<>(photoUrl, albumTitle);
                }
                @Override
                protected void onPostExecute(Tuple<String, String> tuple) {
                    if (tuple != null) {
                        callback.callback(tuple);
                    }
                }
            };
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, songInfo);
        }catch (Exception e){
            Log.e("", "", e);
        }
    }
}
