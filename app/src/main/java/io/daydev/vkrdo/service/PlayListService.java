package io.daydev.vkrdo.service;

import android.os.Handler;
import android.util.Log;
import io.daydev.vkrdo.bean.PlayList;
import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.external.PlaylistSuplier;
import io.daydev.vkrdo.external.SongSuplier;
import io.daydev.vkrdo.external.echo.StaticEchoService;
import io.daydev.vkrdo.external.lastfm.LastFmService;
import io.daydev.vkrdo.external.vk.VkSongSuplier;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.CallbackChecker;
import io.daydev.vkrdo.util.ResultTuple;
import io.daydev.vkrdo.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 */
public class PlayListService extends AbstractLocalBinderService implements PlayList {

    public static final SongInfo WAIT = new SongInfo("waiting for network...","", null, null);
    public static final SongInfo FAIL = new SongInfo("error while generate playlist...","", null, null);

    private static final int PLAY_LIST_CAPATIBILITY = 3;
    private static final int RESOLVE_LIST_CAPATIBILITY = 5;

    private Queue<SongInfo> toPlay = new ConcurrentLinkedQueue<>();
    private Queue<SongInfo> toResolve = new ConcurrentLinkedQueue<>();

    private PlaylistSuplier echoService = new StaticEchoService();
    private SongSuplier vkService = new VkSongSuplier();
    private LastFmService lastFmService = new LastFmService();

    private Callback<SongInfo> callback;
    private CallbackChecker<SongInfo> callbackChecker;

    private volatile RadioInfo currentRadio;
    private SongInfo currentSong;

    private Handler resolverHandler;


    @Override
    public boolean isPlayListStated() {
        return currentRadio != null;
    }

    /**
     * generate play list due to genre
     *
     * @param radioInfo - radio settings
     * @return true - if new playlist generated
     */
    @Override
    public boolean generate(RadioInfo radioInfo) {
        if (radioInfo != null && currentRadio != null && currentRadio.isSame(radioInfo)){
            return false;
        } else {
            toPlay.clear();
            toResolve.clear();
        }
        resolverHandler.removeCallbacks(vkResolver);
        Log.e("PlayListService", "Starting for "+radioInfo.getTitle());

        currentRadio = radioInfo;
        echoService.openSessionAsync(currentRadio, new Callback<RadioInfo>() {
            @Override
            public void callback(RadioInfo obj) {
                if (obj != null) {
                    addToResolve(2);
                } else {
                    currentRadio = null;
                }
            }
        });

        vkResolver.run();
        return true;
    }

    private void addToResolve(int count){
            echoService.getNextAsync(count, new Callback<Tuple<String, Collection<SongInfo>>>() {
                @Override
                public void callback(Tuple<String, Collection<SongInfo>> obj) {
                    if (obj != null && currentRadio != null) {
                        String title = obj.getFirst();
                        if (title != null && title.equals(currentRadio.getTitle())) {
                            Collection<SongInfo> songs = obj.getSecond();
                            if (songs != null && !songs.isEmpty()) {
                                for (SongInfo info : songs) {
                                    toResolve.add(info);
                                    Log.i("PlayListService", "added "+info);
                                    if (toResolve.size() > RESOLVE_LIST_CAPATIBILITY + 2) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            });
    }

    Runnable vkResolver = new Runnable() {
        @Override
        public void run() {
            final RadioInfo radio = currentRadio;
            if (radio != null) {
                final SongInfo songInfo = toResolve.poll();
                if (songInfo != null) {
                    if (songInfo.getLocation() == null || songInfo.getLocation().isEmpty()) {
                        vkService.getSongUriAsync(songInfo, new Callback<ResultTuple<String>>() {
                            @Override
                            public void callback(ResultTuple<String> result) {
                                if (result != null) {
                                    if (result.hasResult() && radio.isSame(currentRadio)) {
                                        songInfo.setLocation(result.getResult());
                                        toPlay.add(songInfo);
                                        Log.i("PlayListService", "resolved " + songInfo);
                                        if (callback != null) {
                                            callback.callback(songInfo);
                                        }
                                    }
                                }
                            }
                        });
                    } else {
                        toPlay.add(songInfo);
                        Log.i("PlayListService", "resolved " + songInfo);
                        if (callback != null) {
                            callback.callback(songInfo);
                        }
                    }
                    if (callbackChecker != null && !callbackChecker.check(songInfo)) {
                        lastFmService.getArtistPhoto(songInfo, new Callback<LastFmService.LastFmInfo>() {
                            @Override
                            public void callback(LastFmService.LastFmInfo info) {
                                if (info != null && radio.isSame(currentRadio)) {
                                    songInfo.setArtistPhoto(info.getPhotoUrl());
                                    songInfo.setAlbum(info.getAlbumTitle());
                                    songInfo.setSimilarArtists(info.getSimilar());
                                }
                            }
                        });
                    } else if (songInfo.getSimilarArtists() == null) {
                        lastFmService.getSimilarArtists(songInfo, new Callback<Collection<String>>() {
                            @Override
                            public void callback(Collection<String> obj) {
                                if (obj != null && radio.isSame(currentRadio)) {
                                    songInfo.setSimilarArtists(obj);
                                }
                            }
                        });
                    }
                }
                resolverHandler.postDelayed(vkResolver, 500);
            }
        }
    };


    /**
     * return next playable song
     *
     * @return SongInfo
     */
    @Override
    public SongInfo next() {
        if (toPlay.size() < PLAY_LIST_CAPATIBILITY && toResolve.size() < RESOLVE_LIST_CAPATIBILITY) {
            addToResolve(2);
        }

        currentSong = toPlay.poll();
        return getCurrent();
    }

    /**
     * returns current playing song
     *
     * @return SongInfo
     */
    @Override
    public SongInfo getCurrent() {
        return currentSong == null ? WAIT : currentSong;
    }

    public RadioInfo getCurrentRadio(){
        return currentRadio;
    }

    /**
     * return "to-play-list" in simple string format
     *
     * @return Collection
     */
    @Override
    public Collection<String> getToPlaySimpleFormat() {
        Collection ret = new ArrayList<>();
        Collections.addAll(ret, toPlay.toArray());
        return ret;
    }

    public void setCallback(Callback<SongInfo> callback) {
        this.callback = callback;
    }

    public void setCallbackChecker(CallbackChecker<SongInfo> checker){
        this.callbackChecker = checker;
    }


    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        resolverHandler = new Handler();
    }



}
