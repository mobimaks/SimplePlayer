package ua.mobimaks.simpleplayer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import ua.mobimaks.simpleplayer.activity.MainActivity;
import ua.mobimaks.simpleplayer.model.PlayerConstants;
import ua.mobimaks.simpleplayer.model.Song;
import ua.mobimaks.simpleplayer.receiver.PlayerReceiver;

/**
 * Created by mobimaks on 04.12.2014.
 */
public class PlayerService extends Service implements OnPreparedListener, OnCompletionListener {

    public static boolean IS_PLAYING;
    private boolean mForegroundActive = false;

    private MediaPlayer mPlayer;
    private ArrayList<Song> mMusicList;
    private int currentSong = 0;
    private PlayerReceiverListener mPlayerReceiverListener;
    private Notification mPlayerNotification;
    private NotificationManager mNotificationManager;
    private PlayerReceiver mPlayerReceiver;

    public interface OnPlayerListener {
        void onPlay();

        void onPause();

        void onPlayPause();

        void onPlayPrevious();

        void onPlayNext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        IS_PLAYING = true;
        Toast.makeText(getApplicationContext(), "Created", Toast.LENGTH_SHORT).show();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        getMusicList();
        registerReceiver();
        initMusicPlayer();
    }

    private void getMusicList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        mMusicList = new ArrayList<>();

        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

            do {
                long songId = musicCursor.getLong(idColumn);
                String songTitle = musicCursor.getString(titleColumn);
                String songArtist = musicCursor.getString(artistColumn);
                mMusicList.add(new Song(songId, songTitle, songArtist));
            } while (musicCursor.moveToNext());
        }
        Collections.sort(mMusicList);
    }

    private void registerReceiver() {
        mPlayerReceiverListener = new PlayerReceiverListener();
        IntentFilter playerFilter = new IntentFilter();
        playerFilter.addAction(PlayerConstants.PREVIOUS);
        playerFilter.addAction(PlayerConstants.PLAY_PAUSE);
        playerFilter.addAction(PlayerConstants.NEXT);
        playerFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        mPlayerReceiver = new PlayerReceiver();
        mPlayerReceiver.setOnReceiveListener(mPlayerReceiverListener);
        registerReceiver(mPlayerReceiver, playerFilter);
    }

    private void initMusicPlayer() {
        mPlayer = new MediaPlayer();
        mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case PlayerConstants.START_FOREGROUND:
                startForeground();
                break;
            case PlayerConstants.STOP_FOREGROUND:
                stopForeground();
                break;
            case PlayerConstants.PLAY_PAUSE:
                playPause();
                break;
            case PlayerConstants.PREVIOUS:
                playPrev();
                break;
            case PlayerConstants.NEXT:
                playNext();
                break;
        }
        return START_STICKY;
    }

    private void startForeground() {
        updateNotificationData();
        startForeground(PlayerConstants.NOTIFICATION_ID, mPlayerNotification);
        mForegroundActive = true;
        playSong();
    }

    private void stopForeground() {
        stopForeground(true);
        mForegroundActive = false;
        sendWidgetUpdate(false);
        stopSelf();
    }

    private void playPause() {
        if (checkIfForegroundStarted()) return;
        if (mPlayer.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    private void playPrev() {
        if (checkIfForegroundStarted()) return;
        currentSong--;
        if (currentSong < 0) {
            currentSong = mMusicList.size() - 1;
        }
        playSong();
    }

    private void playNext() {
        if (checkIfForegroundStarted()) return;
        currentSong++;
        if (currentSong >= mMusicList.size()) {
            currentSong = 0;
        }
        playSong();
    }

    private boolean checkIfForegroundStarted() {
        if (!mForegroundActive) {
            startForeground();
            return true;
        }
        return false;
    }

    private void play() {
        mPlayer.start();
        updateNotification();
    }

    private void pause() {
        mPlayer.pause();
        updateNotification();
    }

    private void playSong() {
        mPlayer.reset();
        Song playSong = mMusicList.get(currentSong);
        long currSong = playSong.getId();

        Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currSong);
        try {
            mPlayer.setDataSource(getApplicationContext(), trackUri);
            mPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            Toast.makeText(this, "Music not found", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Can't play", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateNotification() {
        updateNotificationData();
        mNotificationManager.notify(PlayerConstants.NOTIFICATION_ID, mPlayerNotification);
    }

    private void updateNotificationData() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(PlayerConstants.OPEN_PLAYER);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent previousIntent = new Intent(this, PlayerService.class);
        previousIntent.setAction(PlayerConstants.PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getService(this, 0, previousIntent, 0);

        Intent playIntent = new Intent(this, PlayerService.class);
        playIntent.setAction(PlayerConstants.PLAY_PAUSE);
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0, playIntent, 0);

        Intent nextIntent = new Intent(this, PlayerService.class);
        nextIntent.setAction(PlayerConstants.NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, 0);

        int smallIcon = mPlayer.isPlaying() ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause;
        int playPauseIcon = mPlayer.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
        String playPauseText = mPlayer.isPlaying() ? "Pause" : "Play";

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(mMusicList.get(currentSong).getArtist())
                .setContentText(mMusicList.get(currentSong).getTitle())
                .setOngoing(true)
                .setSmallIcon(smallIcon)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_media_previous, "Prev", prevPendingIntent)
                .addAction(playPauseIcon, playPauseText, playPendingIntent)
                .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent);
        mPlayerNotification = builder.build();

        sendWidgetUpdate();
    }

    private void sendWidgetUpdate() {
        sendWidgetUpdate(mPlayer.isPlaying());
    }

    private void sendWidgetUpdate(boolean isPlaying) {
        Intent widgetIntent = new Intent(PlayerConstants.MUSIC_STATE_CHANGED);
        widgetIntent.putExtra(PlayerConstants.MUSIC_STATE, isPlaying);
        widgetIntent.putExtra(PlayerConstants.SONG_ARTIST, mMusicList.get(currentSong).getArtist());
        widgetIntent.putExtra(PlayerConstants.SONG_TITLE, mMusicList.get(currentSong).getTitle());
        sendBroadcast(widgetIntent);
    }

    @Override
    public IBinder onBind(Intent Intent) {
        return null;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mPlayer.start();
        updateNotification();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playNext();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        IS_PLAYING = false;
        Toast.makeText(getApplicationContext(), "Destroyed", Toast.LENGTH_SHORT).show();
        mPlayer.stop();
        mPlayer.release();
        unregisterReceiver(mPlayerReceiver);
    }

    private class PlayerReceiverListener implements OnPlayerListener {

        @Override
        public void onPlay() {
            if (!mPlayer.isPlaying()) {
                play();
            }
        }

        @Override
        public void onPause() {
            if (mPlayer.isPlaying()) {
                pause();
            }
        }

        @Override
        public void onPlayPause() {
            playPause();
        }

        @Override
        public void onPlayPrevious() {
            playPrev();
        }

        @Override
        public void onPlayNext() {
            playNext();
        }
    }
}
