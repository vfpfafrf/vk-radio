package io.daydev.vkrdo.fragment;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import io.daydev.vkrdo.MediaEvent;
import io.daydev.vkrdo.R;
import io.daydev.vkrdo.bean.RadioBuilder;
import io.daydev.vkrdo.bean.RadioInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dmitry on 07.03.15.
 */
public class FavoriteFragment extends Fragment {

    public static final String FAV_PARAM = "fav";
    public static final String RADIO_TITLE = "favorites";

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.favorites, container, false);

        List<String> favs = null;
        Bundle arguments = getArguments();
        if (arguments != null) {
            favs = (List<String>) arguments.getSerializable(FAV_PARAM);
        }
        if (favs == null){
            favs = new ArrayList<>();
        }
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this.getActivity(), android.R.layout.simple_list_item_1, favs);

        ListView listView = (ListView)rootView.findViewById(R.id.favoritesList);
        listView.setAdapter(listAdapter);

        Button start = (Button)rootView.findViewById(R.id.play_fav);
        final List<String> finalFavs = favs;
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder artist = new StringBuilder();
                for(String favorite : finalFavs){
                    artist.append(favorite).append(",");
                }
                RadioInfo radioInfo = RadioBuilder.getInstance()
                        .setTitle(RADIO_TITLE)
                        .setArtist(artist.toString())
                        .build();

                Intent intent = new Intent(MediaEvent.EVENT);
                intent.putExtra(MediaEvent.TYPE, MediaEvent.SIMPLE_RADIO);
                intent.putExtra(MediaEvent.SIMPLE_RADIO, RADIO_TITLE);
                intent.putExtra(MediaEvent.REAL_RADIO, radioInfo);
                LocalBroadcastManager.getInstance(FavoriteFragment.this.getActivity()).sendBroadcast(intent);
            }
        });
        return rootView;
    }
}
