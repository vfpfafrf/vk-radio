package io.daydev.vkrdo.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

    private TextView currentSong;
    private TextView nextSong;
    private SeekBar seekBar;
    private ImageView albumCover;
    private Button buttonPlayStop;

    private enum State {
        PLAY, PAUSE
    }

    private State currentState;

    // cache resources string
    private String play;
    private String pause;
    private String next;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        radioInfo = null;
        View rootView = inflater.inflate(R.layout.radio, container, false);

        Bundle arguments = getArguments();
        if (arguments != null){
            radioInfo = (RadioInfo) arguments.getSerializable(RADIO_PARAM);

            Intent intent = new Intent(MediaEvent.EVENT);
            intent.putExtra(MediaEvent.RADIO_TITLE, radioInfo.getTitle());
            LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(intent);
        }

        if (radioInfo == null){
            radioInfo = RadioBuilder.buildDefault();
        }

        currentSong = (TextView) rootView.findViewById(R.id.currentSong);
        albumCover = (ImageView) rootView.findViewById(R.id.albumArt);
        nextSong = (TextView) rootView.findViewById(R.id.nextSong);

        currentState = State.PAUSE;

        Button buttonNext = (Button) rootView.findViewById(R.id.ButtonNext);
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
                    sendToMediaService(MediaPlayerService.ACTION_GENRE, MediaPlayerService.EXTRA_RADIO, radioInfo);
                    sendToMediaService(MediaPlayerService.ACTION_PLAY);
                } else {
                    sendToMediaService(MediaPlayerService.ACTION_PAUSE);
                }
            }
        });


        seekBar = (SeekBar) rootView.findViewById(R.id.SeekBar);
        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                SeekBar sb = (SeekBar) v;
                sendToMediaService (MediaPlayerService.ACTION_SEEK, MediaPlayerService.EXTRA_SEEK, sb.getProgress());
                return false;
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
                    sendToMediaService(MediaPlayerService.ACTION_GENRE, MediaPlayerService.EXTRA_RADIO, radioInfo);
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
        if (extra != null && extraValue != null){
            intent.putExtra(MediaPlayerService.EXTRA_RADIO, radioInfo);
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
                    currentSong.setText(song.toString());
                    break;
                case MediaPlayerService.MSG_SET_DURATION:
                    seekBar.setMax((Integer) msg.obj);
                    seekBar.setProgress(0);
                    break;
                case MediaPlayerService.MSG_TRACK_LIST_CHANGES:
                    Collection<SongInfo> data = (Collection) msg.obj;
                    if (data != null && !data.isEmpty() && data.size() > 1) {
                        nextSong.setText(data.iterator().next().toString());
                    } else {
                        nextSong.setText(next);
                    }
                    break;
                case MediaPlayerService.MSG_PAUSE:
                    currentState = State.PAUSE;
                    buttonPlayStop.setText(play);
                    break;
                case MediaPlayerService.MSG_PLAY:
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
            }
        } catch (Exception e){ // eq fragment not connected to activity race for resources
            Log.e("RadioFragment", "on message handle", e);
        }
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