package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.Overhear;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.base.OverhearListActivity;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.tasks.LastfmGetAlbumImage;
import com.afollestad.overhear.ui.ArtistViewer;
import com.afollestad.overhear.utils.MusicUtils;
import com.afollestad.overhear.utils.ViewUtils;
import com.afollestad.overhear.utils.WebArtUtils;
import com.afollestad.overhearapi.Album;
import com.afollestad.silk.adapters.SilkCursorAdapter;
import com.afollestad.silk.views.image.SilkImageView;

import java.util.ArrayList;

public class AlbumAdapter extends SilkCursorAdapter<Album> {

    public AlbumAdapter(Activity context, Cursor c) {
        super(context, R.layout.album_item, c, new CursorConverter<Album>() {
            @Override
            public Album convert(Cursor cursor) {
                return Album.fromCursor(cursor);
            }
        });
    }

    public static void retrieveAlbumArt(Activity context, Album album, SilkImageView view) {
        view.setImageBitmap(null);
        if (album == null) {
            view.showFallback(Overhear.get(context).getManager());
            return;
        }
        String url = WebArtUtils.getImageURL(context, album);
        if (url == null) {
            new LastfmGetAlbumImage(context, context.getApplication(), view, false).execute(album);
        } else {
            view.setImageURL(Overhear.get(context).getManager(), url);
        }
    }

    public static View getViewForAlbum(final Activity context, final Album album, View view, int scrollState) {
        if (view == null)
            view = LayoutInflater.from(context).inflate(R.layout.album_item, null);
        ((TextView) view.findViewById(R.id.title)).setText(album.getName());
        ((TextView) view.findViewById(R.id.artist)).setText(album.getArtist().getName());

        final SilkImageView image = (SilkImageView) view.findViewById(R.id.image);
        if (scrollState > -1 && scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE)
            retrieveAlbumArt(context, album, image);
        else image.setImageBitmap(null);

        View options = view.findViewById(R.id.options);
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu menu = new PopupMenu(context, view);
                menu.inflate(R.menu.album_item_popup);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.addToPlaylist: {
                                AlertDialog diag = MusicUtils.createPlaylistChooseDialog(context, null, album, null);
                                diag.show();
                                return true;
                            }
                            case R.id.playAll: {
                                context.startService(new Intent(context, MusicService.class)
                                        .setAction(MusicService.ACTION_PLAY_ALL)
                                        .putExtra("scope", QueueItem.SCOPE_ALBUM)
                                        .putExtra("album", album.getJSON().toString()));
                                return true;
                            }
                            case R.id.addToQueue: {
                                ArrayList<QueueItem> content = QueueItem.getAll(context,
                                        MediaStore.Audio.Media.IS_MUSIC + " = 1 AND " +
                                                MediaStore.Audio.Media.ALBUM_ID + " = " + album.getAlbumId(),
                                        MediaStore.Audio.Media.TRACK, -1, QueueItem.SCOPE_ALBUM);
                                MusicUtils.addToQueue(context, content);
                                return true;
                            }
                            case R.id.redownloadArt: {
                                new LastfmGetAlbumImage(context, context.getApplication(), image, true).execute(album);
                                return true;
                            }
                            case R.id.viewArtist: {
                                context.startActivity(new Intent(context, ArtistViewer.class)
                                        .putExtra("artist", album.getArtist().getJSON().toString()));
                                return true;
                            }
                        }
                        return false;
                    }
                });
                menu.show();
            }
        });

        ImageView peakOne = (ImageView) view.findViewById(R.id.peak_one);
        ImageView peakTwo = (ImageView) view.findViewById(R.id.peak_two);
        peakOne.setImageResource(R.anim.peak_meter_1);
        peakTwo.setImageResource(R.anim.peak_meter_2);
        AnimationDrawable mPeakOneAnimation = (AnimationDrawable) peakOne.getDrawable();
        AnimationDrawable mPeakTwoAnimation = (AnimationDrawable) peakTwo.getDrawable();

        QueueItem focused = null;
        boolean isPlaying = false;
        if (context instanceof OverhearActivity) {
            if (((OverhearActivity) context).getService() != null) {
                focused = ((OverhearActivity) context).getService().getQueue().getFocused();
                isPlaying = ((OverhearActivity) context).getService().isPlaying();
            }
        } else {
            if (((OverhearListActivity) context).getService() != null) {
                focused = ((OverhearListActivity) context).getService().getQueue().getFocused();
                isPlaying = ((OverhearListActivity) context).getService().isPlaying();
            }
        }
        if (focused != null && album.getName().equals(focused.getAlbum(context)) &&
                album.getArtist().getName().equals(focused.getArtist(context))) {
            peakOne.setVisibility(View.VISIBLE);
            peakTwo.setVisibility(View.VISIBLE);
            if (isPlaying) {
                if (!mPeakOneAnimation.isRunning()) {
                    mPeakOneAnimation.start();
                    mPeakTwoAnimation.start();
                }
            } else {
                mPeakOneAnimation.stop();
                mPeakOneAnimation.selectDrawable(0);
                mPeakTwoAnimation.stop();
                mPeakTwoAnimation.selectDrawable(0);
            }
        } else {
            peakOne.setVisibility(View.GONE);
            peakTwo.setVisibility(View.GONE);
            if (mPeakOneAnimation.isRunning()) {
                mPeakOneAnimation.stop();
                mPeakTwoAnimation.stop();
            }
        }

        return view;
    }

    @Override
    public View onViewCreated(int index, View recycled, Album item) {
        recycled = getViewForAlbum((Activity) getContext(), item, recycled, getScrollState());
        int pad = getContext().getResources().getDimensionPixelSize(R.dimen.list_top_padding);
        if (index == 0) {
            if (getCount() == 1) {
                ViewUtils.relativeMargins(recycled, pad, pad);
            } else {
                ViewUtils.relativeMargins(recycled, pad, 0);
            }
        } else if (index == getCount() - 1) {
            ViewUtils.relativeMargins(recycled, 0, pad);
        } else {
            ViewUtils.relativeMargins(recycled, 0, 0);
        }
        return recycled;
    }
}