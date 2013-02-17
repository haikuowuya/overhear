package com.afollestad.overhear.ui;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ListView;
import android.widget.SearchView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.SearchAdapter;
import com.afollestad.overhear.fragments.SongListFragment;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Song;

import java.util.ArrayList;

public class SearchScreen extends ListActivity {

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    };


    protected SearchAdapter adapter;
    private Handler mHandler = new Handler();
    private String lastQuery;
    private Runnable searchRunner = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    search(lastQuery);
                }
            });
        }
    };

    private Cursor openCursor(Uri uri, String query, String column) {
        query = query.replace("'", "\\'").replace("%", "\\%").replace("_", "\\_");
        return getContentResolver().query(
                uri,
                null,
                column + " LIKE ('%" + query + "%') ESCAPE '\\'",
                null,
                column + " LIMIT 10");
    }

    public void search(String query) {
        adapter.clear();
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        Cursor cursor = openCursor(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, query, MediaStore.Audio.AlbumColumns.ALBUM);
        ArrayList<Album> albums = new ArrayList<Album>();
        while (cursor.moveToNext()) {
            albums.add(Album.fromCursor(getApplicationContext(), cursor));
        }
        if (albums.size() > 0)
            adapter.add("Albums", albums.toArray());
        cursor.close();

        cursor = openCursor(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, query, MediaStore.Audio.ArtistColumns.ARTIST);
        ArrayList<Artist> artists = new ArrayList<Artist>();
        while (cursor.moveToNext()) {
            artists.add(Artist.fromCursor(cursor));
        }
        if (artists.size() > 0)
            adapter.add("Artists", artists.toArray());
        cursor.close();

        cursor = openCursor(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, query, MediaStore.Audio.Media.TITLE);
        ArrayList<Song> songs = new ArrayList<Song>();
        while (cursor.moveToNext()) {
            songs.add(Song.fromCursor(cursor));
        }
        if (songs.size() > 0)
            adapter.add("Songs", songs.toArray());
        cursor.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_search);
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
        registerReceiver(mStatusReceiver, filter);
        adapter = new SearchAdapter(this);
        setListAdapter(adapter);
        getListView().setFastScrollEnabled(true);
        getListView().setSmoothScrollbarEnabled(true);
        getListView().setEmptyView(findViewById(android.R.id.empty));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        switch (adapter.getItemViewType(position)) {
            case 1:
                SongListFragment.performOnClick(this, (Song) adapter.getItem(position), null);
                break;
            case 2:
                startActivity(new Intent(this, AlbumViewer.class).putExtra("album",
                        ((Album) adapter.getItem(position)).getJSON().toString()));
                break;
            case 3:
                startActivity(new Intent(this, ArtistViewer.class).putExtra("artist",
                        ((Artist) adapter.getItem(position)).getJSON().toString()));
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_screen, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                lastQuery = s;
                mHandler.removeCallbacks(searchRunner);
                mHandler.postDelayed(searchRunner, 250);
                return false;
            }
        });
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.requestFocus();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mStatusReceiver);
    }
}