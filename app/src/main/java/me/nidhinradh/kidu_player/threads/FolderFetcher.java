package me.nidhinradh.kidu_player.threads;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;

import me.nidhinradh.kidu_player.activities.MainActivity;
import me.nidhinradh.kidu_player.data.Album;
import me.nidhinradh.kidu_player.data.Video;

public class FolderFetcher extends Thread {

    private static ArrayList<Album> albumArrayList;
    private MainActivity mainActivity;

    public FolderFetcher(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        albumArrayList = new ArrayList<>();
    }

    @Override
    public void run() {
        long bucketId;
        String bucketDisplayName;
        String title;
        String uri;
        String duration;

        String[] PROJECTION_BUCKET = {
                MediaStore.Video.VideoColumns.BUCKET_ID,
                MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Video.VideoColumns.DATA};

        String BUCKET_GROUP_BY = "1) GROUP BY 1,(2";
        String BUCKET_ORDER_BY = "bucket_display_name";

        Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Cursor folderCursor = mainActivity.getContentResolver().query(videoUri, PROJECTION_BUCKET, BUCKET_GROUP_BY, null, BUCKET_ORDER_BY);
        if (folderCursor != null) {
            if (folderCursor.moveToFirst()) {
                int bucketIDIndex = folderCursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID);
                int bucketIndex = folderCursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);

                do {
                    bucketDisplayName = folderCursor.getString(bucketIndex);
                    bucketId = folderCursor.getLong(bucketIDIndex);
                    String SEARCH_PARAMS = "bucket_id = " + bucketId;
                    String ORDER_BY = MediaStore.Images.Media.DATE_TAKEN;
                    String[] columns = {MediaStore.Video.Media.TITLE,
                            MediaStore.Video.Media.DATA,
                            MediaStore.Video.Media.DURATION};

                    Cursor videoCursor = mainActivity.getContentResolver().query(videoUri, columns, SEARCH_PARAMS, null, ORDER_BY);
                    if (videoCursor != null) {
                        if (videoCursor.moveToFirst()) {
                            int titleIndex = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
                            int uriIndex = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                            int durationIndex = videoCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);

                            ArrayList<Video> videoArrayList = new ArrayList<>();

                            do {
                                title = videoCursor.getString(titleIndex);
                                uri = videoCursor.getString(uriIndex);
                                duration = videoCursor.getString(durationIndex);

                                videoArrayList.add(new Video(title, uri, duration));

                            } while (videoCursor.moveToNext());
                            albumArrayList.add(new Album(bucketDisplayName, videoArrayList));
                        }
                        videoCursor.close();
                    }
                } while (folderCursor.moveToNext());
            }
            folderCursor.close();
        }
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainActivity.renderFolderList(albumArrayList);
            }
        });
    }
}
