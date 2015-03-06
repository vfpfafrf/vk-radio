package io.daydev.vkrdo.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import io.daydev.vkrdo.MediaEvent;
import io.daydev.vkrdo.R;
import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.notification.MediaNotification;
import io.daydev.vkrdo.notification.MediaNotificationCallback;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.CallbackChecker;

import java.io.Serializable;


/**
 * MediaPlayerService
 */
public class MediaPlayerService extends AbstractLocalBinderService implements MediaPlayerEvents, Callback<SongInfo>, CallbackChecker<SongInfo>, ServiceConnection {

    public static final String EXTRA_RADIO = "vk.radio";
    public static final String EXTRA_SEEK = "extra.seek";

    private MediaPlayer mediaPlayer;
    private MediaSession mediaSession;
    private MediaController mediaController;
    private MediaNotification mediaNotification;

    private PlayListService playListService;
    private boolean playListServiceBound = false;

    private boolean startedNewRadio = false;

    public static final int MSG_TRACK_LIST_CHANGES = 3;
    public static final int MSG_SET_DURATION = 4;
    public static final int MSG_SET_CURRENT_SONG = 5;
    public static final int MSG_PLAY = 6;
    public static final int MSG_PAUSE = 7;
    public static final int MSG_STOP = 8;
    public static final int MSG_PROGRESS = 9;
    public static final int MSG_SEEK = 10;
    public static final int MSG_ART = 11;

