package com.aozora.aozora;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.VideoView;

import java.io.InputStream;

public class HelpProgress extends Activity {
    private Button helpClose;
    private VideoView animation_video, default_video;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_progress);
        helpClose = findViewById(R.id.help_close);

        helpClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });

        animation_video = findViewById(R.id.progress_animation);
        default_video = findViewById(R.id.progress_default);
        String animation_path = "android.resource://" + getPackageName() + "/" + R.raw.progress_animation;
        String default_path = "android.resource://" + getPackageName() + "/" + R.raw.progress_default;
        animation_video.setVideoURI(Uri.parse(animation_path));
        default_video.setVideoURI(Uri.parse(default_path));
        animation_video.setOnPreparedListener(mp -> {
            mp.setLooping(true);  // ループ再生ON
            mp.setVolume(0f, 0f); // ミュート
            animation_video.start();
        });

        default_video.setOnPreparedListener(mp -> {
            mp.setLooping(true);  // ループ再生ON
            mp.setVolume(0f, 0f); // ミュート
            default_video.start();
        });

    }

}
