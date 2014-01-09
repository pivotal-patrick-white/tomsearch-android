package com.example.tomsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
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

        new DownloadDetailsTask(id).execute();
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

    private class DownloadDetailsTask extends AsyncTask<String, Void, String> {

        private String id;

        public DownloadDetailsTask(String id) {
            this.id = id;
        }

        @Override
        protected String doInBackground(String... params) {
            StringBuilder builder = new StringBuilder();

            String url = "http://api.rottentomatoes.com/api/public/v1.0/movies/" + id + ".json?apikey=7er6em5vc84hq6my9kr3t6ga";
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet get = new HttpGet(url);
                HttpResponse response = client.execute(get);
                StatusLine status = response.getStatusLine();
                if (status.getStatusCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream is = entity.getContent();
                    Header[] headers = response.getHeaders("Content-Encoding");
                    if (headers.length > 0) {
                        if (headers[0].getValue().equals("gzip"))
                            is = new GZIPInputStream(is);
                    }
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = br.readLine()) != null) {
                        builder.append(line);
                    }
                } else {
                    Log.e(">:C", "failed to download");
                    return "error: failed to download";
                }
            } catch (IOException e) {
                Log.e(">:C", "ioexception");
                return "error: ioexception";
            }
            return builder.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jo = new JSONObject(result);
                RelativeLayout spinner = (RelativeLayout) findViewById(R.id.spinnerContainer);
                ScrollView descriptionContainer = (ScrollView) findViewById(R.id.descriptionContainer);
                descriptionContainer.setVisibility(View.VISIBLE);
                TextView description = (TextView) findViewById(R.id.moviedescription);

                // text view content
                String descriptionText = "MPAA: " + jo.get("mpaa_rating") + "\nRuntime: " + jo.get("runtime") + " minutes";
                String castText = "";
                JSONArray cast = jo.getJSONArray("abridged_cast");
                if (cast.length() == 1) {
                    castText = "\nWith " + ((JSONObject) cast.get(0)).get("name") + ".";
                } else if (cast.length() > 1) {
                    castText = "\nWith ";
                    for (int i = 0; i < cast.length() - 2; i++) {
                        castText += ((JSONObject) cast.get(i)).get("name") + ", ";
                    }
                    castText += ((JSONObject) cast.get(cast.length() - 2)).get("name") + " and "
                            + ((JSONObject) cast.get(cast.length() - 1)).get("name") + ".";
                }
                descriptionText += castText + "\n\n" + jo.get("synopsis");
                description.setText(descriptionText);
                TextView criticsRating = (TextView) findViewById(R.id.criticrating);
                int score = (Integer) jo.getJSONObject("ratings").get("critics_score");
                criticsRating.setText("Critic Score: " + (score == -1 ? "N/A" : score + "%"));
                TextView audienceRating = (TextView) findViewById(R.id.audiencerating);
                score = (Integer) jo.getJSONObject("ratings").get("audience_score");
                audienceRating.setText("Audience Score: " + (score == -1 ? "N/A" : score + "%"));

                spinner.setVisibility(View.GONE);
                descriptionContainer.setVisibility(View.VISIBLE);
            } catch (JSONException e) {
                Log.e(">:C", "jsonexception");
                e.printStackTrace();
            }
        }
    }
}
