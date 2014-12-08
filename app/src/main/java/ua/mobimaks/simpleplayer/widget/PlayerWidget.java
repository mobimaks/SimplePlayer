package ua.mobimaks.simpleplayer.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import ua.mobimaks.simpleplayer.R;
import ua.mobimaks.simpleplayer.activity.MainActivity;
import ua.mobimaks.simpleplayer.model.PlayerConstants;
import ua.mobimaks.simpleplayer.service.PlayerService;

public class PlayerWidget extends AppWidgetProvider {

    private String mSongArtist, mSongTitle;
    private boolean mMusicPlaying, mUpdateTitle;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(PlayerConstants.MUSIC_STATE_CHANGED)) {
            mSongArtist = intent.getStringExtra(PlayerConstants.SONG_ARTIST);
            mSongTitle = intent.getStringExtra(PlayerConstants.SONG_TITLE);
            mMusicPlaying = intent.getBooleanExtra(PlayerConstants.MUSIC_STATE, false);
            mUpdateTitle = true;
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, PlayerWidget.class));
            onUpdate(context, appWidgetManager, appWidgetIds);
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        mUpdateTitle = false;
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                 int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.player_widget);

        if (mUpdateTitle) {
            views.setTextViewText(R.id.text_Artist_PlayerWidget, mSongArtist);
            views.setTextViewText(R.id.text_Title_PlayerWidget, mSongTitle);
            int playPauseIcon = mMusicPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play;
            views.setImageViewResource(R.id.imageButton_PlayPause_PlayerWidget, playPauseIcon);
        }

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setAction(PlayerConstants.OPEN_PLAYER);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent openPendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        Intent previousIntent = new Intent(context, PlayerService.class);
        previousIntent.setAction(PlayerConstants.PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getService(context, 0, previousIntent, 0);

        Intent playIntent = new Intent(context, PlayerService.class);
        playIntent.setAction(PlayerConstants.PLAY_PAUSE);
        PendingIntent playPendingIntent = PendingIntent.getService(context, 0, playIntent, 0);

        Intent nextIntent = new Intent(context, PlayerService.class);
        nextIntent.setAction(PlayerConstants.NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(context, 0, nextIntent, 0);

        views.setOnClickPendingIntent(R.id.layout_SongInfo_PlayerWidget, openPendingIntent);
        views.setOnClickPendingIntent(R.id.imageButton_Previous_PlayerWidget, prevPendingIntent);
        views.setOnClickPendingIntent(R.id.imageButton_PlayPause_PlayerWidget, playPendingIntent);
        views.setOnClickPendingIntent(R.id.imageButton_Next_PlayerWidget, nextPendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}


