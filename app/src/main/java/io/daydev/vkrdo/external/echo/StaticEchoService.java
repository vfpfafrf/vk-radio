package io.daydev.vkrdo.external.echo;

import android.os.AsyncTask;
import android.util.Log;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.Song;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.Tuple;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * echonest service for static play list
 */
public class StaticEchoService extends AbstractEchoService {

    private volatile String title;
    private PlaylistParams playlistParams;
    private volatile boolean generating = false;
    private int offset = 0;
    private boolean useVk = false;


    private Queue<SongInfo> songQueue = new ConcurrentLinkedQueue<>();

    @Override
    protected RadioInfo initPlayList(RadioInfo radioInfo, PlaylistParams params) throws EchoNestException {
        this.playlistParams = params;
        this.playlistParams.setResults(30);
        offset = 0;
        useVk = false;

        title = radioInfo.getTitle();
        songQueue.clear();

        pregenerate(title);

        return radioInfo;
    }

    private void pregenerate(final String radioTitle) {
        if (!generating) {
            try {
                if (!useVk) {
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
                                            SongInfo info = new SongInfo(song.getArtistName(), song.getTitle(), null, null);
                                            info.setCoverArt(song.getCoverArt());
                                            if (!songQueue.contains(info)) {
                                                songQueue.add(info);
                                            }
                                        } else {
                                            break;
                                        }
                                    }
                                    if (radioTitle.equals(title) && songQueue.size() < 10) {
                                        useVk = true;
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
                } else {
                    vkPlayListGenerator(radioTitle);
                }
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
            SongInfo song = songQueue.poll();
            if (song != null) {
                ret.add(song);
            }
        }
        callback.callback(new Tuple<>(title, ret));
    }

    private void vkPlayListGenerator(final String radioTitle){
        if (radioTitle.equals(title) && useVk && !generating) { //oh shit...little radio gen - setup to VK
            final String artist = (String) playlistParams.getMap().get("artist");
            Log.i(TAG, "start vk playlist gen with " + artist + " offset " + offset);
            if (artist != null) {
                generating = true;
                VKRequest get = new VKRequest("audio.search", VKParameters.from(VKApiConst.Q, artist, VKApiConst.COUNT, 50, VKApiConst.SORT, 2, VKApiConst.OFFSET, offset));
                get.executeWithListener(new VKRequest.VKRequestListener() {
                    @Override
                    public void onComplete(VKResponse response) {
                        super.onComplete(response);
                        try {
                            JSONObject jsonObject = response.json;
                            int count = jsonObject.getJSONObject("response").getInt("count");
                            if (count > 0) {
                                List<SongInfo> playList = new ArrayList<>();
                                JSONArray jArray = jsonObject.getJSONObject("response").getJSONArray("items");
                                for (int i = 0; i < jArray.length(); i++) {
                                    JSONObject jObject = jArray.getJSONObject(i);

                                    String artist = jObject.getString("artist");
                                    String title = jObject.getString("title");
                                    String url = jObject.getString("url");

                                    String check = title.replaceAll(" ", "");
                                    if (!check.toLowerCase().contains("(remix)") && !check.toLowerCase().contains("(mix)")) {
                                        SongInfo songInfo = new SongInfo(artist, title, url, null);

                                        if (!playList.contains(songInfo)) {
                                            playList.add(songInfo);
                                        }
                                    }
                                    offset++;
                                }

                                if (!playList.isEmpty()) {
                                    Collections.shuffle(playList, new Random(System.currentTimeMillis()));
                                    for (SongInfo songInfo : playList) {
                                        Log.i(TAG, "add vk " + songInfo);
                                        songQueue.add(songInfo);
                                    }
                                }
                            } else {
                                offset = 0;
                            }
                        } catch (Exception e) {
                            Log.e("SongSuplier", "while getting song", e);
                        } finally {
                            generating = false;
                        }
                    }
                });
            }
        }
    }
}
