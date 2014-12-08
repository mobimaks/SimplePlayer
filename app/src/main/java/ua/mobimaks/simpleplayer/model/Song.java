package ua.mobimaks.simpleplayer.model;

/**
 * Created by mobimaks on 04.12.2014.
 */
public class Song implements Comparable<Song> {

    private long id;
    private String title;
    private String artist;

    public Song(long id, String title, String artist) {
        this.id = id;
        this.title = title;
        this.artist = artist;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    @Override
    public int compareTo(Song another) {
        return this.getTitle().compareTo(another.getTitle());
    }
}
