package io.daydev.vkrdo.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import io.daydev.vkrdo.MediaEvent;
import io.daydev.vkrdo.R;


public class HomeFragment extends Fragment {


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.home, container, false);

        final TextView title = (TextView) rootView.findViewById(R.id.artist);
        Button start = (Button)rootView.findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String artist = title.getText().toString();

                if (artist.isEmpty()){
                    artist = MediaEvent.DEFAULT_HOME_RADIO;
                }

                Intent intent = new Intent(MediaEvent.EVENT);
                intent.putExtra(MediaEvent.TYPE, MediaEvent.SIMPLE_RADIO);
                intent.putExtra(MediaEvent.SIMPLE_RADIO, artist);
                LocalBroadcastManager.getInstance(HomeFragment.this.getActivity()).sendBroadcast(intent);
            }
        });

        return rootView;
    }
}