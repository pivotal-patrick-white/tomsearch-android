package com.example.tomsearch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.util.LruCache;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

public class DetailsActivity extends Activity {

    private LruCache<String, Bitmap> imageCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        imageCache = ((TSApplication) getApplication()).getImageCache();
        if (imageCache == null) {
            int cacheMemory = ((int) (Runtime.getRuntime().maxMemory() / 1024)) / 8;
            imageCache = new LruCache<String, Bitmap>(cacheMemory) {
                @Override
                protected int sizeOf(String key, Bitmap bmp) {
                    return bmp.getByteCount() / 1024;
                }
            };

            ((TSApplication) getApplication()).setImageCache(imageCache);
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();

        TextView movietitle = (TextView) findViewById(R.id.movietitle);
        movietitle.setText(intent.getStringExtra(MainActivity.MOVIE_TITLE_MESSAGE));
        String id = intent.getStringExtra(MainActivity.MOVIE_ID_MESSAGE);

        ImageView movieposter = (ImageView) findViewById(R.id.movieposter);

        movieposter.setImageBitmap(imageCache.get(id));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
