package ua.mobimaks.simpleplayer.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import ua.mobimaks.simpleplayer.R;
import ua.mobimaks.simpleplayer.model.PlayerConstants;
import ua.mobimaks.simpleplayer.service.PlayerService;


public class MainActivity extends Activity implements View.OnClickListener {

    private TextView tvArtist, tvTitle;
    private ImageButton btnMusicPrev, btnMusicPlayPause, btnMusicNext, btnMusicStop;
    private String mSongArtist, mSongTitle;
    private boolean mMusicPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        registerMusicReceiver();
    }

    private void initViews() {
        tvArtist = (TextView) findViewById(R.id.text_SongArtist_PlayerFragment);
        tvTitle = (TextView) findViewById(R.id.text_SongTitle_PlayerFragment);

        btnMusicPrev = (ImageButton) findViewById(R.id.button_MusicPrev_PlayerFragment);
        btnMusicPrev.setOnClickListener(this);

        btnMusicPlayPause = (ImageButton) findViewById(R.id.button_MusicPlayPause_PlayerFragment);
        btnMusicPlayPause.setOnClickListener(this);

        btnMusicNext = (ImageButton) findViewById(R.id.button_MusicNext_PlayerFragment);
        btnMusicNext.setOnClickListener(this);

        btnMusicStop = (ImageButton) findViewById(R.id.button_MusicStop_PlayerFragment);
        btnMusicStop.setOnClickListener(this);
    }

    private void registerMusicReceiver() {
        registerReceiver(mMusicReceiver, new IntentFilter(PlayerConstants.MUSIC_STATE_CHANGED));
    }

    private BroadcastReceiver mMusicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mSongArtist = intent.getStringExtra(PlayerConstants.SONG_ARTIST);
            mSongTitle = intent.getStringExtra(PlayerConstants.SONG_TITLE);
            mMusicPlaying = intent.getBooleanExtra(PlayerConstants.MUSIC_STATE, false);

            tvArtist.setText(mSongArtist);
            tvTitle.setText(mSongTitle);

            int playPauseIcon = mMusicPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
            btnMusicPlayPause.setImageResource(playPauseIcon);
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_MusicPrev_PlayerFragment:
                changeMusicState(PlayerConstants.PREVIOUS);
                break;
            case R.id.button_MusicPlayPause_PlayerFragment:
                if (PlayerService.IS_PLAYING) {
                    changeMusicState(PlayerConstants.PLAY_PAUSE);
                } else {
                    startPlayerService();
                }
                break;
            case R.id.button_MusicNext_PlayerFragment:
                changeMusicState(PlayerConstants.NEXT);
                break;
            case R.id.button_MusicStop_PlayerFragment:
                stopPlayerService();
                break;
        }
    }

    private void changeMusicState(String state) {
        Intent intent = new Intent();
        intent.setAction(state);
        sendBroadcast(intent);
    }

    private void startPlayerService() {
        startServiceCommand(PlayerConstants.START_FOREGROUND);
    }

    private void stopPlayerService() {
        if (PlayerService.IS_PLAYING) {
            startServiceCommand(PlayerConstants.STOP_FOREGROUND);
        }
    }

    private void startServiceCommand(String action) {
        Intent playService = new Intent(this, PlayerService.class);
        playService.setAction(action);
        startService(playService);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mMusicReceiver);
        super.onDestroy();
    }
}
