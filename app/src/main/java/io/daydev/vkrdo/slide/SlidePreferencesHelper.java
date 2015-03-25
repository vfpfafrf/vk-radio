package io.daydev.vkrdo.slide;

import io.daydev.vkrdo.bean.RadioInfo;

import java.util.*;

/**
 * Helper class for store and manipulate Drawer and Preferences items
 */
public class SlidePreferencesHelper {

    // slide menu items
    private List<String> navMenuTitles;
    private Map<String, RadioInfo> radios;
    private Map<String, RadioInfo> preferencesMap;
    private ArrayList<String> favoritesArtists;

    public void onCreate(Map<String, RadioInfo> savedPreferences, ArrayList<String> favoritesArtists){
        radios = new HashMap<>();
        navMenuTitles = new ArrayList<>();

        preferencesMap = savedPreferences == null ? new HashMap<String, RadioInfo>() : savedPreferences;

        radios.putAll(preferencesMap);
        for (Map.Entry<String, RadioInfo> me : preferencesMap.entrySet()) {
            if (me.getValue().getTitle() != null && !me.getValue().getTitle().isEmpty()) {
                navMenuTitles.add(me.getValue().getTitle());
            }
        }

        this.favoritesArtists = (favoritesArtists == null ? new ArrayList<String>() : favoritesArtists);
    }

    public Collection<NavDrawerItem> convertToNavDrawItems(){
        Collection<NavDrawerItem> navDrawerItems = new ArrayList<>();

        for (String nav : navMenuTitles) {
            navDrawerItems.add(new NavDrawerItem(nav, android.R.drawable.ic_media_pause));
        }
        return navDrawerItems;
    }

    public RadioInfo getByPosition(int position){
        String title = navMenuTitles.get(position);
        return radios.get(title);
    }

    public int position(RadioInfo radioInfo){
        return radioInfo == null ? -1 : position(radioInfo.getTitle());
    }

    public int position(String radioTitle){
        return radioTitle == null ? -1 : navMenuTitles.indexOf(radioTitle);
    }

    public void remove(String radioTitle, NavDrawerListAdapter slideAdapter){
        navMenuTitles.remove(radioTitle);
        radios.remove(radioTitle);
        preferencesMap.remove(radioTitle);
        slideAdapter.removeItem(radioTitle);

    }

    public Map<String, RadioInfo> getPreferencesMap() {
        return preferencesMap;
    }

    /**
     * returns size of visible menu items, may be different from PreferencesMap
     * @return int
     */
    public int getNavMenuSize(){
        return navMenuTitles.size();
    }

    /**
     * add or replace oldRadio with newRadio
     * @param oldRadio old
     * @param newRadio new
     * @return true - all ok, false - radio do not saved
     */
    public boolean addOrReplace(RadioInfo oldRadio, RadioInfo newRadio, NavDrawerListAdapter slideAdapter){
        String newTitle = newRadio.getTitle();
        if (oldRadio == null) {
            if (newTitle == null || newTitle.isEmpty()) {
                if (newRadio.getGenre() == null || newRadio.getGenre().isEmpty()) {
                    if (newRadio.getArtist() == null || newRadio.getArtist().isEmpty()) {
                        //newRadio.setTitle(getString(R.string.new_radio_title));
                        return false; // do not save radio without artist and genre
                    } else {
                        newTitle = newRadio.getArtist();
                    }
                } else {
                    newTitle = newRadio.getGenre();
                }
            }

            int i = 1;
            while (radios.containsKey(newTitle)) {
                newTitle = (newRadio.getTitle() + " " + i++);
            }
            newRadio.setTitle(newTitle);
        } else {
            String oldTitle = oldRadio.getTitle();
            if (newTitle == null || newTitle.isEmpty()){
                newTitle = oldTitle;
            }
            radios.remove(oldTitle);
            preferencesMap.remove(oldTitle);
        }

        radios.put(newTitle, newRadio);
        preferencesMap.put(newTitle, newRadio);

        if (oldRadio != null && !oldRadio.getTitle().equals(newTitle)) {
            if (navMenuTitles.contains(oldRadio.getTitle())) {
                navMenuTitles.set(navMenuTitles.indexOf(oldRadio.getTitle()), newTitle);
                slideAdapter.replace(oldRadio.getTitle(),  NavDrawerItem.generate(newRadio));
            } else {
                navMenuTitles.add(newTitle);
                slideAdapter.add( NavDrawerItem.generate(newRadio));
            }
        } else if (oldRadio == null) {
            navMenuTitles.add(newTitle);
            slideAdapter.add( NavDrawerItem.generate(newRadio));
        }

        return true;
    }

    /**
     * add to in memory only
     * @param radioInfo Radio
     * @return true - if add/ false - already exists
     */
    public boolean addWithoutPreferences(RadioInfo radioInfo, NavDrawerListAdapter slideAdapter){
        radios.put(radioInfo.getTitle(), radioInfo);
        if (!navMenuTitles.contains(radioInfo.getTitle())){
            synchronized (this) {
                if (!navMenuTitles.contains(radioInfo.getTitle())) {
                    navMenuTitles.add(radioInfo.getTitle());
                    slideAdapter.add(NavDrawerItem.generateVirtual(radioInfo));
                }
            }
            return true;
        }
        return false;
    }


    public boolean addToFavorite(String atrist){
        atrist = atrist.trim();
        if (!favoritesArtists.contains(atrist)) {
            favoritesArtists.add(atrist);
            return true;
        }
        return  false;
    }

    public boolean isFavorite(String artist){
        return artist != null && favoritesArtists.contains(artist.trim());
    }

    public void removeFromFavorite(String artist){
        if (artist != null){
            favoritesArtists.remove(artist.trim());
        }
    }

    public ArrayList<String> getFavoritesArtists() {
        return favoritesArtists;
    }
}
