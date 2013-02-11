package com.afollestad.overhear.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.R;
import com.afollestad.overhear.Twitter;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.adapters.ArtistAdapter;
import com.afollestad.overhear.fragments.NowPlayingBarFragment;
import com.afollestad.overhear.fragments.SongListFragment;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import org.json.JSONException;
import org.json.JSONObject;

public class AlbumViewer extends Activity {

    private Album album;
    private Artist artist;

    public final static int TWEET_PLAYING_LOGIN = 400;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TWEET_PLAYING_LOGIN && resultCode == Activity.RESULT_OK) {
            startActivity(new Intent(this, TweetNowPlaying.class));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_album_viewer);
        load();

        if (savedInstanceState == null) {
            // First-time init; create fragment to embed in activity.
            FragmentTransaction ft = getFragmentManager().beginTransaction();

            Fragment songFrag = new SongListFragment();
            Bundle args = new Bundle();
            args.putInt("album_id", album.getAlbumId());
            args.putBoolean("show_artist", false);
            songFrag.setArguments(args);
            ft.add(R.id.songList, songFrag);

            Fragment nowFrag = new NowPlayingBarFragment();
            args = new Bundle();
            args.putBoolean("disable_long_click", true);
            nowFrag.setArguments(args);
            ft.add(R.id.nowPlaying, nowFrag);

            ft.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.album_viewer, menu);
        return true;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra("album")) {
            setIntent(intent);
            load();
        }
    }

    private void load() {
        try {
            album = Album.fromJSON(this, new JSONObject(getIntent().getStringExtra("album")));
            artist = album.getArtist();
        } catch (JSONException e) {
            throw new java.lang.Error(e.getMessage());
        }

        if (findViewById(R.id.artistCover) != null) {
            ((TextView) findViewById(R.id.artistName)).setText(artist.getName());
            setTitle(album.getName());

            findViewById(R.id.artistCover).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(AlbumViewer.this, ArtistViewer.class)
                            .putExtra("artist", artist.getJSON().toString()));
                }
            });
            ArtistAdapter.retrieveArtistArt(this, artist, (AImageView) findViewById(R.id.artistCover));
        }

        AlbumAdapter.retrieveAlbumArt(this, album, (AImageView) findViewById(R.id.albumCover));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.shopArtist:
                MusicUtils.browseArtist(getApplicationContext(), artist.getName());
                return true;
            case R.id.tweetPlaying:
                if (Twitter.getTwitterInstance(getApplicationContext(), true) == null)
                    startActivityForResult(new Intent(this, LoginHandler.class), TWEET_PLAYING_LOGIN);
                else
                    startActivity(new Intent(this, TweetNowPlaying.class));
                return true;
            case R.id.search:
                startActivity(new Intent(this, SearchScreen.class));
                return true;
        }
        return false;
    }
}