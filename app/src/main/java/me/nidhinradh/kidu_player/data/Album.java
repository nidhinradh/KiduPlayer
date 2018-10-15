package me.nidhinradh.kidu_player.data;

import java.util.ArrayList;

public class Album {
    private String bucketName;
    private ArrayList<Video> videoArrayList;

    public Album(String bucketName, ArrayList<Video> videoArrayList) {
        this.bucketName = bucketName;
        this.videoArrayList = videoArrayList;
    }

    public String getBucketName() {
        return bucketName;
    }

    public ArrayList<Video> getVideoArrayList() {
        return videoArrayList;
    }
}
