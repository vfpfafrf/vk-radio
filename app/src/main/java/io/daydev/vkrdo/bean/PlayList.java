package io.daydev.vkrdo.bean;

import java.util.Collection;

/**
 * Created by dmitry on 22.02.15.
 */
public interface PlayList {

    public boolean isPlayListStated();

    /**
     * generate play list due to genre
     *
     * @param radio - radio settings
     * @return true - if new playlist generated
     */
    public boolean generate(RadioInfo radio);

    /**
     * return next playable song
     *
     * @return SongInfo
     */
    public SongInfo next();

    /**
     * returns current playing song
     * @return SongInfo
     */
    public SongInfo getCurrent();

    public RadioInfo getCurrentRadio();

    /**
     * return "to-play-list" in simple string format
     * @return Collection
     */
    public Collection<String> getToPlaySimpleFormat();
}
