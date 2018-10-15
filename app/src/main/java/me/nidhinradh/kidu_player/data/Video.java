package me.nidhinradh.kidu_player.data;


import java.io.Serializable;

public class Video implements Serializable {
    private String title;
    private String uri;
    private String duration;

    public Video(String title, String uri, String duration) {
        this.title = title;
        this.uri = uri;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public String getUri() {
        return uri;
    }

    public String getDuration() {
        return duration;
    }
}
