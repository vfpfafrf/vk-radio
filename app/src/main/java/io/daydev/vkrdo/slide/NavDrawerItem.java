package io.daydev.vkrdo.slide;

import io.daydev.vkrdo.bean.RadioInfo;
import io.daydev.vkrdo.bean.SongInfo;

/**
 * Created by dmitry on 03.03.15.
 */
public class NavDrawerItem {

    public static NavDrawerItem generate(RadioInfo radioInfo){
        return new NavDrawerItem(radioInfo.getTitle(), android.R.drawable.ic_media_pause);
    }

    public static NavDrawerItem generateVirtual(RadioInfo radioInfo){
        return new NavDrawerItem(radioInfo.getTitle(), android.R.drawable.ic_media_pause, true, "V");
    }

    public static NavDrawerItem generate(SongInfo songInfo){
        return new NavDrawerItem(songInfo.getArtist(), 0);
    }

    public static NavDrawerItem generate(String title){
        return new NavDrawerItem(title, 0);
    }

    private String title;
    private int icon;
    private String count = "0";
    private boolean isCounterVisible = false;

    public NavDrawerItem(){}

    public NavDrawerItem(String title, int icon){
        this.title = title;
        this.icon = icon;
    }

    public NavDrawerItem(String title, int icon, boolean isCounterVisible, String count){
        this.title = title;
        this.icon = icon;
        this.isCounterVisible = isCounterVisible;
        this.count = count;
    }

    public String getTitle(){
        return this.title;
    }

    public int getIcon(){
        return this.icon;
    }

    public String getCount(){
        return this.count;
    }

    public boolean getCounterVisibility(){
        return this.isCounterVisible;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setIcon(int icon){
        this.icon = icon;
    }

    public void setCount(String count){
        this.count = count;
    }

    public void setCounterVisibility(boolean isCounterVisible){
        this.isCounterVisible = isCounterVisible;
    }
}