package io.daydev.vkrdo;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.vk.sdk.VKUIHelper;
import io.daydev.vkrdo.bean.RadioBuilder;
import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.bean.SongInfo;
import io.daydev.vkrdo.external.ConfigurationHolder;
import io.daydev.vkrdo.fragment.*;
import io.daydev.vkrdo.preference.PreferenceHelper;
import io.daydev.vkrdo.service.MediaPlayerService;
import io.daydev.vkrdo.slide.NavDrawerItem;
import io.daydev.vkrdo.slide.NavDrawerListAdapter;
import io.daydev.vkrdo.slide.SlidePreferencesHelper;
import io.daydev.vkrdo.util.Callback;
import io.daydev.vkrdo.util.CallbackChecker;
import io.daydev.vkrdo.util.ResultTuple;
import io.daydev.vkrdo.util.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends VKActivity implements MediaEvent {

    private static final String PREF_NAME = "vkrdo";
    private static final String PREF_RADIO = "myradio";
    private static final String PREF_FAV = "myfav";

    private static final int POSITION_ACCURATE = 2;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout rightDrawerLayout;
    private ListView rightDrawerList;
    private ActionBarDrawerToggle rightDrawerToggle;


    // nav drawer title
    private CharSequence mDrawerTitle;

    // used to store app title
    private CharSequence mTitle;

    // slide menu items
    private SlidePreferencesHelper slidePreferencesHelper;

    private Callback<Message> currentFragmentCallback;
    private CallbackChecker<RadioInfo> currentFragmentRadioChecker;
    private RadioInfo currentRadio;

    private NavDrawerListAdapter slideAdapter;
    private NavDrawerListAdapter rightAdapter;


    private enum State {
        HOME, SETTINGS, RADIO, FAV
    }

    private State currentState;

    private boolean preventBack = false;

    private io.daydev.vkrdo.bean.Configuration configuration;
    private static final String CONFIG = "http://78.47.96.12/public/vkrdo.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (configuration == null) {
            ConfigurationHolder task = new ConfigurationHolder();
            task.load(CONFIG, new Callback<ResultTuple<io.daydev.vkrdo.bean.Configuration>>() {
                @Override
                public void callback(ResultTuple<io.daydev.vkrdo.bean.Configuration> result) {
                    if (result != null && result.hasResult()) {
                        configuration = result.getResult();
                    } else {
                        displayError(result == null ? "" : result.getError());
                    }
                }
            });
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter(MediaEvent.EVENT));

        Intent intent = new Intent(this, MediaPlayerService.class);
        intent.setAction(MediaPlayerService.ACTION_EMPTY);
        startService(intent);

        intent = new Intent(this, MediaPlayerService.class);
        intent.setAction(MediaPlayerService.ACTION_STATUS);
        startService(intent);

        mTitle = mDrawerTitle = getTitle();
        currentState = null;
        preventBack = false;

        //PreferenceHelper.clean(getSharedPreferences(PREF_NAME, MODE_PRIVATE), PREF_RADIO);
        slidePreferencesHelper = new SlidePreferencesHelper();
        slidePreferencesHelper.onCreate(PreferenceHelper.getMap(getSharedPreferences(PREF_NAME, MODE_PRIVATE), PREF_RADIO),
                PreferenceHelper.getCollection(getSharedPreferences(PREF_NAME, MODE_PRIVATE), PREF_FAV));

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.list_slidermenu);

        rightDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        rightDrawerList = (ListView) findViewById(R.id.right_drawer);

        mDrawerList.setOnItemClickListener(new SlideMenuClickListener());
        rightDrawerList.setOnItemClickListener(new RightMenuClickListener());

        List<NavDrawerItem> navDrawerItems = new ArrayList<>();
        navDrawerItems.add(new NavDrawerItem(getString(R.string.add_radio_slide), android.R.drawable.ic_input_add));
        navDrawerItems.add((new NavDrawerItem("Favorite", android.R.drawable.star_on)));
        navDrawerItems.addAll(slidePreferencesHelper.convertToNavDrawItems());

        // setting the nav drawer list adapter
        slideAdapter = new NavDrawerListAdapter(getApplicationContext(), navDrawerItems);
        mDrawerList.setAdapter(slideAdapter);

        rightAdapter = new NavDrawerListAdapter(getApplicationContext(), new ArrayList<NavDrawerItem>());
        rightDrawerList.setAdapter(rightAdapter);

        // enabling action bar app icon and behaving it as toggle button
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, //nav menu toggle icon
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                preventBack = false;
                // calling onPrepareOptionsMenu() to show action bar icons
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                preventBack = true;
                // calling onPrepareOptionsMenu() to hide action bar icons
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        rightDrawerToggle = new ActionBarDrawerToggle(this, rightDrawerLayout,
                R.drawable.ic_drawer, //nav menu toggle icon
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                preventBack = false;
                // calling onPrepareOptionsMenu() to show action bar icons
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                preventBack = true;
                // calling onPrepareOptionsMenu() to hide action bar icons
                invalidateOptionsMenu();
            }
        };
        rightDrawerLayout.setDrawerListener(rightDrawerToggle);

        if (savedInstanceState == null) {
            // on first time display view for first nav item
            SplashFragment fragment = new SplashFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_container, fragment)
                    .commit();
        }
        VKUIHelper.onCreate(this);

        IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headset, receiverFilter);

    }

    private final BroadcastReceiver headset = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch(state) {
                    case 0: // unplugged - send pause event
                        Intent pauseIntent = new Intent(MediaEvent.EVENT);
                        pauseIntent.putExtra(MediaEvent.TYPE, MediaEvent.MEDIAPLAYER_COMMAND);

                        pauseIntent.putExtra(MediaEvent.DATA_RADIO, currentRadio);
                        pauseIntent.putExtra(MediaEvent.DATA_MESSAGE_CODE, MediaPlayerService.MSG_PAUSE);
                        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
                        break;
                    case 1: // plugged - do nothing
                        break;

                }
            }


        }
    };

    /**
     * Slide menu item click listener
     */
    private class SlideMenuClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            slideAdapter.setIcon(currentRadio, android.R.drawable.ic_media_pause);
            // display view for selected nav drawer item
            displayView(position);
        }
    }

    private class RightMenuClickListener implements ListView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position != 1){ //if not text "similar artist" - start radio & play!
                String radio = rightAdapter.getItem(position).getTitle();

                RadioInfo radioInfo = RadioBuilder.getInstance()
                        .setTitle(radio)
                        .setArtist(radio)
                        .setArtistLinkType(RadioInfo.ArtistLinkType.LIMIT)
                        .build();

                Intent intent = new Intent(MediaEvent.EVENT);
                intent.putExtra(MediaEvent.TYPE, MediaEvent.SIMPLE_RADIO);
                intent.putExtra(MediaEvent.SIMPLE_RADIO, RADIO_TITLE);
                intent.putExtra(MediaEvent.REAL_RADIO, radioInfo);

                LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action bar actions click
        switch (item.getItemId()) {
            case R.id.action_settings:
                if (!State.HOME.equals(currentState)) {
                    radioSettings(currentRadio);
                }
                return true;
            case R.id.action_fav:
                if (State.RADIO.equals(currentState)) {
                    TextView currentText = (TextView) findViewById(R.id.currentSong);
                    if (currentText != null){
                        String artist = currentText.getText().toString();
                        Log.e("fav", artist);
                        if (slidePreferencesHelper.addToFav(artist)){
                            item.setIcon(android.R.drawable.star_on);
                        } else {
                            item.setIcon(android.R.drawable.star_off);
                            slidePreferencesHelper.removeFromFav(artist);
                        }
                        PreferenceHelper.saveCollection(getSharedPreferences(PREF_NAME, MODE_PRIVATE), PREF_FAV, slidePreferencesHelper.getFavoritesArtists());
                    }
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called when invalidateOptionsMenu() is triggered
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // if nav drawer is opened, hide the action items
        if (menu != null) {
            boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
            if (menu.findItem(R.id.action_settings) != null) {
                menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
            }
            if (menu.findItem(R.id.action_fav) != null) {
                menu.findItem(R.id.action_fav).setVisible(!drawerOpen);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Diplaying fragment view for selected nav drawer list item
     */
    private void displayView(int position) {
        displayView(position, null);
    }

    private void displayView(int position, Serializable extra) {
        // update the main content by replacing fragments
        Fragment fragment = null;
        Bundle bundle = null;
        String title = getString(R.string.app_name);

        currentFragmentCallback = null;
        currentFragmentRadioChecker = null;

        switch (position) {
            case -1:
                initSdk(configuration);
                fragment = new HomeFragment();
                currentState = State.HOME;
                break;
            case 0:
                radioSettings(null);
                title = getString(R.string.new_radio_title);
                break;
            case 1:
                fragment = new FavoriteFragment();
                title = getString(R.string.fav_title);
                currentState = State.FAV;
                bundle = new Bundle();
                bundle.putStringArrayList(FavoriteFragment.FAV_PARAM, slidePreferencesHelper.getFavoritesArtists());
                break;
            default:
                currentState = State.RADIO;
                fragment = new RadioFragment();
                currentFragmentCallback = (Callback<Message>) fragment;
                currentFragmentRadioChecker = (CallbackChecker<RadioInfo>) fragment;

                currentRadio = slidePreferencesHelper.getByPosition(position-POSITION_ACCURATE);
                title = currentRadio.getTitle();

                bundle = new Bundle();
                bundle.putSerializable(RadioFragment.RADIO_PARAM, currentRadio);
                if (extra != null) {
                    bundle.putSerializable(RadioFragment.EXTRA_PARAM, extra);
                }
                slideAdapter.setIcon(currentRadio, android.R.drawable.ic_media_pause);
        }

        if (fragment != null) {
            try {
                if (bundle != null) {
                    fragment.setArguments(bundle);
                }

                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction transaction = fragmentManager
                        .beginTransaction()
                        .replace(R.id.frame_container, fragment)
                        .addToBackStack(null);

                //stupid fix for this http://stackoverflow.com/questions/7575921/illegalstateexception-can-not-perform-this-action-after-onsaveinstancestate-h
                if (fragment instanceof RadioFragment) {
                    transaction.commit();
                } else {
                    transaction.commitAllowingStateLoss();
                }
            } catch (Exception e) {
                Log.e("MainActivity", "displayView", e);
                return;
            }
        }

        // update selected item and title, then close the drawer
        if (position >= 0) {
            mDrawerList.setItemChecked(position, true);
            mDrawerList.setSelection(position);
        }

        setTitle(title);
        mDrawerLayout.closeDrawer(mDrawerList);
        rightDrawerLayout.closeDrawer(rightDrawerList);
    }

    private void radioSettings(RadioInfo radioInfo) {
        if (State.SETTINGS.equals(currentState)) {
            return;
        }
        slideAdapter.setIcon(radioInfo, android.R.drawable.ic_menu_preferences);

        currentState = State.SETTINGS;
        RadioPreferencesFragment fragment = new RadioPreferencesFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(RadioPreferencesFragment.RADIO_PARAM, radioInfo);
        fragment.setArguments(bundle);
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.frame_container, fragment)
                .addToBackStack(null)
                .commit();

        fragment.setSettingsCallback(new Callback<Tuple<RadioInfo, RadioInfo>>() {
            @Override
            public void callback(Tuple<RadioInfo, RadioInfo> obj) {
                RadioInfo oldRadio = obj.getFirst();
                RadioInfo newRadio = obj.getSecond();

                if (!slidePreferencesHelper.addOrReplace(oldRadio, newRadio, slideAdapter)){
                    return;
                }
                PreferenceHelper.saveMap(getSharedPreferences(PREF_NAME, MODE_PRIVATE), PREF_RADIO, slidePreferencesHelper.getPreferencesMap());

                if (oldRadio != null && oldRadio.isSame(currentRadio)) {
                    currentRadio = newRadio;
                    setTitle(currentRadio.getTitle());
                }
                //startNewRadio(newRadio.getTitle(), newRadio);
            }
        });
    }


    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        if (getActionBar() != null) {
            getActionBar().setTitle(mTitle);
        }
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(TYPE);
            if (type == null || type.isEmpty()) {
                return;
            }

            switch (type) {
                case GLOBAL_ERROR:
                    // display error
                    String globalError = intent.getStringExtra(GLOBAL_ERROR);
                    if (globalError != null && !globalError.isEmpty()) {
                        displayError(globalError);
                    }
                    break;

                case RADIO_REMOVE:
                    // remove radio and go to home
                    String radioRemove = intent.getStringExtra(RADIO_REMOVE);
                    if (radioRemove != null && !radioRemove.isEmpty()) {
                        // if "magic" - display home screen - mean remove not saved radio
                        if (!radioRemove.equals(MAGIC_HOME)) {
                            slidePreferencesHelper.remove(radioRemove, slideAdapter);
                            PreferenceHelper.saveMap(getSharedPreferences(PREF_NAME, MODE_PRIVATE), PREF_RADIO, slidePreferencesHelper.getPreferencesMap());
                        }

                        displayView(-1);
                    }
                    break;

                case SIMPLE_RADIO:
                    // display and maybe play radio
                    String radio = intent.getStringExtra(SIMPLE_RADIO);
                    Integer position = null;
                    String extra = null;
                    if (radio != null && !radio.isEmpty()) {
                        // if "magic" - display home screen
                        if (radio.equals(MAGIC_HOME)) {
                            if (!State.RADIO.equals(currentState) && configuration != null) {
                                position = -1;
                            }
                        } else {
                            RadioInfo radioInfo = (RadioInfo) intent.getSerializableExtra(REAL_RADIO);
                            if (radioInfo == null) {
                                //setup "virtual" radio - do not save it!
                                radioInfo = RadioBuilder.getInstance()
                                        .setTitle(radio)
                                        .setArtist(radio)
                                        .setArtistLinkType(RadioInfo.ArtistLinkType.LIMIT)
                                        .build();

                                if (slidePreferencesHelper.addWithoutPreferences(radioInfo, slideAdapter)) {
                                    position = slidePreferencesHelper.position(radioInfo) + POSITION_ACCURATE;
                                } else {
                                    position = slidePreferencesHelper.getNavMenuSize();
                                }
                            } else {
                                if (!radioInfo.getTitle().equals(FavoriteFragment.RADIO_TITLE)) {
                                    // display current radio & play it
                                    position =  slidePreferencesHelper.position(radioInfo);
                                    if (position == -1) {
                                        slidePreferencesHelper.addWithoutPreferences(radioInfo, slideAdapter);
                                        position = slidePreferencesHelper.position(radioInfo) + POSITION_ACCURATE;
                                    } else {
                                        position += POSITION_ACCURATE;
                                    }
                                } else {
                                    // display "favorites" radio and play it
                                    if (slidePreferencesHelper.addWithoutPreferences(radioInfo, slideAdapter)) {
                                        position = slidePreferencesHelper.position(radioInfo) + POSITION_ACCURATE;
                                    } else {
                                        position = slidePreferencesHelper.getNavMenuSize();
                                    }
                                }
                                extra = RadioFragment.EXTRA_PLAY;
                            }
                        }
                    }
                    if (position != null){
                        displayView(position, extra);
                    }
                    break;

                case RADIO_TITLE:
                    // setup header title
                    String radioTitle = intent.getStringExtra(RADIO_TITLE);
                    if (radioTitle != null && !radioTitle.isEmpty()) {
                        setTitle(radioTitle);
                        int pos = slidePreferencesHelper.position(radioTitle) + POSITION_ACCURATE;
                        mDrawerList.setItemChecked(pos, true);
                        mDrawerList.setSelection(pos);
                    }
                    break;

                default:
                    // currently played event from media service
                    RadioInfo radioSource = (RadioInfo) intent.getSerializableExtra(MediaEvent.DATA_RADIO);
                    if (radioSource != null) {

                        // if no settings then goto radio fragment
                        if (currentFragmentRadioChecker == null && !State.SETTINGS.equals(currentState) && !State.FAV.equals(currentState)) {
                            displayView(slidePreferencesHelper.position(radioSource) + POSITION_ACCURATE);
                        }

                        // setup icon if it pause/stop event
                        int message = intent.getIntExtra(MediaEvent.DATA_MESSAGE_CODE, 1);
                        switch (message) {
                            case MediaPlayerService.MSG_STOP:
                            case MediaPlayerService.MSG_PAUSE:
                                slideAdapter.setIcon(radioSource, android.R.drawable.ic_media_pause);
                                break;
                            case MediaPlayerService.MSG_PLAY:
                                slideAdapter.setIcon(radioSource, android.R.drawable.ic_media_play, android.R.drawable.ic_media_pause);
                                break;
                        }

                        // next processing by current radio fragment
                        if (currentFragmentRadioChecker != null && currentFragmentCallback != null && currentFragmentRadioChecker.check(radioSource)) {
                            Message msg = new Message();

                            msg.what = message;
                            msg.obj = intent.getSerializableExtra(MediaEvent.DATA_SERIALIZEBLE);
                            if (msg.obj == null) {
                                msg.obj = intent.getParcelableExtra(MediaEvent.DATA_PARCEABLE);
                            }

                            currentFragmentCallback.callback(msg);

                            if (message == MediaPlayerService.MSG_SET_CURRENT_SONG){
                                SongInfo currentSong = (SongInfo) intent.getSerializableExtra(MediaEvent.DATA_SERIALIZEBLE);
                                Message favmsg = new Message();
                                if (slidePreferencesHelper.isFavorite(currentSong.getArtist())){
                                    favmsg.what = MediaPlayerService.MSG_FAV;
                                } else {
                                    favmsg.what = MediaPlayerService.MSG_NOT_FAV;
                                }
                                currentFragmentCallback.callback(favmsg);

                                rightAdapter.removeAll(false);
                                rightAdapter.add(NavDrawerItem.generate(currentSong), false);
                                rightAdapter.add(NavDrawerItem.generate("Similar artists:"), false);
                                if (currentSong.getSimilarArtists() != null) {
                                    for (String similar : currentSong.getSimilarArtists()) {
                                        rightAdapter.add(NavDrawerItem.generate(similar), false);
                                    }
                                }
                                rightAdapter.notifyDataSetChanged();
                            }
                        }
                    }
            }

        }
    };

    private boolean doubleBackToExitPressedOnce;

    @Override
    public void onBackPressed() {
        if (preventBack) {
            return;
        }

        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate();
            Fragment f = getFragmentManager().findFragmentById(R.id.frame_container);
            if (f instanceof RadioFragment) {
                currentState = State.RADIO;
                slideAdapter.setIcon(currentRadio, android.R.drawable.ic_media_pause);
            } else if (f instanceof RadioPreferencesFragment) {
                currentState = State.SETTINGS;
            } else if (f instanceof HomeFragment) {
                currentState = State.HOME;
                setTitle(R.string.app_name);
                mDrawerList.clearChoices();
                rightDrawerList.clearChoices();
            } else if(f instanceof FavoriteFragment){
                currentState = State.FAV;
                setTitle(R.string.fav_title);
            }
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, getString(R.string.back_hint), Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }

    private void displayError(String description) {
        ErrorFragment fragment = new ErrorFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(ErrorFragment.ERROR_DESCRIPTION, description);
        fragment.setArguments(bundle);

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager
                .beginTransaction()
                .replace(R.id.frame_container, fragment)
                .commit();
    }
}