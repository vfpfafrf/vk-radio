package io.daydev.vkrdo.notification;

import android.graphics.Bitmap;
import io.daydev.vkrdo.bean.SongInfo;

/**
 * Created by dmitry on 05.03.15.
 */
public interface MediaNotificationCallback {

    /**
     * try to setup new image
     * @param song for this song
     * @return true if should
     */
    public boolean imageCheck(SongInfo song, Bitmap image);

    /**
     * notify image changes
     * @param image Bitmap
     */
    public void notifyImage(Bitmap image);
}
