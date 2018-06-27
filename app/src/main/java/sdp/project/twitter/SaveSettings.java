package sdp.project.twitter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class SaveSettings {

    Context context;
    SharedPreferences sharedRef;

    public SaveSettings(Context context){
        this.context = context;
        sharedRef = context.getSharedPreferences("myRef", Context.MODE_PRIVATE);
    }


    void SaveData(String UserID, String Username, String Email, String Password, String Picture_path){
        SharedPreferences.Editor editor = sharedRef.edit();
        editor.putString("UserID", UserID);
        editor.putString("Username", Username);
        editor.putString("Email", Email);
        editor.putString("Password", Password);
        editor.putString("Picture_path", Picture_path);
        editor.commit();
        //LoadData();
    }

    boolean LoadData(){

        String UserID = sharedRef.getString("UserID", "0");

        if(UserID.equals("0")){
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return false;
        }
        else {
            User.getInstance(context);
            return true;
        }

    }

    void ClearData(){
        User.clearUserInstance();
        sharedRef.edit().clear().commit();
        LoadData();
    }
}