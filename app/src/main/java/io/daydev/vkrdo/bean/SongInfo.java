package io.daydev.vkrdo.bean;

import java.io.Serializable;
import java.util.Collection;

/**
 * Created by dmitry on 22.02.15.
 */
public class SongInfo implements Serializable {

    private final String artist;
    private String artistPhoto;
    private String album;
    private final String title;
    private volatile String location;
    private String coverArt;
    private Collection<String> similarArtists;

    public SongInfo(String artist, String title, String location, String artistPhoto) {
        this.artist = artist;
        this.title = title;
        this.location = location;
        this.artistPhoto = artistPhoto;
    }

    public void setArtistPhoto(String artistPhoto) {
        this.artistPhoto = artistPhoto;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public String getArtistPhoto() {
        return artistPhoto == null ? coverArt : artistPhoto;
    }

    public String getCoverArt() {
        return coverArt;
    }

    public void setCoverArt(String coverArt) {
        this.coverArt = coverArt;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public Collection<String> getSimilarArtists() {
        return similarArtists;
    }

    public void setSimilarArtists(Collection<String> similarArtists) {
        this.similarArtists = similarArtists;
    }

    @Override
    public String toString() {
        return artist + (title == null || title.isEmpty() ? "" : " - " + title);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SongInfo info = (SongInfo) o;

        if (artist != null ? !artist.equals(info.artist) : info.artist != null) return false;
        if (title != null ? !title.equals(info.title) : info.title != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = artist != null ? artist.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        return result;
    }
}
