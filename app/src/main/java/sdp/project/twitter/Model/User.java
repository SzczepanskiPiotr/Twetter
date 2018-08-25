package sdp.project.twitter.Model;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("user_id")
    private String UserID;
    @SerializedName("username")
    private String Username;
    @SerializedName("email")
    private String Email;
    @SerializedName("picture_path")
    private String Picture_path;

    public User(String user_id, String username, String email, String picture_path) {
        this.UserID = user_id;
        this.Username = username;
        this.Email = email;
        this.Picture_path = picture_path;
    }

    public int getUserID() {
        return Integer.parseInt(UserID);
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

}
