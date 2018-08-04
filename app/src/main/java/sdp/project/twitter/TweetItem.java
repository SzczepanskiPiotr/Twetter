package sdp.project.twitter;

import com.google.gson.annotations.SerializedName;

public class TweetItem {
    @SerializedName("tweet_id")
    public int tweet_id;
    @SerializedName("tweet_text")
    public String tweet_text;
    @SerializedName("tweet_picture")
    public String tweet_picture;
    @SerializedName("tweet_date")
    public String tweet_date;
    @SerializedName("user_id")
    public int user_id;
    @SerializedName("username")
    public String username;
    @SerializedName("picture_path")
    public String picture_path;
    @SerializedName("favoruiteCount")
    public int favouriteCount;
    @SerializedName("isFavourite")
    public boolean isFavourite;

    public TweetItem(int tweet_id, String tweet_text, String tweet_picture,
              String tweet_date, int user_id, String username , String picture_path,
              int favouriteCount, boolean isFavourite)
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
    }
}
