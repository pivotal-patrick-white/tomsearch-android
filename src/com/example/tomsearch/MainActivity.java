package com.example.tomsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private RelativeLayout rl;
    private LruCache<String, Bitmap> imageCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int cacheMemory = ((int) (Runtime.getRuntime().maxMemory() / 1024)) / 8;
        imageCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap bmp) {
                return bmp.getByteCount() / 1024;
            }
        };

        search(findViewById(R.id.button1));
        findViewById(R.id.button1).requestFocus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;

    }

    public void search(View view) {
        Log.i(MainActivity.class.toString(), "Search");
        EditText et = (EditText) findViewById(R.id.editText1);
        et.clearFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
        rl = (RelativeLayout) findViewById(R.id.spinnerContainer);
        rl.setVisibility(View.VISIBLE);
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadResultsTask(et.getText().toString()).execute();
        } else {
            Toast err = Toast.makeText(getApplicationContext(), getResources().getString(R.string.noconn), Toast.LENGTH_SHORT);
            err.show();
            rl.setVisibility(View.GONE);
        }
    }

    public void setListContents(MovieData[] movies) {
        Log.i(DownloadResultsTask.class.getName(), "\"okay!\"");
        MovieAdapter adapter = new MovieAdapter(this, movies);
        ListView lv = (ListView) findViewById(R.id.listView1);
        lv.setAdapter(adapter);
    }

    private class DownloadResultsTask extends AsyncTask<String, Void, String> {

        private String query;

        public DownloadResultsTask(String query) {
            this.query = query;
        }

        @Override
        protected String doInBackground(String... params) {
            StringBuilder builder = new StringBuilder();
            String encodedQuery = "";
            try {
                encodedQuery = URLEncoder.encode(query, "utf-8");
            } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            String url = "http://api.rottentomatoes.com/api/public/v1.0/movies.json?q=" + encodedQuery
                    + "&page_limit=10&page=1&apikey=7er6em5vc84hq6my9kr3t6ga";
            if (encodedQuery.equals("")) {
                url = "http://api.rottentomatoes.com/api/public/v1.0/lists/movies/box_office.json?limit=16&country=us&apikey=7er6em5vc84hq6my9kr3t6ga";
            }
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
            rl.setVisibility(View.GONE);
            try {
                JSONObject jo = new JSONObject(result);
                JSONArray movies = jo.getJSONArray("movies");
                ArrayList<MovieData> movieList = new ArrayList<MovieData>();
                for (int i = 0; i < movies.length(); i++) {
                    JSONObject movie = movies.getJSONObject(i);
                    movieList.add(new MovieData(movie));
                }
                setListContents(movieList.toArray(new MovieData[movieList.size()]));
            } catch (JSONException e) {
                Log.e(">:C", "jsonexception");
            }
        }
    }

    private class MovieAdapter extends ArrayAdapter<MovieData> {

        private Context context;
        private MovieData[] values;

        public MovieAdapter(Context context, MovieData[] objects) {
            super(context, R.layout.movie_list_layout, objects);
            this.context = context;
            this.values = objects;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Log.i("MovieAdapter", "called");
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = inflater.inflate(R.layout.movie_list_layout, parent, false);
            TextView titleView = (TextView) rowView.findViewById(R.id.textView1);
            TextView subView = (TextView) rowView.findViewById(R.id.textView2);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView1);
            MovieData data = values[position];
            Bitmap bmp = imageCache.get(data.getId());
            if (bmp == null) {
                new DownloadImageTask(data.getId(), imageView).execute(data.getImageSrc());
            } else {
                imageView.setImageBitmap(bmp);
            }
            titleView.setText(data.getName());
            subView.setText(data.getTag());
            return rowView;
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        private String id;
        private ImageView target;

        public DownloadImageTask(String id, ImageView iv) {
            super();
            this.id = id;
            this.target = iv;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bmp = null;
            try {
                URL url = new URL(params[0]);
                bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return bmp;
        }

        @Override
        protected void onPostExecute(Bitmap bmp) {
            target.setImageBitmap(bmp);
            imageCache.put(id, bmp);
        }
    }

}
