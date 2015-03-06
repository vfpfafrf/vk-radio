package io.daydev.vkrdo.external.echo;

import android.os.AsyncTask;
import android.util.Log;
import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.PlaylistParams;
import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.external.ConfigurationHolder;
import io.daydev.vkrdo.external.PlaylistSuplier;
import io.daydev.vkrdo.util.Callback;

/**
 * Created by dmitry on 06.03.15.
 */
public abstract class AbstractEchoService implements PlaylistSuplier {

    protected static final String TAG = "ECHO";

    protected volatile EchoNestAPI echoNest;

    public void openSessionAsync(final RadioInfo radioInfo, final Callback<RadioInfo> callback) {
        if (echoNest == null) {
            synchronized (this) {
                if (echoNest == null) {
                    echoNest = new EchoNestAPI(ConfigurationHolder.getInstance().getEchoKey());
                    echoNest.setTraceSends(false);
                }
            }
        }

        try {
            AsyncTask<PlaylistParams, String, RadioInfo> task = new AsyncTask<PlaylistParams, String, RadioInfo>() {
                @Override
                protected RadioInfo doInBackground(PlaylistParams... args) {
                    try {
                        return initPlayList (radioInfo, args[0]);
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
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, generateParams(radioInfo));
        } catch (Exception e) {
            Log.e(TAG, "openSessionAsync", e);
        }
    }

    protected abstract RadioInfo initPlayList(RadioInfo radioInfo, PlaylistParams params) throws EchoNestException;

    protected PlaylistParams generateParams(RadioInfo radioInfo) {
        PlaylistParams params = new PlaylistParams();

        params.includeAudioSummary();
        if (radioInfo.isEmpty()) {
            params.addGenre("pop");
            params.setType(PlaylistParams.PlaylistType.GENRE_RADIO);
        } else {


            if (radioInfo.getArtist() != null && !radioInfo.getArtist().isEmpty()) {
                params.addArtist(radioInfo.getArtist());

                if (radioInfo.getMood() != null && !radioInfo.getMood().isEmpty()) {
                    params.addMood(radioInfo.getMood());
                }

                if (radioInfo.getArtistLinkType() != null) {
                    if (radioInfo.getArtistLinkType().equals(RadioInfo.ArtistLinkType.LIMIT)) {
                        params.setType(PlaylistParams.PlaylistType.ARTIST);
                    } else {
                        params.setType(PlaylistParams.PlaylistType.ARTIST_RADIO);
                    }
                }
            } else if (radioInfo.getGenre() != null && !radioInfo.getGenre().isEmpty()) {
                params.addGenre(radioInfo.getGenre());
                params.setType(PlaylistParams.PlaylistType.GENRE_RADIO);
            }

            if (radioInfo.getYearFrom() != null) {
                params.setArtistStartYearAfter(radioInfo.getYearFrom());
            }

            if (radioInfo.getYearTo() != null) {
                params.setArtistEndYearBefore(radioInfo.getYearTo());
            }
        }
        return params;
    }


}
