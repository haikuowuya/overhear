package com.afollestad.overhear.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;
import com.afollestad.overhear.R;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.ui.NowPlayingViewer;
import com.afollestad.overhear.ui.OverviewScreen;

class NotificationViewCreator {

    @SuppressWarnings("deprecation")
    public static Notification createNotification(Context context, QueueItem nowPlaying, Bitmap art, boolean playing) {
        Notification.Builder builder = new Notification.Builder(context);
        builder.setContent(createView(context, false, nowPlaying, art, playing));
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_notify);

        Intent nowPlayingIntent = new Intent(context, NowPlayingViewer.class).
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(OverviewScreen.class);
        stackBuilder.addNextIntent(new Intent(context, OverviewScreen.class));
        stackBuilder.addNextIntent(nowPlayingIntent);
        builder.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            return NotificationViewCreator16.createNotification(context, builder, nowPlaying, art, playing);
        }

        Notification noti = builder.getNotification();
        noti.flags = Notification.FLAG_HIGH_PRIORITY;
        return noti;
    }

    static RemoteViews createView(Context context, boolean big, QueueItem nowPlaying, Bitmap art, boolean playing) {
        RemoteViews views;
        if (big)
            views = new RemoteViews(context.getPackageName(), R.layout.status_bar_big);
        else
            views = new RemoteViews(context.getPackageName(), R.layout.status_bar);
        if (art != null)
            views.setImageViewBitmap(R.id.status_bar_album_art, art);

        Intent si = new Intent(context, MusicService.class);
        si.setAction(MusicService.ACTION_SKIP);
        PendingIntent pi = PendingIntent.getService(context, 3, si, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.status_bar_next, pi);

        si.setAction(MusicService.ACTION_TOGGLE_PLAYBACK);
        pi = PendingIntent.getService(context, 2, si, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.status_bar_play, pi);

        si.setAction(MusicService.ACTION_STOP);
        pi = PendingIntent.getService(context, 4, si, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.status_bar_collapse, pi);

        if (playing)
            views.setImageViewResource(R.id.status_bar_play, R.drawable.ic_pause);
        else
            views.setImageViewResource(R.id.status_bar_play, R.drawable.ic_play);

        views.setTextViewText(R.id.status_bar_track_name, nowPlaying.getTitle(context));
        views.setTextViewText(R.id.status_bar_artist_name, nowPlaying.getArtist(context));
        if (big) {
            views.setTextViewText(R.id.status_bar_album_name, nowPlaying.getAlbum(context));
            si.setAction(MusicService.ACTION_REWIND);
            pi = PendingIntent.getService(context, 1, si, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.status_bar_previous, pi);
        }

        return views;
    }
}
