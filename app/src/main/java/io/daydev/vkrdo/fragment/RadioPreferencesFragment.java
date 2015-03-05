package io.daydev.vkrdo.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.support.v4.content.LocalBroadcastManager;
import io.daydev.vkrdo.MediaEvent;
import io.daydev.vkrdo.bean.RadioBuilder;
import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.preference.ButtonPreference;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.Tuple;


public class RadioPreferencesFragment extends PreferenceFragment {

    public static final String RADIO_PARAM = "radio";

    private static final String KEY_TITLE = "vkrdo.title";
    private static final String KEY_ARTIST = "vkrdo.artist";
    private static final String KEY_GENRE = "vkrdo.genre";
    private static final String KEY_MOOD = "vkrdo.genre";
    private static final String KEY_YEAR_FROM = "vkrdo.yfrom";
    private static final String KEY_YEAR_TO = "vkrdo.yto";
    private static final String KEY_PLAY = "vkrdo.play";

    private static final String[] ALL = new String[]{KEY_ARTIST,KEY_GENRE,KEY_TITLE,KEY_MOOD,KEY_YEAR_FROM,KEY_YEAR_TO, KEY_PLAY};

    private RadioInfo radioInfo;

    private Callback<Tuple<RadioInfo, RadioInfo>> settingsCallback;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        radioInfo= null;

        if (args != null){
            radioInfo = (RadioInfo) args.getSerializable(RADIO_PARAM);
        }

        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : ALL){
            editor.remove(key);
        }
        editor.commit();


        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this.getActivity());

        PreferenceCategory category = new PreferenceCategory(this.getActivity());
        category.setTitle("Common");
        screen.addPreference(category);

        EditTextPreference radioTitle = new EditTextPreference(this.getActivity());
        radioTitle.setKey(KEY_TITLE);
        radioTitle.setTitle("Radio title");
        radioTitle.setText(radioInfo == null ? "radio title to display" : radioInfo.getTitle());
        radioTitle.setSummary(radioInfo == null ? "radio title to display" : radioInfo.getTitle());
        radioTitle.setDefaultValue(radioInfo == null ? "" : radioInfo.getTitle());
        radioTitle.setOnPreferenceChangeListener(changeListener);
        category.addPreference(radioTitle);

        category = new PreferenceCategory(this.getActivity());
        category.setTitle("Genre");
        screen.addPreference(category);

        EditTextPreference genreTitle = new EditTextPreference(this.getActivity());
        genreTitle.setTitle("Radio genre");
        genreTitle.setKey(KEY_GENRE);
        genreTitle.setSummary(radioInfo == null ? "radio genre" : radioInfo.getGenre());
        genreTitle.setText(radioInfo == null ? "radio genre" : radioInfo.getGenre());
        genreTitle.setDefaultValue(radioInfo == null ? "" : radioInfo.getGenre());
        genreTitle.setOnPreferenceChangeListener(changeListener);
        category.addPreference(genreTitle);

        category = new PreferenceCategory(this.getActivity());
        category.setTitle("Artist");
        screen.addPreference(category);

        EditTextPreference artistTitle = new EditTextPreference(this.getActivity());
        artistTitle.setTitle("Artist");
        artistTitle.setKey(KEY_ARTIST);
        artistTitle.setSummary(radioInfo == null ? "artist name" : radioInfo.getArtist());
        artistTitle.setText(radioInfo == null ? "artist name" : radioInfo.getArtist());
        artistTitle.setDefaultValue(radioInfo == null ? "" : radioInfo.getArtist());
        artistTitle.setOnPreferenceChangeListener(changeListener);
        screen.addPreference(artistTitle);

        ButtonPreference preference = new ButtonPreference(this.getActivity());
        preference.setKey(KEY_PLAY);
        preference.setButtonCallback(new Callback<String>() {
            @Override
            public void callback(String obj) {
                if (obj.equalsIgnoreCase(ButtonPreference.CMD_PLAY)) {
                    if (radioInfo == null){
                        radioInfo = RadioBuilder.buildDefault();
                    }
                    Intent intent = new Intent(MediaEvent.EVENT);
                    intent.putExtra(MediaEvent.SIMPLE_RADIO, radioInfo.getTitle());
                    intent.putExtra(MediaEvent.REAL_RADIO, radioInfo);
                    LocalBroadcastManager.getInstance(RadioPreferencesFragment.this.getActivity()).sendBroadcast(intent);
                } else if (obj.equalsIgnoreCase(ButtonPreference.CMD_REMOVE)){
                    //remove radio and go to home screen
                    Intent intent = new Intent(MediaEvent.EVENT);
                    intent.putExtra(MediaEvent.RADIO_REMOVE, radioInfo == null ? MediaEvent.MAGIC_HOME : radioInfo.getTitle());
                    LocalBroadcastManager.getInstance(RadioPreferencesFragment.this.getActivity()).sendBroadcast(intent);
                }
            }
        });
        screen.addPreference(preference);
        setPreferenceScreen(screen);
    }

    Preference.OnPreferenceChangeListener changeListener = new  Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            RadioInfo newRadio;
            if (radioInfo == null){
                newRadio = RadioBuilder.getInstance().build();
            } else {
                newRadio = RadioBuilder.clone(radioInfo);
            }
            String stringValue = newValue == null ? null : newValue.toString();
            switch (key){
                case KEY_TITLE:
                    newRadio.setTitle(stringValue);
                    break;
                case KEY_GENRE:
                    newRadio.setGenre(stringValue);
                    break;
                case KEY_ARTIST:
                    newRadio.setArtist(stringValue);
                    break;
            }

            if (settingsCallback != null){
                settingsCallback.callback(new Tuple<>(radioInfo, newRadio));
                radioInfo = newRadio;
            }
/*
            if (preference instanceof ListPreference) {
                if (!TextUtils.isEmpty(stringValue)) {
                    preference.setSummary(stringValue);
                }
            } else if (preference instanceof EditTextPreference){
                if (!TextUtils.isEmpty(stringValue)){
                    preference.setSummary(stringValue);
                }
            }*/

            preference.setSummary(stringValue);
            return false;
        }
    };

    public void setSettingsCallback(Callback<Tuple<RadioInfo, RadioInfo>> settingsCallback) {
        this.settingsCallback = settingsCallback;
    }

}
