package sdp.project.twitter.Model;

import com.google.gson.annotations.SerializedName;

public class TweetItem {
    @SerializedName("tweet_id")
    private int tweet_id;
    @SerializedName("tweet_text")
    private String tweet_text;
    @SerializedName("tweet_picture")
    private String tweet_picture;
    @SerializedName("tweet_date")
    private String tweet_date;
    @SerializedName("user_id")
    private int user_id;
    @SerializedName("username")
    private String username;
    @SerializedName("picture_path")
    private String picture_path;
    @SerializedName("favoruiteCount")
    private int favouriteCount;
    @SerializedName("isFavourite")
    private boolean isFavourite;
    @SerializedName("latitude")
    private float latitude;
    @SerializedName("longitude")
    private float longitude;
    @SerializedName("country")
    private String country;
    @SerializedName("city")
    private String city;

    private Location tweetLocation;

    public TweetItem(int tweet_id, String tweet_text, String tweet_picture,
              String tweet_date, int user_id, String username , String picture_path,
              int favouriteCount, boolean isFavourite, float latitude, float longitude, String country, String city)
    {
        this.tweet_id = tweet_id;
        this.tweet_text = tweet_text;
        this.tweet_picture = tweet_picture;
        this.user_id = user_id;
        this.username = username;
        this.picture_path = picture_path;
        this.tweet_date = tweet_date;
        this.favouriteCount = favouriteCount;
        this.isFavourite = isFavourite;
        this.country = country;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tweetLocation = new Location(latitude,longitude,country,city);
    }

    public int getTweet_id() {
        return tweet_id;
    }

    public void setTweet_id(int tweet_id) {
        this.tweet_id = tweet_id;
    }

    public String getTweet_text() {
        return tweet_text;
    }

    public void setTweet_text(String tweet_text) {
        this.tweet_text = tweet_text;
    }

    public String getTweet_picture() {
        return tweet_picture;
    }

    public void setTweet_picture(String tweet_picture) {
        this.tweet_picture = tweet_picture;
    }

    public String getTweet_date() {
        return tweet_date;
    }

    public void setTweet_date(String tweet_date) {
        this.tweet_date = tweet_date;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPicture_path() {
        return picture_path;
    }

    public void setPicture_path(String picture_path) {
        this.picture_path = picture_path;
    }

    public int getFavouriteCount() {
        return favouriteCount;
    }

    public void setFavouriteCount(int favouriteCount) {
        this.favouriteCount = favouriteCount;
    }

    public boolean isFavourite() {
        return isFavourite;
    }

    public void setFavourite(boolean favourite) {
        isFavourite = favourite;
    }

    public Location getTweetLocation() {
        return tweetLocation;
    }

    public void setTweetLocation(Location tweetLocation) {
        this.tweetLocation = tweetLocation;
    }

    public void setTweetLocationFromData(){
        if((country != null && city != null))
            if(!city.equals("XX") && !country.equals("XX"))
                this.tweetLocation = new Location(latitude,longitude,country,city);
    }
}
