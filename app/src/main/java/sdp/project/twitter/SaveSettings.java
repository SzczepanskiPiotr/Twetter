package sdp.project.twitter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class SaveSettings {

    private static SaveSettings settingsInstance;
    private static Context context;

    private static final String SHARED_PREF_NAME = "myRef";

    private static final String SETTINGS_USERID = "";
    private static final String SETTINGS_USERNAME = "";
    private static final String SETTINGS_EMAIL = "";
    private static final String SETTINGS_PICTUREPATH = "";

    private SaveSettings(Context context){
        this.context = context;
    }
    public static synchronized SaveSettings getInstance(Context context) {
        if (settingsInstance == null) {
            settingsInstance = new SaveSettings(context);
        }
        return settingsInstance;
    }


    public boolean userLogin(User user) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SETTINGS_USERID, user.getUserID());
        editor.putString(SETTINGS_USERNAME, user.getUsername());
        editor.putString(SETTINGS_EMAIL, user.getEmail());
        editor.putString(SETTINGS_PICTUREPATH, user.getPicture_path());
        editor.apply();
        return true;
    }

    public boolean isLoggedIn() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        if (sharedPreferences.getString(SETTINGS_EMAIL, null) != null)
            return true;
        return false;
    }

    public User getUser() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return new User(
                sharedPreferences.getString(SETTINGS_USERID, null),
                sharedPreferences.getString(SETTINGS_USERNAME, null),
                sharedPreferences.getString(SETTINGS_EMAIL, null),
                sharedPreferences.getString(SETTINGS_PICTUREPATH, null)
        );
    }

    public boolean logout() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        return true;
    }
}