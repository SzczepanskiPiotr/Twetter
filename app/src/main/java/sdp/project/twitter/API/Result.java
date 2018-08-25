package sdp.project.twitter.API;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

import sdp.project.twitter.Model.TweetItem;
import sdp.project.twitter.Model.User;

public class Result {
    @SerializedName("error")
    private Boolean error;

    @SerializedName("message")
    private String message;

    @SerializedName("user")
    private User user;

    @SerializedName("tweets")
    private ArrayList<TweetItem> t;


    public Result(Boolean error, String message, User user, ArrayList<TweetItem> t) {
        this.error = error;
        this.message = message;
        this.user = user;
        this.t = t;
    }

    public Boolean getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }

    public ArrayList<TweetItem> getTweets() {
        return t;
    }
}
