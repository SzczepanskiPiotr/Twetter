package sdp.project.twitter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class User {

    private SharedPreferences sharedRef;
    private volatile static User userInstance;
    private String UserID;
    private String Username;
    private String Email;
    private String Password;
    private String Picture_path;

    private User(Context context) {
        sharedRef = context.getSharedPreferences("myRef", Context.MODE_PRIVATE);
        this.UserID = sharedRef.getString("UserID", "0");;
        this.Username = sharedRef.getString("Username", "0");;
        this.Email = sharedRef.getString("Email", "0");;
        this.Password = sharedRef.getString("Password", "0");;
        this.Picture_path = sharedRef.getString("Picture_path", "0");;
    }

    public static User getInstance(Context context) {
        if (userInstance == null) {
            synchronized (User.class) {
                if (userInstance == null) {
                    userInstance = new User(context);
                    Log.i("LOL", "CREATED USER");
                }
            }
        }
        return userInstance;
    }

    public static void clearUserInstance(){
        userInstance = null;
    }

    public String getUserID() {
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

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public String getPicture_path() {
        return Picture_path;
    }

    public void setPicture_path(String picture_path) {
        Picture_path = picture_path;
    }

}
