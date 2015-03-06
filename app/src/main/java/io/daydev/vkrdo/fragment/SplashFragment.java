package io.daydev.vkrdo.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.daydev.vkrdo.MediaEvent;
import io.daydev.vkrdo.R;

/**
 * Splash screen fragment - send event to display "home" radio after second
 */
public class SplashFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.splash, container, false);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MediaEvent.EVENT);
                intent.putExtra(MediaEvent.TYPE, MediaEvent.SIMPLE_RADIO);
                intent.putExtra(MediaEvent.SIMPLE_RADIO, MediaEvent.MAGIC_HOME);
                LocalBroadcastManager.getInstance(SplashFragment.this.getActivity()).sendBroadcast(intent);
            }
        }, 1000);

        return root;
    }

}
