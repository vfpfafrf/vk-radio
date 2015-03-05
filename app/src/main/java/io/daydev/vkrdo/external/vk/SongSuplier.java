package io.daydev.vkrdo.external.vk;

import android.util.Log;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.util.Callback;
import com.vk.sdk.api.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

public class SongSuplier {

    public void getSongUriAsync(final SongInfo song, final Callback<String> callback) {
        VKRequest get = new VKRequest("audio.search", VKParameters.from(VKApiConst.Q, song.getArtist() + " " + song.getTitle(), VKApiConst.COUNT, 10));
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
                } catch (Exception e) {
                    Log.e("omfg", "", e);
                }

                if (!ret.isEmpty()) {
                    callback.callback(ret.iterator().next());
                } else {
                    callback.callback(null);
                }
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);
            }
        });
    }
}
