package io.daydev.vkrdo.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import io.daydev.vkrdo.MediaEvent;
import io.daydev.vkrdo.R;
import io.daydev.vkrdo.bean.RadioBuilder;
import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.service.MediaPlayerService;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.CallbackChecker;

import java.io.Serializable;
import java.util.Collection;

/**
 * Radio player fragment
 */
public class RadioFragment extends Fragment implements Callback<Message>, CallbackChecker<RadioInfo> {

    public static final String RADIO_PARAM = "radio";
    public static final String EXTRA_PARAM = "extra-param";
    public static final String EXTRA_PLAY = "extra-play";

    private RadioInfo radioInfo;

    private TextView currentSongArtist;
    private TextView currentSongTitle;
    private TextView nextSongArtist;
    private TextView nextSongTitle;
    private SeekBar seekBar;
    private ImageView albumCover;
    private Button buttonPlayStop;
    private Button buttonNext;
    private MenuItem actionFav;

    private enum State {
        PLAY, PAUSE
    }

    private State currentState;

    // cache resources string
    private String play;
    private String pause;
    private String next;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main, menu);
        actionFav = menu.findItem(R.id.action_fav);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        radioInfo = null;
        View rootView = inflater.inflate(R.layout.radio, container, false);

        Bundle arguments = getArguments();
        if (arguments != null){
            radioInfo = (RadioInfo) arguments.getSerializable(RADIO_PARAM);

            Intent intent = new Intent(MediaEvent.EVENT);
            intent.putExtra(MediaEvent.TYPE, MediaEvent.RADIO_TITLE);
            intent.putExtra(MediaEvent.RADIO_TITLE, radioInfo.getTitle());
            LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(intent);
        }

        if (radioInfo == null){
            radioInfo = RadioBuilder.buildDefault();
        }

        currentSongArtist = (TextView) rootView.findViewById(R.id.currentSong);
        currentSongTitle = (TextView) rootView.findViewById(R.id.currentSongTitle);
        albumCover = (ImageView) rootView.findViewById(R.id.albumArt);
        nextSongArtist = (TextView) rootView.findViewById(R.id.nextSongArtist);
        nextSongTitle = (TextView) rootView.findViewById(R.id.nextSongTitle);

        currentState = State.PAUSE;

        buttonNext = (Button) rootView.findViewById(R.id.ButtonNext);
        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendToMediaService(MediaPlayerService.ACTION_NEXT);
            }
        });

        buttonPlayStop = (Button) rootView.findViewById(R.id.ButtonPlayStop);
        buttonPlayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (State.PAUSE.equals(currentState)) {
                    Log.e("RF", "Sending play with "+radioInfo);
                    sendToMediaService(MediaPlayerService.ACTION_PLAY);
                } else {
                    sendToMediaService(MediaPlayerService.ACTION_PAUSE);
                }
            }
        });


        seekBar = (SeekBar) rootView.findViewById(R.id.SeekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendToMediaService (MediaPlayerService.ACTION_SEEK, MediaPlayerService.EXTRA_SEEK, seekBar.getProgress());
            }
        });
        seekBar.setProgress(0);
        seekBar.setMax(100);

        sendToMediaService(MediaPlayerService.ACTION_STATUS);

        play = getString(R.string.play_str);
        pause = getString(R.string.pause_str);
        next = getString(R.string.next_song_wait);

        if (arguments != null) {
            String param = (String) arguments.getSerializable(EXTRA_PARAM);
            if (param != null && !param.isEmpty()){
                if (EXTRA_PLAY.equalsIgnoreCase(param)){
                    //start playing!
                    sendToMediaService(MediaPlayerService.ACTION_PLAY);
                }
            }
        }
        return rootView;
    }


    private void sendToMediaService(String msg){
        sendToMediaService(msg, null, null);
    }

    private void sendToMediaService(String msg, String extra, Serializable extraValue){
        Intent intent = new Intent(getActivity(), MediaPlayerService.class);
        intent.setAction(msg);
        intent.putExtra(MediaPlayerService.EXTRA_RADIO, radioInfo);
        if (extra != null && extraValue != null){
            intent.putExtra(extra, extraValue);
        }
        getActivity().startService(intent);
    }

    /**
     * Broadcast Message callback
     *
     * @param msg V result
     */
    @Override
    public void callback(Message msg) {
        try {
            switch (msg.what) {
                case MediaPlayerService.MSG_SET_CURRENT_SONG:
                    SongInfo song = (SongInfo) msg.obj;
                    currentSongArtist.setText(song.getArtist());
                    currentSongTitle.setText(song.getTitle());
                    break;
                case MediaPlayerService.MSG_SET_DURATION:
                    seekBar.setMax((Integer) msg.obj);
                    seekBar.setProgress(0);
                    break;
                case MediaPlayerService.MSG_TRACK_LIST_CHANGES:
                    Collection<SongInfo> data = (Collection) msg.obj;
                    if (data != null && !data.isEmpty() && data.size() > 1) {
                        SongInfo songInfo = data.iterator().next();
                        nextSongArtist.setText(songInfo.getArtist());
                        nextSongTitle.setText(songInfo.getTitle());
                    } else {
                        nextSongArtist.setText(next);
                        nextSongTitle.setText("");
                    }
                    break;
                case MediaPlayerService.MSG_PAUSE:
                    currentState = State.PAUSE;
                    buttonPlayStop.setText(play);
                    break;
                case MediaPlayerService.MSG_PLAY:
                    buttonNext.setEnabled(true); //eq in any way - this radio started play - so we can use next
                    currentState = State.PLAY;
                    buttonPlayStop.setText(pause);
                    break;
                case MediaPlayerService.MSG_STOP:
                    seekBar.setProgress(0);
                    currentState = State.PAUSE;
                    buttonPlayStop.setText(play);
                    break;
                case MediaPlayerService.MSG_PROGRESS:
                    seekBar.setProgress((Integer) msg.obj);
                    break;
                case MediaPlayerService.MSG_ART:
                    if (msg.obj == null) {
                        albumCover.setImageResource(R.drawable.ic_headphones);
                    } else {
                        albumCover.setImageBitmap((Bitmap) msg.obj);
                    }
                    break;
                case MediaPlayerService.MSG_FAV:
                    if (actionFav != null) {
                        actionFav.setIcon(android.R.drawable.star_on);
                    }
                    break;
                case MediaPlayerService.MSG_NOT_FAV:
                    if (actionFav != null) {
                        actionFav.setIcon(android.R.drawable.star_off);
                    }
                    break;
            }
        } catch (Exception e){ // eq fragment not connected to activity race for resources
            Log.e("RadioFragment", "on message handle", e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //sendToMediaService(MediaPlayerService.ACTION_STOP);
    }

    /**
     * checks external radio object - if its played in this fragment
     * @param obj radio info to check
     * @return boolean
     */
    @Override
    public boolean check(RadioInfo obj) {
        return !(obj == null || radioInfo == null) && radioInfo.isSame(obj);
    }
}