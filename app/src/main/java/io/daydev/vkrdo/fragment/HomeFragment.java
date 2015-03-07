package io.daydev.vkrdo.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.TextView;
import io.daydev.vkrdo.MediaEvent;
import io.daydev.vkrdo.R;


public class HomeFragment extends Fragment {


    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.home, container, false);

        final TextView title = (TextView) rootView.findViewById(R.id.artist);
        ImageButton start = (ImageButton)rootView.findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String artist = title.getText().toString();

                if (artist.isEmpty()){
                    artist = MediaEvent.DEFAULT_HOME_RADIO;
                }

                InputMethodManager imm = (InputMethodManager)HomeFragment.this.getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(title.getWindowToken(), 0);


                Intent intent = new Intent(MediaEvent.EVENT);
                intent.putExtra(MediaEvent.TYPE, MediaEvent.SIMPLE_RADIO);
                intent.putExtra(MediaEvent.SIMPLE_RADIO, artist);
                LocalBroadcastManager.getInstance(HomeFragment.this.getActivity()).sendBroadcast(intent);
            }
        });

        return rootView;
    }
}