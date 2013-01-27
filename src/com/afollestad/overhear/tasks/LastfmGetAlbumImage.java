package com.afollestad.overhear.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.App;
import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.WebArtUtils;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.LastFM;

import java.lang.ref.WeakReference;

public class LastfmGetAlbumImage extends AsyncTask<Album, Integer, String> {

    private WeakReference<Activity> context;
    private WeakReference<AImageView> view;
    private String tag;
   
    public LastfmGetAlbumImage(Activity context, AImageView view) {
        this.context = new WeakReference<Activity>(context);
        this.view = new WeakReference<AImageView>(view);
        this.tag = (String)view.getTag();
    }

    @Override
    protected String doInBackground(Album... als) {
        String url = WebArtUtils.getImageURL(context.get(), als[0]);
        if(url == null) {
            url = als[0].getAlbumArtUri(context.get()).toString();
        }
        if (url == null && MusicUtils.isOnline(context.get())) {
            try {
                Log.i("Overhear", "Getting album information from LastFM for: " + als[0].getName() + " by " + als[0].getArtist());
                url = LastFM.getAlbumInfo(als[0].getArtist().getName(), als[0].getName()).getCoverImageURL();
                WebArtUtils.setImageURL(context.get(), als[0], url);
                return url;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    @Override
    protected void onPostExecute(String result) {
        if(view != null && view.get() != null && result != null && (tag == null || tag.equals(view.get().getTag()))) {
            view.get().setAImageSource(((App)context.get().getApplication()).getManager(), result);
        }
        super.onPostExecute(result);
    }
}