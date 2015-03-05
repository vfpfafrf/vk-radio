package io.daydev.vkrdo.bean;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

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
    private Integer yearFrom;
    private Integer yearTo;

    public RadioInfo() {
    }

    public RadioInfo(String title, String genre, String mood, String artist, ArtistLinkType artistLinkType, Integer yearFrom, Integer yearTo) {
        this.title = title;
        this.genre = genre;
        this.mood = mood == null ? null : (mood.equals("EMPTY") ? null : mood);
        this.artist = artist;
        this.artistLinkType = (artistLinkType == null ? ArtistLinkType.LIMIT : artistLinkType);

        setYearFrom(yearFrom);
        setYearTo(yearTo);
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

    public Integer getYearFrom() {
        return yearFrom;
    }

    public String getYearFromAsString() {
        return yearFrom == null ? "" : yearFrom.toString();
    }

    public String getYearToAsString() {
        return yearTo == null ? "" : yearTo.toString();
    }

    public void setYearFrom(Integer yearFrom) {
        if (yearFrom != null){
            if (yearFrom < 1900){
                yearFrom = 1900;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            if (yearFrom > calendar.get(Calendar.YEAR)){
                yearFrom = calendar.get(Calendar.YEAR);
            }
        }
        this.yearFrom = yearFrom;
    }

    public Integer getYearTo() {
        return yearTo;
    }

    public void setYearTo(Integer yearTo) {
        if (yearTo != null) {
            if (yearTo < 1900) {
                yearTo = 1900;
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            if (yearTo > calendar.get(Calendar.YEAR)) {
                yearTo = calendar.get(Calendar.YEAR);
            }
        }
        this.yearTo = yearTo;
    }

    public ArtistLinkType getArtistLinkType() {
        return artistLinkType == null ? ArtistLinkType.LIMIT : artistLinkType;
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
