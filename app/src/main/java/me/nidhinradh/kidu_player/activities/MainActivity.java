package me.nidhinradh.kidu_player.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;
import me.nidhinradh.kidu_player.itemdecoration.ItemOffsetDecoration;
import me.nidhinradh.kidu_player.viewholders.SectionHeaderViewHolder;
import me.nidhinradh.kidu_player.viewholders.SectionItemViewHolder;
import me.nidhinradh.kidu_player.R;
import me.nidhinradh.kidu_player.data.Album;
import me.nidhinradh.kidu_player.data.Video;
import me.nidhinradh.kidu_player.threads.FolderFetcher;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100;
    FolderFetcher folderFetcher;
    RecyclerView folderRecyclerView;
    private SectionedRecyclerViewAdapter sectionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        folderFetcher = new FolderFetcher(MainActivity.this);

        requestReadPermission();
    }


    private void requestReadPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            folderFetcher.start();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    folderFetcher.start();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
    }

    public void renderFolderList(ArrayList<Album> albumArrayList) {
        sectionAdapter = new SectionedRecyclerViewAdapter();
        for (int i = 0; i < albumArrayList.size(); i++) {
            String bucketDisplayName = albumArrayList.get(i).getBucketName();
            ArrayList<Video> videoArrayList = albumArrayList.get(i).getVideoArrayList();
            sectionAdapter.addSection(new FolderSection(bucketDisplayName, videoArrayList, getApplicationContext()));
        }
        folderRecyclerView = findViewById(R.id.rv_folder);
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(getApplicationContext(), R.dimen.item_offset);
        folderRecyclerView.addItemDecoration(itemDecoration);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            setPortraitLayout();
        } else {
            setLandscapeLayout();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setLandscapeLayout();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setPortraitLayout();
        }
    }

    private void setPortraitLayout() {
        GridLayoutManager glm = new GridLayoutManager(getApplicationContext(), 3);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (sectionAdapter.getSectionItemViewType(position)) {
                    case SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER:
                        return 3;
                    default:
                        return 1;
                }
            }
        });
        folderRecyclerView.setLayoutManager(glm);
        folderRecyclerView.setAdapter(sectionAdapter);
    }

    private void setLandscapeLayout() {
        GridLayoutManager glm = new GridLayoutManager(getApplicationContext(), 6);
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (sectionAdapter.getSectionItemViewType(position)) {
                    case SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER:
                        return 6;
                    default:
                        return 1;
                }
            }
        });
        folderRecyclerView.setLayoutManager(glm);
        folderRecyclerView.setAdapter(sectionAdapter);
    }

    public class FolderSection extends StatelessSection {

        String sectionHeader;
        int sectionCount;
        ArrayList<Video> videoArrayList;
        boolean expanded = false;
        Context context;

        FolderSection(String sectionHeader, ArrayList<Video> videoArrayList, Context context) {
            super(SectionParameters.builder()
                    .itemResourceId(R.layout.section_item)
                    .headerResourceId(R.layout.section_header)
                    .build());

            this.sectionHeader = sectionHeader;
            this.videoArrayList = videoArrayList;
            this.context = context;
            this.sectionCount = videoArrayList.size();
        }

        @Override
        public int getContentItemsTotal() {
            return expanded ? videoArrayList.size() : 0;
        }

        @Override
        public RecyclerView.ViewHolder getItemViewHolder(View view) {
            return new SectionItemViewHolder(view);
        }

        @Override
        public void onBindItemViewHolder(RecyclerView.ViewHolder holder, final int position) {
            SectionItemViewHolder sectionItemViewHolder = (SectionItemViewHolder) holder;
            String duration = "";
            long millis;
            if (videoArrayList.get(position).getDuration() != null) {
                millis = Long.parseLong(videoArrayList.get(position).getDuration());
            } else {
                MediaPlayer mp = new MediaPlayer();
                try {
                    mp.setDataSource(context, Uri.parse(videoArrayList.get(position).getUri()));
                    mp.prepare();
                } catch (IOException e) {
                    Log.e(getResources().getString(R.string.app_name), getResources().getString(R.string.io_exception));
                }
                millis = mp.getDuration();
                mp.release();
            }
            String hour = String.format(Locale.getDefault(), "%02d", TimeUnit.MILLISECONDS.toHours(millis));
            String minute = String.format(Locale.getDefault(), "%02d", TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)));
            String second = String.format(Locale.getDefault(), "%02d", TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            if (!hour.equals("00"))
                duration += hour + ":";
            if (!hour.equals("00") && minute.equals("00"))
                duration += "00:";
            if (hour.equals("00") && minute.equals("00"))
                duration += "0:";
            if (!minute.equals("00"))
                duration += minute + ":";
            if (second.equals("00"))
                duration += "00";
            if (!second.equals("00"))
                duration += second;

            sectionItemViewHolder.tvVideoDuration.setText(duration);
            Glide.with(context)
                    .load(videoArrayList.get(position).getUri())
                    .apply(new RequestOptions()
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .encodeFormat(Bitmap.CompressFormat.PNG)
                            .encodeQuality(75))
                    .into(sectionItemViewHolder.ivVideoThumbnail);
            sectionItemViewHolder.clItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ArrayList<Uri> uris = new ArrayList<>();
                    for (Video video : videoArrayList) {
                        uris.add(Uri.parse(video.getUri()));
                    }
                    Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                    intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                    intent.setType("video/");
                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                    intent.putExtra("INDEX", position);
                    startActivity(intent);
                }
            });
        }

        @Override
        public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
            return new SectionHeaderViewHolder(view);
        }

        @Override
        public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {
            final SectionHeaderViewHolder headerHolder = (SectionHeaderViewHolder) holder;
            headerHolder.tvSectionHeader.setText(sectionHeader);
            String count = sectionCount + " ";
            if (sectionCount == 1) {
                count += getResources().getString(R.string.main_item);
            } else {
                count += getResources().getString(R.string.main_items);
            }
            headerHolder.tvSectionCount.setText(count);
            headerHolder.rlSectionHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    expanded = !expanded;
                    headerHolder.ivArrow.setImageResource(
                            expanded ? R.drawable.ic_keyboard_arrow_up : R.drawable.ic_keyboard_arrow_down
                    );
                    sectionAdapter.notifyDataSetChanged();
                }
            });
        }
    }

}
