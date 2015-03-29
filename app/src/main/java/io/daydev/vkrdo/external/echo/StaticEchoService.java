package io.daydev.vkrdo.external.echo;

import android.os.AsyncTask;
import android.util.Log;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Playlist;
import com.echonest.api.v4.PlaylistParams;
import com.echonest.api.v4.Song;
import com.vk.sdk.api.*;
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
    private int artistsOffset = 0;
    private boolean useVk = false;
    private List<String> artists;

    private Queue<SongInfo> songQueue = new ConcurrentLinkedQueue<>();

    @Override
    protected RadioInfo initPlayList(RadioInfo radioInfo, PlaylistParamsWrapper params) throws EchoNestException {

        playlistParams =  params.getPlaylistParams();
        artists = params.getArtists();
        if (artists != null && !artists.isEmpty()) {
            Collections.shuffle(artists);
            artistsOffset = 0;
        }

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

                                    PlaylistParams pl = new PlaylistParams();
                                    pl.getMap().putAll(params[0].getMap());

                                    int maxResult = 30;
                                    int fallbackResult = 10;
                                    if (artists != null && !artists.isEmpty()){
                                        if (artists.size() < 5) { // ONLY 5 artists allowed for API call
                                            for (String artist : artists) {
                                                pl.addArtist(artist);
                                            }
                                        } else {
                                            maxResult = 7;
                                            fallbackResult = 5;
                                            for (int i=artistsOffset;i<artistsOffset+5;i++) {
                                                if (i < artists.size()) {
                                                    pl.addArtist(artists.get(i));
                                                }
                                            }
                                            artistsOffset += 5;
                                            if (artistsOffset >= artists.size()){
                                                artistsOffset -= artists.size();
                                            }
                                        }
                                    }
                                    pl.setResults(maxResult);

                                    Playlist playlist = echoNest.createStaticPlaylist(pl);
                                    for (Song song : playlist.getSongs()) {
                                        if (radioTitle != null && radioTitle.equals(title)) {
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
                                    if (radioTitle != null && radioTitle.equals(title) && songQueue.size() < fallbackResult) {
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

                        @Override
                        protected void onPostExecute(String s) {
                            if (artists != null && !artists.isEmpty()) {
                                if (artists.size() > 5) { // ONLY 5 artists allowed for API call
                                    List<VKRequest> vkRequests = new ArrayList<>();
                                    for (int i=artistsOffset;i<artistsOffset+4;i++) {
                                        if (i < artists.size()) {
                                            vkRequests.add(vkGetAudioRequest(artists.get(i), 10)); // 10 to fix stupid vk.com search
                                        }
                                    }
                                    artistsOffset += 5;
                                    if (artistsOffset >= artists.size()){
                                        artistsOffset -= artists.size();
                                    }
                                    VKBatchRequest batch = new VKBatchRequest(vkRequests.toArray(new VKRequest[vkRequests.size()]));
                                    batch.executeWithListener(new VKBatchRequest.VKBatchRequestListener() {
                                        @Override
                                        public void onComplete(VKResponse[] responses) {
                                            super.onComplete(responses);
                                            if (responses != null && responses.length > 0) {
                                                for (VKResponse response : responses) {
                                                    try {
                                                        String artist = response.request.getMethodParameters().get(VKApiConst.Q).toString();
                                                        processVkResponse(response, artist, 1);
                                                    } catch (Exception e) {
                                                        Log.e(TAG, "", e);
                                                    }
                                                }
                                                // TODO: may be shuffle queu ??
                                            }
                                        }
                                    });
                                }
                            }
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
                vkRequest(artist);
            }
        }
    }

    private VKRequest vkGetAudioRequest(String artist, int count){
        return new VKRequest("audio.search", VKParameters.from(VKApiConst.Q, artist,
                VKApiConst.COUNT, count,
                VKApiConst.SORT, 2,
                VKApiConst.OFFSET, offset,
                "performer_only", 1));
    }

    private void processVkResponse(VKResponse response, String radioArtists, int resultCount) throws Exception{
            JSONObject jsonObject = response.json;
            int count = jsonObject.getJSONObject("response").getInt("count");
            if (count > 0) {
                List<SongInfo> playList = new ArrayList<>();
                JSONArray jArray = jsonObject.getJSONObject("response").getJSONArray("items");
                String radioTitleLower = radioArtists.toLowerCase();
                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject jObject = jArray.getJSONObject(i);

                    String artist = jObject.getString("artist");
                    String title = jObject.getString("title");
                    String url = jObject.getString("url");

                    if (artist != null && title != null && url != null) {
                        title = title.trim();
                        artist = artist.trim();

                        if (artist.toLowerCase().contains(radioTitleLower)) {
                            String check = title.replaceAll(" ", "").toLowerCase();
                            if (!check.contains("(remix)") && !check.contains("(mix)")) {
                                SongInfo songInfo = new SongInfo(artist, title, url, null);

                                if (!playList.contains(songInfo)) {
                                    playList.add(songInfo);

                                    if (playList.size() == resultCount){
                                        break;
                                    }
                                }
                            }
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

    }

    private void vkRequest(final String artist){
        vkGetAudioRequest(artist, 50).executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                try {
                    processVkResponse(response, artist, -1);
                } catch (Exception e) {
                    Log.e("SongSuplier", "while getting song", e);
                } finally {
                    generating = false;
                }
            }
        });
    }
}
