package sdp.project.twitter;

public class TweetItem {
    public String tweet_id;
    public String tweet_text;
    public String tweet_picture;
    public String tweet_date;
    public String user_id;
    public String username;
    public String picture_path;

    public boolean isFavourite;
    public String favouriteCount="";

    TweetItem(String tweet_id, String tweet_text, String tweet_picture,
              String tweet_date, String user_id, String username , String picture_path)
    {
        this.tweet_id = tweet_id;
        this.tweet_text = tweet_text;
        this.tweet_picture = tweet_picture;
        this.user_id = user_id;
        this.username = username;
        this.picture_path = picture_path;
        this.tweet_date = tweet_date;
    }
}
