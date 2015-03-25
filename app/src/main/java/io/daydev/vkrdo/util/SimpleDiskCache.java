package io.daydev.vkrdo.util;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.*;

/**
 * Adapted from https://github.com/fhucho/simple-disk-cache
 * License Apache 2.0
 */
public class SimpleDiskCache {

    public static final int IO_BUFFER_SIZE = 8 * 1024;

    private DiskLruCache mDiskCache;
    private Bitmap.CompressFormat mCompressFormat = Bitmap.CompressFormat.JPEG;
    private int mCompressQuality = 70;
    private static final int APP_VERSION = 1;
    private static final int VALUE_COUNT = 1;

    private static final String TAG = "DiskLruImageCache";

    public static boolean isExternalStorageRemovable() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD || Environment.isExternalStorageRemovable();
    }

    public static File getExternalCacheDir(Context context) {
        if (hasExternalCacheDir()) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }

    public static boolean hasExternalCacheDir() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }


    public SimpleDiskCache(Context context, String uniqueName, int diskCacheSize, Bitmap.CompressFormat compressFormat, int quality) {
        try {
            final File diskCacheDir = getDiskCacheDir(context, uniqueName);
            mDiskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, diskCacheSize);
            mCompressFormat = compressFormat;
            mCompressQuality = quality;
        } catch (IOException e) {
            Log.e(TAG, "SimpleDiskCache", e);
        }
    }

    private boolean writeBitmapToFile(Bitmap bitmap, DiskLruCache.Editor editor) throws IOException {
        try (OutputStream out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE)){
            return bitmap.compress(mCompressFormat, mCompressQuality, out);
        }
    }

    private File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !isExternalStorageRemovable() ?
                        getExternalCacheDir(context).getPath() :
                        context.getCacheDir().getPath();

        return new File(cachePath + File.separator + uniqueName);
    }

    public void put(String key, Bitmap data) {
        key = clearKey(key);
        DiskLruCache.Editor editor = null;
        try {
            editor = mDiskCache.edit(key);
            if (editor == null) {
                return;
            }

            if (writeBitmapToFile(data, editor)) {
                mDiskCache.flush();
                editor.commit();
            } else {
                editor.abort();
            }
        } catch (IOException e) {
            try {
                if (editor != null) {
                    editor.abort();
                }
            } catch (IOException ignored) {
            }
        }

    }

    public Bitmap getBitmap(String key) {
        key = clearKey(key);
        Bitmap bitmap = null;
        try (DiskLruCache.Snapshot snapshot = mDiskCache.get(key)) {
            if (snapshot == null) {
                return null;
            }
            final InputStream in = snapshot.getInputStream(0);
            if (in != null) {
                final BufferedInputStream buffIn = new BufferedInputStream(in, IO_BUFFER_SIZE);
                bitmap = BitmapFactory.decodeStream(buffIn);
            }
        } catch (IOException e) {
            Log.e(TAG, "getBitmap", e);
        }

        return bitmap;

    }

    public boolean containsKey(String key) {
        key = clearKey(key);
        boolean contained = false;
        try (DiskLruCache.Snapshot snapshot = mDiskCache.get(key)) {
            contained = snapshot != null;
        } catch (IOException e) {
            Log.e(TAG, "containsKey", e);
        }
        return contained;

    }

    private String clearKey(String key) {
        return key == null  || key.isEmpty() ? key : key.replaceAll("[ \n\r]", "_");
    }

    public void clearCache() {
        try {
            mDiskCache.delete();
        } catch (IOException e) {
            Log.e(TAG, "clearCache", e);
        }
    }

    public File getCacheFolder() {
        return mDiskCache.getDirectory();
    }
}