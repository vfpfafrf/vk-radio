package io.daydev.vkrdo.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import io.daydev.vkrdo.MainActivity;
import io.daydev.vkrdo.MediaEvent;
import io.daydev.vkrdo.R;
import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.CallbackChecker;
import io.daydev.vkrdo.util.SimpleDiskCache;

import java.io.Serializable;
import java.net.URL;


/**
 * MediaPlayerService
 */
public class MediaPlayerService extends AbstractLocalBinderService implements Callback<SongInfo>, CallbackChecker<SongInfo>, ServiceConnection {

    private static final String EMPTY_KEY = "qwerty";

    public static final String ACTION_EMPTY = "action_empty";
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";
    public static final String ACTION_GENRE = "action_genre";
    public static final String ACTION_SEEK = "action_seek";
    public static final String ACTION_STATUS = "action_status";

    public static final String EXTRA_RADIO = "vk.radio";
    public static final String EXTRA_SEEK = "extra.seek";

    private MediaPlayer mediaPlayer;
    private MediaSession mediaSession;
    private MediaController mediaController;

    private final Handler handler = new Handler();


    private PlayListService playListService;
    private boolean playListServiceBound = false;

    private volatile  boolean waiting = false;
    private Bitmap currentBitmap;
    private RadioInfo radio;

    public static final int MSG_TRACK_LIST_CHANGES = 3;
    public static final int MSG_SET_DURATION = 4;
    public static final int MSG_SET_CURRENT_SONG = 5;
    public static final int MSG_PLAY = 6;
    public static final int MSG_PAUSE = 7;
    public static final int MSG_STOP = 8;
    public static final int MSG_PROGRESS = 9;
    public static final int MSG_SEEK = 10;
    public static final int MSG_ART = 11;
    public static final int MSG_NEXT = 12;
    public static final int MSG_STATUS = 13;

    private SimpleDiskCache diskCache;


