package io.daydev.vkrdo.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.support.v4.content.LocalBroadcastManager;
import io.daydev.vkrdo.MediaEvent;
import io.daydev.vkrdo.R;
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
    private static final String KEY_MOOD = "vkrdo.mood";
    private static final String KEY_YEAR_FROM = "vkrdo.yfrom";
    private static final String KEY_YEAR_TO = "vkrdo.yto";
    private static final String KEY_PLAY = "vkrdo.play";
    private static final String KEY_LINK = "vkrdo.link";

    private static final String[] ALL = new String[]{KEY_ARTIST, KEY_GENRE, KEY_TITLE, KEY_MOOD, KEY_YEAR_FROM, KEY_YEAR_TO, KEY_PLAY, KEY_LINK};

    private RadioInfo radioInfo;

    private Callback<Tuple<RadioInfo, RadioInfo>> settingsCallback;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        radioInfo = null;

        if (args != null) {
            radioInfo = (RadioInfo) args.getSerializable(RADIO_PARAM);
        }

        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : ALL) {
            editor.remove(key);
        }
        editor.apply();

        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this.getActivity());
        screen.addPreference(createEditPreference(KEY_TITLE,
                radioInfo == null ? "radio title to display" : radioInfo.getTitle(),
                radioInfo == null ? "" : radioInfo.getTitle(),
                R.string.pref_title));

        PreferenceCategory category = createCategory(R.string.pref_category_genre);
        screen.addPreference(category);

        category.addPreference(createEditPreference(KEY_GENRE,
                radioInfo == null ? "radio genre" : radioInfo.getGenre(),
                radioInfo == null ? "" : radioInfo.getGenre(),
                R.string.pref_genre
                ));

        category = createCategory(R.string.pref_category_artist);
        screen.addPreference(category);

        category.addPreference(createEditPreference(KEY_ARTIST,
                radioInfo == null ? "artist name" : radioInfo.getArtist(),
                radioInfo == null ? "" : radioInfo.getArtist(),
                R.string.pref_artist
        ));

        CharSequence[] values = getResources().getStringArray(R.array.linkArtist);
        category.addPreference(createListPreference(KEY_LINK,
                values,
                getResources().getStringArray(R.array.linkArtistValues),
                radioInfo == null ? "1" : Integer.valueOf(radioInfo.getArtistLinkType().ordinal() + 1).toString(),
                (radioInfo == null ? "type" : values[radioInfo.getArtistLinkType().ordinal()]).toString(),
                R.string.pref_link
        ));

