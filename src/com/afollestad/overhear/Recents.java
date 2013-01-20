package com.afollestad.overhear;

import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

/**
 * Various utilities for modifying the recent history.
 * @author Aidan Follestad
 */
public class Recents {

	public final static Uri PROVIDER_URI = Uri.parse("content://com.afollestad.overhear.recentsprovider");
	
	/**
	 * Adds a song's album to the recent history database.
	 */
	public static void add(Context context, Song song) {
		Album album = Album.getAlbum(context, song.getAlbum(), song.getArtist());
		ContentValues values = album.getContentValues(true);
		int updated = context.getContentResolver().update(PROVIDER_URI, values, "_id = " + album.getAlbumId(), null);
		if(updated == 0) {
			context.getContentResolver().insert(PROVIDER_URI, values);
		}
	}
}