    private final Runnable progressNotification = new Runnable() {
        public void run() {
            sendMessage(MSG_PROGRESS, mediaPlayer.getCurrentPosition());
            if (mediaPlayer.isPlaying()) {
                handler.postDelayed(progressNotification, 1000);
            }
        }
    };


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
        switch (action){
            case ACTION_PLAY:
                if (playListService.generate(radio)) {
                    playListService.setCallback(this);
                    playListService.setCallbackChecker(this);
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
                if (!waiting){
                    controls.skipToNext();
                }
                break;
            case ACTION_GENRE:
                if (radio != null){
                    sendMessage(MSG_STOP, null); // send message - current raio stop playing....
                    controls.stop();
                }
                radio = (RadioInfo) intent.getSerializableExtra(EXTRA_RADIO);
                break;
            case ACTION_SEEK:
                try {
                    Integer seek = (Integer) intent.getSerializableExtra(ACTION_SEEK);
                    if (seek != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.seekTo(seek);
                    }
                }catch (Exception e){
                    Log.e("MediaPLayerService", "while seekTo", e);
                }
                break;
            case ACTION_STATUS:
                if (playListService.isPlayListStated()) {
                    sendMessage(MSG_TRACK_LIST_CHANGES, playListService.getToPlaySimpleFormat());
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        sendMessage(MSG_SET_DURATION, mediaPlayer.getDuration());
                    }
                    SongInfo song = playListService.getCurrent();
                    if (song != null) {
                        sendMessage (MSG_SET_CURRENT_SONG, song);
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            sendMessage(MSG_PLAY, song);
                        }
                    }
                    sendMessage(MSG_ART, currentBitmap);
                }

        }
    }

    private Notification.Action generateAction(int icon, String title, String intentAction) {
        Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        return new Notification.Action.Builder(icon, title, pendingIntent).build();
    }

    private void buildNotification(final Notification.Action action, final SongInfo songInfo) {
        if (songInfo == null){
            return;
        }
        if (!cacheContains(songInfo) && songInfo.getArtistPhoto() != null && !songInfo.getArtistPhoto().isEmpty()) {
            try {

                AsyncTask<String, String, Bitmap> task = new AsyncTask<String, String, Bitmap>() {
                    @Override
                    protected Bitmap doInBackground(String... params) {
                        try {
                            Bitmap image;
                            if (!cacheContains(songInfo)){
                                URL url = new URL(params[0]);
                                image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                if (image != null) {
                                    putCache(songInfo, image);
                                }
                            } else {
                                image = getCache(songInfo);
                            }

                            if (image != null){
                                SongInfo song = playListService.getCurrent();
                                if (song != null && song.equals(songInfo)) {
                                    buildImageNotification(action, songInfo.getArtist(), songInfo.getTitle(), image);
                                    sendMessage(MSG_ART, image);
                                }
                            }

                        } catch (Exception e) {
                            Log.e("MediaPlayerService", "network error", e);
                        }
                        return null;
                    }
                };
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, songInfo.getArtistPhoto());
            } catch (Exception e) {
                Log.e("MediaPlayerService", "network error", e);
            }
        }
        Bitmap bitmap = getCache(songInfo);
        buildImageNotification(action, songInfo.getArtist(), songInfo.getTitle(), bitmap);
        sendMessage(MSG_ART, bitmap);
    }

    private void buildImageNotification(Notification.Action action, String artist, String track, Bitmap image) {
        currentBitmap = image;
        Notification.MediaStyle style = new Notification.MediaStyle();
        Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
        intent.setAction(ACTION_STOP);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_headphones)
                .setContentTitle(artist)
                .setContentText(track)
                .setDeleteIntent(pendingIntent)
                .setStyle(style);

        if (image != null) {
            builder.setLargeIcon(image);
            //builder.setColor(BitmapUtil.getDominantColor(image));
        }
        builder.addAction(action);
        builder.addAction(generateAction(android.R.drawable.ic_media_next, "Next", ACTION_NEXT));
        style.setShowActionsInCompactView(0, 1);
        style.setMediaSession(mediaSession.getSessionToken());

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        Intent intent = new Intent(this, PlayListService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);

        diskCache = new SimpleDiskCache(this, "journal", 1024 * 1024 * 50, Bitmap.CompressFormat.JPEG, 90);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mediaPlayer == null) {
            initMediaSessions();
        }
        handleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void initMediaSessions() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaController.getTransportControls().skipToNext();
            }
        });


        mediaSession = new MediaSession(getApplicationContext(), "VK radio media session");
        mediaController = new MediaController(getApplicationContext(), mediaSession.getSessionToken());
        mediaSession.setCallback(new MediaSession.Callback() {

                                     @Override
                                     public void onPlay() {
                                         super.onPlay();
                                         Log.e("MediaPlayerService", "onPlay");
                                         playNext();
                                     }

                                     @Override
                                     public void onPause() {
                                         super.onPause();
                                         Log.e("MediaPlayerService", "onPause");
                                         mediaPlayer.pause();
                                         sendMessage(MSG_PAUSE, null);
                                         SongInfo song = playListService.getCurrent();
                                         if (song != null) {
                                             buildImageNotification(generateAction(android.R.drawable.ic_media_play, "Play", ACTION_PLAY), song.getArtist(), song.getTitle(), currentBitmap);
                                         }
                                     }

                                     @Override
                                     public void onSkipToNext() {
                                         super.onSkipToNext();
                                         Log.e("MediaPlayerService", "onSkipToNext");
                                         playNext();

                                     }

                                     @Override
                                     public void onStop() {
                                         super.onStop();
                                         Log.e("MediaPlayerService", "onStop");
                                         sendMessage(MSG_STOP, null);

                                         mediaPlayer.release();

                                         NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                                         notificationManager.cancel(1);
                                         Intent intent = new Intent(getApplicationContext(), MediaPlayerService.class);
                                         stopService(intent);
                                     }

                                     private void playNext() {
                                         final SongInfo song = playListService.next();
                                         try {
                                             try {
                                                 mediaPlayer.reset();
                                             }catch (Exception ignore){
                                                 //ignore
                                             }
                                             handler.removeCallbacks(progressNotification);

                                             if (song.getLocation() == null) {
                                                 try(AssetFileDescriptor afd = getBaseContext().getResources().openRawResourceFd(R.raw.threesec)) {
                                                     mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                                                 }
                                                 waiting = true;
                                             } else {
                                                 waiting = false;
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
                                                     mediaPlayer.start();

                                                     sendMessage(MSG_SET_DURATION, mediaPlayer.getDuration());
                                                     buildNotification(generateAction(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE), song);
                                                 }
                                             });


                                         } catch (Exception e) {
                                             try {
                                                 mediaPlayer.pause();
                                             }catch (Exception ignore){
                                                 //ignore
                                             }
                                             Log.e("MediaPlayerService", "while next", e);
                                         }

                                     }

                                 }
        );
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
        playListService =  binder.getService();
        playListServiceBound = true;
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
        playListServiceBound = false;
    }


    @Override
    public boolean check(SongInfo obj) {
        return diskCache != null && cacheContains(obj);
    }

    private boolean cacheContains(SongInfo songInfo) {
        if (songInfo == null){
            return false;
        }
        String cacheKey = buildCacheKey(songInfo);
        String caheKey2 = buildCacheKeyL2(songInfo);
        return (!cacheKey.equals(EMPTY_KEY) && (diskCache.containsKey(cacheKey)) || (!caheKey2.equals(EMPTY_KEY)  && diskCache.containsKey(caheKey2))) ;
    }

    private Bitmap getCache(SongInfo songInfo){
        if (!cacheContains(songInfo)){
            return null;
        }
        String cacheKey = buildCacheKey(songInfo);
        String cacheKey2 = buildCacheKeyL2(songInfo);
        Bitmap ret = null;
        if (!cacheKey2.equals(EMPTY_KEY)){
            ret = diskCache.getBitmap(cacheKey2);
        }
        if (ret == null && !cacheKey.equals(EMPTY_KEY)){
            ret = diskCache.getBitmap(cacheKey);
        }
        return ret;
    }

    private void putCache(SongInfo songInfo, Bitmap bitmap){
        String cache2 = buildCacheKeyL2(songInfo);
        if (!cache2.equals(EMPTY_KEY)) {
            diskCache.put(cache2, bitmap);
        } else {
            String cache = buildCacheKey(songInfo);
            if (!cache.equals(EMPTY_KEY)){
                diskCache.put(cache, bitmap);
            }
        }
    }

    private String buildCacheKey(SongInfo songInfo){
        return songInfo == null ? EMPTY_KEY : buildCacheKey(songInfo.getArtist(), songInfo.getTitle());
    }

    private String buildCacheKeyL2 (SongInfo songInfo){
        return songInfo == null || songInfo.getAlbum() == null || songInfo.getAlbum().isEmpty() ? EMPTY_KEY : buildCacheKey(songInfo.getArtist(), songInfo.getAlbum());
    }

    private String buildCacheKey(String artist, String title){
        if (artist == null || title == null){
            return EMPTY_KEY;
        }
        return artist+ " "+title;
    }

    /**
     * send message to clients
     *
     * @param msg  int code
     * @param data Object message data
     */
    protected void sendMessage(int msg, Object data) {
        Intent intent = new Intent(MediaEvent.EVENT);
        intent.putExtra(MediaEvent.DATA_RADIO, radio);
        intent.putExtra(MediaEvent.DATA_MESSAGE_CODE, msg);
        if (data instanceof Serializable) {
            intent.putExtra(MediaEvent.DATA_SERIALIZEBLE, (Serializable) data);
        } else if (data instanceof Parcelable){
            intent.putExtra(MediaEvent.DATA_PARCEABLE, (Parcelable) data);
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}