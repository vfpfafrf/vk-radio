package io.daydev.vkrdo.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.session.MediaSession;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import io.daydev.vkrdo.MainActivity;
import io.daydev.vkrdo.R;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.service.MediaPlayerEvents;
import io.daydev.vkrdo.service.MediaPlayerService;
import io.daydev.vkrdo.util.SimpleDiskCache;

import java.net.URL;

/**
 * Helper class for media notifications: images, caches etc
 */
public class MediaNotification implements MediaPlayerEvents {

    private static final String TAG = "MediaNotification";

    private static final String EMPTY_KEY = "qwerty";

    private SimpleDiskCache diskCache;
    private Bitmap currentBitmap;
    private SongInfo currentSong;

    final private Context mediaService;

    private MediaNotificationCallback bitmapCallback;

    public MediaNotification(Context mediaService) {
        this.mediaService = mediaService;

        diskCache = new SimpleDiskCache(mediaService, "journal", 1024 * 1024 * 50, Bitmap.CompressFormat.JPEG, 90);
    }

    public void setBitmapCallback(MediaNotificationCallback bitmapCallback) {
        this.bitmapCallback = bitmapCallback;
    }

    private Notification.Action generateAction(Context applicationContext, int icon, String title, String intentAction) {
        Intent intent = new Intent(applicationContext, MediaPlayerService.class);
        intent.setAction(intentAction);
        PendingIntent pendingIntent = PendingIntent.getService(applicationContext, 1, intent, 0);
        return new Notification.Action.Builder(icon, title, pendingIntent).build();
    }

    public Notification.Action actionPlay(Context applicationContext){
        return generateAction(applicationContext, android.R.drawable.ic_media_play, "Play", ACTION_PLAY);
    }

    public Notification.Action actionPause(Context applicationContext){
        return generateAction(applicationContext, android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE);
    }

    public Notification.Action actionNext(Context applicationContext){
        return generateAction(applicationContext, android.R.drawable.ic_media_next, "Next", ACTION_NEXT);
    }


    public Notification.Builder buildNotification(final Context applicationContext, final Notification.Action action, final SongInfo songInfo, final MediaSession.Token mediaSession) {
        if (songInfo == null){
            return null;
        }

        Bitmap bitmap;

        if (currentSong != null && currentSong.equals(songInfo)) {
            bitmap = currentBitmap;
        } else {

            if (!cacheContains(songInfo) && songInfo.getArtistPhoto() != null && !songInfo.getArtistPhoto().isEmpty()) {
                try {

                    AsyncTask<String, String, Bitmap> task = new AsyncTask<String, String, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(String... params) {
                            try {
                                Bitmap image;
                                if (!cacheContains(songInfo)) {
                                    URL url = new URL(params[0]);
                                    image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                    if (image != null) {
                                        putCache(songInfo, image);
                                    }
                                } else {
                                    image = getCache(songInfo);
                                }

                                if (image != null && bitmapCallback != null) {
                                    if (bitmapCallback.imageCheck(songInfo, image)){
                                        buildImageNotification(applicationContext, action, songInfo, image, mediaSession);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "network error", e);
                            }
                            return null;
                        }
                    };
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, songInfo.getArtistPhoto());
                } catch (Exception e) {
                    Log.e(TAG, "download error", e);
                }
            }

            currentSong = songInfo;
            bitmap = getCache(songInfo);
        }

        return buildImageNotification(applicationContext, action, songInfo, bitmap, mediaSession);
    }

    private Notification.Builder buildImageNotification(Context applicationContext, Notification.Action action, SongInfo songInfo, Bitmap image, final MediaSession.Token mediaSession) {
        currentBitmap = image;
        bitmapCallback.notifyImage(image);

        Notification.MediaStyle style = new Notification.MediaStyle();
        style.setShowActionsInCompactView(0, 1);
        style.setMediaSession(mediaSession);

        // delete - eq stop event
        Intent intent = new Intent(applicationContext, MediaPlayerService.class);
        intent.setAction(ACTION_STOP);
        PendingIntent pendingIntent = PendingIntent.getService(applicationContext, 1, intent, 0);

        Notification.Builder builder = new Notification.Builder(mediaService)
                .setSmallIcon(R.drawable.ic_headphones)
                .setContentTitle(songInfo.getArtist())
                .setContentText(songInfo.getTitle())
                .setDeleteIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(style);

        if (currentBitmap != null) {
            builder.setLargeIcon(currentBitmap);
            //builder.setColor(BitmapUtil.getDominantColor(image));
        }

        // add actions -  play/pause and Next
        builder.addAction(action);
        builder.addAction(actionNext(applicationContext));

        // add to back stack
        Intent resultIntent = new Intent(mediaService, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mediaService);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);

        return builder;
    }

    public Bitmap getCurrentBitmap() {
        return currentBitmap;
    }

    // cache shits
    // ---------------------------------------------------------------------------------------------------------


    public boolean cacheContains(SongInfo songInfo) {
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
}