    /**
     * calls callback when song added
     *
     * @param song V result
     */
    @Override
    public void callback(SongInfo song) {
        sendMessage(MSG_TRACK_LIST_CHANGES, playListService.getToPlaySimpleFormat());
    }


    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null || playListService == null) {
            return;
        }

        String action = intent.getAction().toLowerCase();
        MediaController.TransportControls controls = mediaController.getTransportControls();

        switch (action) {
            case ACTION_PLAY:
                RadioInfo radio = (RadioInfo) intent.getSerializableExtra(EXTRA_RADIO);
                if (radio != null) {
                    if (playListService.generate(radio)) {
                        startedNewRadio = true;
                        playListService.setCallback(this);
                        playListService.setCallbackChecker(this);
                    }
                }
                controls.play();
                break;
            case ACTION_PAUSE:
                controls.pause();
                break;
            case ACTION_STOP:
                controls.stop();
                break;
            case ACTION_PREVIOUS:
                controls.skipToPrevious();
                break;
            case ACTION_NEXT:
                controls.skipToNext();
                break;
            case ACTION_SEEK:
                try {
                    Integer seek = (Integer) intent.getSerializableExtra(EXTRA_SEEK);
                    if (seek != null) {
                        controls.seekTo(seek);
                    }
                } catch (Exception e) {
                    Log.e("MediaPLayerService", "while seekTo", e);
                }
                break;
            case ACTION_STATUS:
                Log.e("omfg", "action status " + playListService.isPlayListStated());
                if (playListService.isPlayListStated()) {
                    sendMessage(MSG_TRACK_LIST_CHANGES, playListService.getToPlaySimpleFormat());
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        sendMessage(MSG_SET_DURATION, mediaPlayer.getDuration());
                    }
                    SongInfo song = playListService.getCurrent();
                    if (song != null) {
                        sendMessage(MSG_SET_CURRENT_SONG, song);
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            sendMessage(MSG_PLAY, song);
                        }
                    }
                    if (mediaNotification != null) {
                        sendMessage(MSG_ART, mediaNotification.getCurrentBitmap());
                    }
                }

        }
    }

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        if (!playListServiceBound) {
            Intent serviceIntent = new Intent(this, PlayListService.class);
            bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mediaSession == null) {
            initMediaSessions();
        }
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void initMediaSessions() {
        mediaNotification = new MediaNotification(this);
        mediaNotification.setBitmapCallback(new MediaNotificationCallback() {
            @Override
            public boolean imageCheck(SongInfo song, Bitmap image) {
                SongInfo current = playListService.getCurrent();
                return song != null && current != null && song.equals(current);
            }

            @Override
            public void notifyImage(Bitmap image) {
                sendMessage(MSG_ART, image);
            }
        });

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.w("mediaPlayer", "------------------------on complete--------");
                mediaController.getTransportControls().skipToNext();
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e("OMFG", "Ahtung ahtung"+what+" "+extra);
                sendError("MediaPlayer error what="+what+" extra="+extra);
                return false; //todo: handle error and return true)
            }
        });

        mediaSession = new MediaSession(getApplicationContext(), "VK radio media session");
        mediaController = new MediaController(getApplicationContext(), mediaSession.getSessionToken());
        mediaSession.setCallback(new MediaSession.Callback() {

            private volatile boolean paused = false;

            private final Handler handler = new Handler();

            private final Runnable progressNotification = new Runnable() {
                public void run() {
                    try {
                        if (mediaPlayer != null) {
                            sendMessage(MSG_PROGRESS, mediaPlayer.getCurrentPosition());
                            if (mediaPlayer.isPlaying()) {
                                handler.postDelayed(progressNotification, 1000);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("MediaPlayerService", "progressNotifycation", e);
                    }
                }
            };

            @Override
            public void onPlay() {
                super.onPlay();
                Log.e("MediaPlayerService", "onPlay ");
                if (paused && !startedNewRadio) { //resume from paused and new radio loading
                    try {
                        mediaPlayer.start();
                        SongInfo song = playListService.getCurrent();
                        if (song != null) {
                            buildNotification(mediaNotification.actionPause(getApplicationContext()), song);
                        }

                    } catch (IllegalStateException ignore) {
                        //
                    }
                } else {
                    playNext();
                }
                paused = false;
            }

            @Override
            public void onPause() {
                super.onPause();
                Log.e("MediaPlayerService", "onPause ");
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                }
                paused = true;
                sendMessage(MSG_PAUSE, null);
                SongInfo song = playListService.getCurrent();
                if (song != null) {
                    buildNotification(mediaNotification.actionPlay(getApplicationContext()), song);
                }
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                Log.e("MediaPlayerService", "onSkipToNext ");
                playNext();
            }

            @Override
            public void onStop() {
                super.onStop();
                Log.e("MediaPlayerService", "onStop ");
                sendMessage(MSG_STOP, null);

                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();

                NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(1);
                Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
                stopService(intent);
            }

            @Override
            public void onSeekTo(long pos) {
                super.onSeekTo(pos);
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(Long.valueOf(pos).intValue());
                }
            }

            private void playNext() {
                paused = false;
                final SongInfo song = playListService.next();
                Log.i("song", "get next song " + song.getArtist());
                try {
                    mediaPlayer.reset();
                    handler.removeCallbacks(progressNotification);

                    if (song.getLocation() == null) {
                        try (AssetFileDescriptor afd = getBaseContext().getResources().openRawResourceFd(R.raw.threesec)) {
                            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        }
                    } else {
                        mediaPlayer.setDataSource(song.getLocation());
                        sendMessage(MSG_TRACK_LIST_CHANGES, playListService.getToPlaySimpleFormat());
                        handler.postDelayed(progressNotification, 1000);
                    }

                    mediaPlayer.prepareAsync();
                    sendMessage(MSG_SET_CURRENT_SONG, song);
                    sendMessage(MSG_PLAY, song);

                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            Log.e("MediaPlayerService", "-------------on prepared-----------------------------" + song);
                            mediaPlayer.start();
                            startedNewRadio = false;
                            sendMessage(MSG_SET_DURATION, mediaPlayer.getDuration());
                            buildNotification(mediaNotification.actionPause(getApplicationContext()), song);
                        }
                    });


                } catch (Exception e) {
                    try {
                        mediaPlayer.pause();
                    } catch (Exception ignore) {
                        Log.e("MediaPlayerService", "while paause", ignore);
                    }
                    Log.e("MediaPlayerService", "while next", e);
                }

            }

        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (playListServiceBound) {
            unbindService(this);
            playListServiceBound = false;
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
            mediaPlayer.release();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mediaSession != null) {
            mediaSession.release();
        }
        if (playListServiceBound) {
            unbindService(this);
            playListServiceBound = false;
        }

        return super.onUnbind(intent);
    }


    /**
     * Called when a connection to the Service has been established, with
     * the {@link android.os.IBinder} of the communication channel to the
     * Service.
     *
     * @param name    The concrete component name of the service that has
     *                been connected.
     * @param service The IBinder of the Service's communication channel,
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        AbstractLocalBinderService.LocalBinder<PlayListService> binder = (AbstractLocalBinderService.LocalBinder<PlayListService>) service;
        playListService = binder.getService();
        playListServiceBound = true;

        Log.e("MediService", "onServiceConnected " + name);
    }

    /**
     * Called when a connection to the Service has been lost.  This typically
     * happens when the process hosting the service has crashed or been killed.
     * This does <em>not</em> remove the ServiceConnection itself -- this
     * binding to the service will remain active, and you will receive a call
     * to {@link #onServiceConnected} when the Service is next running.
     *
     * @param name The concrete component name of the service whose
     *             connection has been lost.
     */
    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e("MediService", "onServiceDisconnected " + name);
        playListServiceBound = false;
    }


    @Override
    public boolean check(SongInfo obj) {
        return mediaNotification != null && mediaNotification.cacheContains(obj);
    }


    /**
     * send message to clients
     *
     * @param msg  int code
     * @param data Object message data
     */
    protected void sendMessage(int msg, Object data) {
        Intent intent = new Intent(MediaEvent.EVENT);
        intent.putExtra(MediaEvent.DATA_RADIO, playListService.getCurrentRadio());
        intent.putExtra(MediaEvent.DATA_MESSAGE_CODE, msg);
        if (data instanceof Serializable) {
            intent.putExtra(MediaEvent.DATA_SERIALIZEBLE, (Serializable) data);
        } else if (data instanceof Parcelable) {
            intent.putExtra(MediaEvent.DATA_PARCEABLE, (Parcelable) data);
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    protected void sendError(String error){
        Intent intent = new Intent(MediaEvent.EVENT);
        intent.putExtra(MediaEvent.GLOBAL_ERROR, error);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void buildNotification(final Notification.Action action, final SongInfo songInfo) {
        if (mediaNotification == null || mediaSession == null) {
            return;
        }
        Notification.Builder builder = mediaNotification.buildNotification(getApplicationContext(), action, songInfo, mediaSession.getSessionToken());
        if (builder != null) {
            Notification notification = builder.build();
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(1, notification);

            //startForeground(1, notification);
        }
    }
}