package sdp.project.twitter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.annotations.SerializedName;

public class User {

    private int UserID;
    private String Username;
    private String Email;
    private String Picture_path;

    public User(int UserID, String Username, String Email, String Picture_path) {
        this.UserID = UserID;
        this.Username = Username;
        this.Email = Email;
        this.Picture_path = Picture_path;
    }

    public int getUserID() {
        return UserID;
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getPicture_path() {
        return Picture_path;
    }

    public void setPicture_path(String picture_path) {
        Picture_path = picture_path;
    }

    @SerializedName("error")
    private Boolean error;

    @SerializedName("message")
    private String message;

    @SerializedName("user")
    private User user;

}
