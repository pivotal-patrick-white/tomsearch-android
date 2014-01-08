package com.example.tomsearch;

import org.json.JSONException;
import org.json.JSONObject;

public class MovieData {

    private String id;
    private String name;
    private String year;
    private String rating;
    private String mpaa;
    private String imageSrc;

    public MovieData(String id, String name, String year, String rating, String mpaa, String imageSrc) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.imageSrc = imageSrc;
        this.rating = rating;
        this.mpaa = mpaa;
    }

    public MovieData(JSONObject movie) {
        try {
            JSONObject posters = movie.getJSONObject("posters");
            id = movie.getString("id");
            name = movie.getString("title");
            year = movie.getString("year");
            rating = movie.getJSONObject("ratings").getString("critics_score");
            mpaa = movie.getString("mpaa_rating");
            imageSrc = posters.getString("detailed");
        } catch (JSONException e) {
            // error
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name + " (" + year + ")";
    }

    public String getTag() {
        return "Critic Score: " + (rating.equals("-1") ? "N/A" : rating + "%") + " | MPAA: " + mpaa;
    }

    public String getImageSrc() {
        return imageSrc;
    }
}
