package io.daydev.vkrdo.external.lastfm;

import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import de.umass.lastfm.*;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.external.ConfigurationHolder;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.UserAgentList;

import java.util.ArrayList;
import java.util.Collection;


public class LastFmService {

    private static final String TAG = "LastFmService";

    private LruCache<String, LastFmInfo> cache = new LruCache<>(200);
    private LruCache<String, Collection<String>> artistCache = new LruCache<>(300);

    public class LastFmInfo {
        String albumTitle;
        String photoUrl;
        Collection<String> similar;

        public LastFmInfo(String albumTitle, String photoUrl, Collection<String> similar) {
            this.albumTitle = albumTitle;
            this.photoUrl = photoUrl;
            this.similar = similar;
        }

        public String getAlbumTitle() {
            return albumTitle;
        }

        public String getPhotoUrl() {
            return photoUrl;
        }

        public Collection<String> getSimilar() {
            return similar;
        }
    }


    public void getArtistPhoto(SongInfo songInfo, final Callback<LastFmInfo> callback) {

        if (cache.get(songInfo.getArtist()) != null) {
            callback.callback(cache.get(songInfo.getArtist()));
            return;
        }
        try {
            AsyncTask<SongInfo, String, LastFmInfo> task = new AsyncTask<SongInfo, String, LastFmInfo>() {
                @Override
                protected LastFmInfo doInBackground(SongInfo... params) {
                    String photoUrl = null;
                    String albumTitle = null;
                    Collection<String> similar = null;
                    if (params[0] != null) {
                        try {
                            String key = ConfigurationHolder.getInstance().getLastFmKey();
                            if (key != null && !key.isEmpty()) {
                                Caller.getInstance().setCache(null);
                                Caller.getInstance().setUserAgent(UserAgentList.next());

                                Track track = Track.getInfo(params[0].getArtist(), params[0].getTitle(), key);
                                if (track != null) {
                                    albumTitle = track.getAlbum();
                                    if (albumTitle != null) {
                                        Album album = Album.getInfo(params[0].getArtist(), albumTitle, key);
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

                                if (photoUrl != null){
                                    similar = artistCache.get(params[0].getArtist());
                                }

                                if (photoUrl == null || similar == null) {
                                    Artist artistInfo = Artist.getInfo(params[0].getArtist(), key);
                                    if (artistInfo != null) {
                                        Collection<Artist> res = artistInfo.getSimilar();
                                        if (res != null && !res.isEmpty()) {
                                            similar = new ArrayList<>();
                                            for (Artist artist : res) {
                                                similar.add(artist.getName());
                                            }
                                            artistCache.put(params[0].getArtist(), similar);
                                        }

                                        if (photoUrl == null) {
                                            photoUrl = artistInfo.getImageURL(ImageSize.MEGA);
                                            if (photoUrl == null) {
                                                photoUrl = artistInfo.getImageURL(ImageSize.EXTRALARGE);
                                                if (photoUrl == null) {
                                                    photoUrl = artistInfo.getImageURL(ImageSize.HUGE);
                                                }
                                            }
                                            cache.put(params[0].getArtist(), new LastFmInfo(albumTitle, photoUrl, similar));
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "getArtistPhoto", e);
                        }
                    }
                    return new LastFmInfo(albumTitle, photoUrl, similar);
                }

                @Override
                protected void onPostExecute(LastFmInfo tuple) {
                    if (tuple != null) {
                        callback.callback(tuple);
                    }
                }
            };
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, songInfo);
        } catch (Exception e) {
            Log.e(TAG, "getArtistPhoto", e);
        }
    }

    public void getSimilarArtists(SongInfo songInfo, final Callback<Collection<String>> callback) {
        Collection<String> ret = artistCache.get(songInfo.getArtist());
        if (ret != null) {
            callback.callback(ret);
        } else {
            try {
                AsyncTask<SongInfo, String, Collection<String>> task = new AsyncTask<SongInfo, String, Collection<String>>() {
                    @Override
                    protected Collection<String> doInBackground(SongInfo... params) {
                        if (params[0] != null) {
                            try {
                                String key = ConfigurationHolder.getInstance().getLastFmKey();
                                if (key != null && !key.isEmpty()) {
                                    Caller.getInstance().setCache(null);
                                    Caller.getInstance().setUserAgent(UserAgentList.next());

                                    Collection<String> ret = new ArrayList<>();
                                    Collection<Artist> res = Artist.getSimilar(params[0].getArtist(), 5, key);
                                    if (res != null && !res.isEmpty()) {
                                        for (Artist artist : res) {
                                            ret.add(artist.getName());
                                        }
                                    }
                                    artistCache.put(params[0].getArtist(), ret);
                                    return ret;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "getSimilarArtists", e);
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Collection<String> artistCollection) {
                        if (artistCollection != null) {
                            callback.callback(artistCollection);
                        }
                    }
                };
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, songInfo);
            } catch (Exception e) {
                Log.e(TAG, "getSimilarArtists", e);
            }
        }
    }
}
