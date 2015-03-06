package io.daydev.vkrdo.external.vk;

import android.util.Log;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.external.SongSuplier;
import io.daydev.vkrdo.util.Callback;
import com.vk.sdk.api.*;
import io.daydev.vkrdo.util.ResultTuple;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

public class VkSongSuplier implements SongSuplier{

    /**
     * returns to callback URL of resolved song or error
     * @param song song to resolve
     * @param callback callback function
     */
    @Override
    public void getSongUriAsync(final SongInfo song, final Callback<ResultTuple<String>> callback) {
        String title = song.getTitle();
        if (title.contains(" (Radio Edit)")){
            title = title.replace(" (Radio Edit)", "");
        }
        VKRequest get = new VKRequest("audio.search", VKParameters.from(VKApiConst.Q, song.getArtist() + " " + title, VKApiConst.COUNT, 10));
        get.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);

                Collection<String> ret = new ArrayList<>();
                try {
                    JSONObject jsonObject = response.json;
                    if (jsonObject.getJSONObject("response").getInt("count") > 0) {
                        JSONArray jArray = jsonObject.getJSONObject("response").getJSONArray("items");
                        for (int i = 0; i < 1; i++) {
                            JSONObject jObject = jArray.getJSONObject(i);

                            String artist = jObject.getString("artist");
                            String title = jObject.getString("title");

                            String check = title.replaceAll(" ", "");
                            if (artist.equals(song.getArtist()) && !check.toLowerCase().contains("(remix)") && !check.toLowerCase().contains("(mix)")) {
                                ret.add(jObject.getString("url"));
                            }
                        }
                    }

                    if (!ret.isEmpty()) {
                        callback.callback(ResultTuple.success(ret.iterator().next()));
                    } else {
                        callback.callback(null);
                    }
                } catch (Exception e) {
                    Log.e("SongSuplier", "while getting song", e);
                    callback.callback(ResultTuple.<String>error(e.getMessage()));
                }
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);
                callback.callback(ResultTuple.<String>error(error.errorMessage));
            }
        });
    }
}
