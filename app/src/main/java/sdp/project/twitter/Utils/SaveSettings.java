package sdp.project.twitter.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import sdp.project.twitter.Model.User;

public class SaveSettings {

    private static final String TAG = "SaveSettings";


    private static SaveSettings settingsInstance;
    private static Context context;

    private static final String SHARED_PREF_NAME = "myRef";

    private static final String SETTINGS_USERID = "myRef_userId";
    private static final String SETTINGS_USERNAME = "myRef_username";
    private static final String SETTINGS_EMAIL = "myRef_email";
    private static final String SETTINGS_PICTUREPATH = "myRef_picturepath";
    private static final String SETTINGS_TOKEN = "myRef_token";
    private static final ArrayList<Integer> FOLLOWING = new ArrayList<>();

    private SaveSettings(Context context){
        this.context = context;
    }

    public static synchronized SaveSettings getInstance(Context context) {
        if (settingsInstance == null) {
            Log.i(TAG, "CREATING NEW INSTANCE");
            settingsInstance = new SaveSettings(context);
        }
        return settingsInstance;
    }

    public boolean storeToken(String token){
        Log.i(TAG,"STORING TOKEN: " + token);
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SETTINGS_TOKEN, token);
        editor.apply();
        return true;
    }

    public String getToken(){
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(SETTINGS_TOKEN, null);
    }

    public boolean userLogin(User user) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SETTINGS_USERID, String.valueOf(user.getUserID()));
        editor.putString(SETTINGS_USERNAME, user.getUsername());
        editor.putString(SETTINGS_EMAIL, user.getEmail());
        editor.putString(SETTINGS_PICTUREPATH, user.getPicture_path());
        editor.apply();

        return true;
    }

    /**
     *     Save and get ArrayList in SharedPreference
     */

    public void saveArrayList(ArrayList<Integer> list, String key){
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(key, json);
        editor.apply();     // This line is IMPORTANT !!!
    }

    public ArrayList<Integer> getArrayList(String key){
        Gson gson = new Gson();
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(key, null);
        Type type = new TypeToken<ArrayList<Integer>>() {}.getType();
        return gson.fromJson(json, type);
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
        for(int i : getArrayList("FOLLOWING")) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(String.valueOf(i))
                    .addOnCompleteListener(task -> {
                        String msg = "Unfollowing on logout from: " + i;
                        if (!task.isSuccessful()) {
                            msg = "Failed ufollowing from: " + i;
                        }
                        Log.d(TAG, msg);
                        //Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    });
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        return true;
    }

}