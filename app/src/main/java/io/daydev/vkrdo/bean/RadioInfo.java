package io.daydev.vkrdo.bean;

import java.io.Serializable;

/**
 * Created by dmitry on 22.02.15.
 */
public class RadioInfo  implements Serializable{


    public enum ArtistLinkType{
        SIMILAR, LIMIT;

        public static ArtistLinkType value(int key){
            return key ==1 ? SIMILAR : LIMIT;
        }
    }

    private String title;
    private String genre;
    private String mood;
    private String artist;
    private ArtistLinkType artistLinkType;

    public RadioInfo() {
    }

    public RadioInfo(String title, String genre, String mood, String artist, ArtistLinkType artistLinkType) {
        this.title = title;
        this.genre = genre;
        this.mood = mood == null ? null : (mood.equals("EMPTY") ? null : mood);
        this.artist = artist;
        this.artistLinkType = artistLinkType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public ArtistLinkType getArtistLinkType() {
        return artistLinkType;
    }

    public void setArtistLinkType(ArtistLinkType artistLinkType) {
        this.artistLinkType = artistLinkType;
    }

    public boolean isSame(RadioInfo radioInfo) {
        return radioInfo != null && radioInfo.getTitle() != null && radioInfo.getTitle().equals(title);
    }

    public boolean isEmpty(){
        return (genre == null || genre.isEmpty()) && (artist == null || artist.isEmpty()) && (mood == null || mood.isEmpty());
    }

}
