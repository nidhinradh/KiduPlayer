package me.nidhinradh.kidu_player.activities;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import org.rm3l.maoni.Maoni;

import me.nidhinradh.kidu_player.R;
import me.nidhinradh.kidu_player.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(R.string.title_about);
        }

        LinearLayout aboutLayout = findViewById(R.id.about_container);

        String version = getResources().getString(R.string.about_version) + " ";
        try {
            PackageManager manager = getApplicationContext().getPackageManager();
            PackageInfo info = manager.getPackageInfo(getApplicationContext().getPackageName(), 0);
            version += info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(getResources().getString(R.string.app_name), getResources().getString(R.string.name_not_found_exception));
        }

        Element versionElement = new Element();
        versionElement.setTitle(version);

        Element feedbackElement = new Element();
        feedbackElement.setTitle(getResources().getString(R.string.about_title));
        feedbackElement.setIconDrawable(R.drawable.ic_feedback);
        feedbackElement.setIconTint(R.color.colorPrimary);
        feedbackElement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Maoni.Builder(getApplicationContext(), getResources().getString(R.string.about_fileprovider))
                        .withWindowTitle(getResources().getString(R.string.about_send_feedback))
                        .withMessage(getResources().getString(R.string.about_message))
                        .withFeedbackContentHint(getResources().getString(R.string.about_feedback_hint))
                        .showKeyboardOnStart()
                        .disableScreenCapturingFeature()
                        .withTheme(R.style.Feedback)
                        .withDefaultToEmailAddress(getResources().getString(R.string.about_email))
                        .build()
                        .start(AboutActivity.this);
            }
        });

        Element copyElement = new Element();
        copyElement.setTitle(getResources().getString(R.string.about_copy));
        copyElement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setType(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse("http://creativecommons.org/licenses/by/3.0/"));
                startActivity(intent);
            }
        });

        View aboutPage = new AboutPage(this)
                .isRTL(false)
                .setDescription(getResources().getString(R.string.about_description))
                .addItem(versionElement)
                .addItem(feedbackElement)
                .addPlayStore(getResources().getString(R.string.about_play_url), getResources().getString(R.string.about_play_title))
                .addGroup(getResources().getString(R.string.about_group_title))
                .addWebsite(getResources().getString(R.string.about_site_url), getResources().getString(R.string.about_site_title))
                .addFacebook(getResources().getString(R.string.about_fb_url), getResources().getString(R.string.about_fb_title))
                .addGitHub(getResources().getString(R.string.about_git_url), getResources().getString(R.string.about_git_title))
                .addInstagram(getResources().getString(R.string.about_insta_url), getResources().getString(R.string.about_insta_title))
                .addItem(copyElement)
                .create();
        aboutLayout.addView(aboutPage);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
