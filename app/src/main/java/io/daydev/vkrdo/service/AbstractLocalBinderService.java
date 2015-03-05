package io.daydev.vkrdo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/**
 * Created by dmitry on 05.03.15.
 */
public abstract class AbstractLocalBinderService extends Service {

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder<T extends AbstractLocalBinderService> extends Binder {
        public  T getService() {
            // Return this instance of LocalService so clients can call public methods
            return getServiceForBinder();
        }
    }

    protected <T extends AbstractLocalBinderService> T getServiceForBinder(){
        return (T) this;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