/*
        mood looks broken
        values = getResources().getStringArray(R.array.moodList);
        category.addPreference(createListPreference(KEY_MOOD,
                values,
                values,
                radioInfo == null || radioInfo.getMood() == null || radioInfo.getMood().isEmpty() ? "EMPTY" : radioInfo.getMood(),
                radioInfo == null || radioInfo.getMood() == null || radioInfo.getMood().isEmpty() ? "radio mood" : radioInfo.getMood(),
                R.string.pref_link
        ));
*/

        category = createCategory(R.string.pref_category_common);
        screen.addPreference(category);

        category.addPreference(createEditPreference(KEY_YEAR_FROM,
                radioInfo == null ? "year of artist from" : radioInfo.getYearFromAsString(),
                radioInfo == null ? "" : radioInfo.getYearFromAsString(),
                R.string.pref_yf
        ));

        category.addPreference(createEditPreference(KEY_YEAR_TO,
                radioInfo == null ? "year of artist to" : radioInfo.getYearToAsString(),
                radioInfo == null ? "" : radioInfo.getYearToAsString(),
                R.string.pref_yt
        ));

        ButtonPreference preference = new ButtonPreference(this.getActivity());
        preference.setKey(KEY_PLAY);
        preference.setButtonCallback(new Callback<String>() {
            @Override
            public void callback(String obj) {
                if (obj.equalsIgnoreCase(ButtonPreference.CMD_PLAY)) {
                    if (radioInfo == null) {
                        radioInfo = RadioBuilder.buildDefault();
                    }
                    Intent intent = new Intent(MediaEvent.EVENT);
                    intent.putExtra(MediaEvent.SIMPLE_RADIO, radioInfo.getTitle());
                    intent.putExtra(MediaEvent.REAL_RADIO, radioInfo);
                    LocalBroadcastManager.getInstance(RadioPreferencesFragment.this.getActivity()).sendBroadcast(intent);
                } else if (obj.equalsIgnoreCase(ButtonPreference.CMD_REMOVE)) {
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

    Preference.OnPreferenceChangeListener changeListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            RadioInfo newRadio;
            if (radioInfo == null) {
                newRadio = RadioBuilder.getInstance().build();
            } else {
                newRadio = RadioBuilder.clone(radioInfo);
            }
            String stringValue = newValue == null ? null : newValue.toString();
            String defaultValue = stringValue;

            switch (key) {
                case KEY_TITLE:
                    newRadio.setTitle(stringValue);
                    break;
                case KEY_GENRE:
                    newRadio.setGenre(stringValue);
                    break;
                case KEY_ARTIST:
                    newRadio.setArtist(stringValue);
                    break;
                case KEY_LINK:
                    if ("1".equalsIgnoreCase(stringValue)){
                        newRadio.setArtistLinkType(RadioInfo.ArtistLinkType.SIMILAR);
                    } else {
                        newRadio.setArtistLinkType(RadioInfo.ArtistLinkType.LIMIT);
                    }
                    break;
                case KEY_MOOD:
                    newRadio.setMood(stringValue);
                    break;
                case KEY_YEAR_FROM:
                    try{
                        Integer value = Integer.valueOf(stringValue);
                        newRadio.setYearFrom(value);
                    } catch (NumberFormatException ignore){
                        stringValue = "";
                        defaultValue = "";
                    }
                    break;
                case KEY_YEAR_TO:
                    try{
                        Integer value = Integer.valueOf(stringValue);
                        newRadio.setYearTo(value);
                    } catch (NumberFormatException ignore){
                        stringValue = "";
                        defaultValue = "";
                    }
                    break;
            }

            if (settingsCallback != null) {
                settingsCallback.callback(new Tuple<>(radioInfo, newRadio));
                radioInfo = newRadio;
            }

            if (preference instanceof ListPreference) {
                try {
                    Integer value = Integer.valueOf(stringValue);
                    defaultValue = stringValue;
                    stringValue = ((ListPreference) preference).getEntries()[value-1].toString();
                    ((ListPreference) preference).setValue(defaultValue);
                } catch (NumberFormatException ignore){
                    //
                }
            }

            preference.setSummary(stringValue);
            preference.setDefaultValue(defaultValue);
            return false;
        }
    };

    public void setSettingsCallback(Callback<Tuple<RadioInfo, RadioInfo>> settingsCallback) {
        this.settingsCallback = settingsCallback;
    }

    private EditTextPreference createEditPreference(String key, String value, String defaultValue, int resourceTitle){
        EditTextPreference preference = new EditTextPreference(this.getActivity());
        preference.setTitle(resourceTitle);
        preference.setKey(key);
        preference.setSummary(value);
        preference.setText(value);
        preference.setDefaultValue(defaultValue);
        preference.setOnPreferenceChangeListener(changeListener);
        return preference;
    }

    private PreferenceCategory createCategory(int resourceTitle){
        PreferenceCategory category = new PreferenceCategory(this.getActivity());
        category.setTitle(resourceTitle);
        return category;
    }

    private ListPreference createListPreference(String key, CharSequence[] entries, CharSequence[] entriesValues, String value, String summary, int resourceTitle){
        ListPreference preference = new ListPreference(this.getActivity());
        preference.setTitle(resourceTitle);
        preference.setKey(key);
        preference.setSummary(summary);
        preference.setDefaultValue(value);
        preference.setValue(value);
        preference.setEntries(entries);
        preference.setEntryValues(entriesValues);
        preference.setOnPreferenceChangeListener(changeListener);
        return preference;
    }

}
