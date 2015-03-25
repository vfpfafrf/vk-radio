package io.daydev.vkrdo.external.echo;

import com.echonest.api.v4.PlaylistParams;

import java.util.List;

/**
 * Created by dmitry on 25.03.15.
 */
public class PlaylistParamsWrapper {

    private PlaylistParams playlistParams;
    private List<String> artists;

    public PlaylistParamsWrapper(PlaylistParams playlistParams, List<String> artists) {
        this.playlistParams = playlistParams;
        this.artists = artists;
    }

    public PlaylistParams getPlaylistParams() {
        return playlistParams;
    }

    public List<String> getArtists() {
        return artists;
    }
}
