package sdp.project.twitter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class SaveSettings {

    public static String UserID="";

    Context context;
    SharedPreferences sharedRef;

    public SaveSettings(Context context){
        this.context = context;
        sharedRef = context.getSharedPreferences("myRef", Context.MODE_PRIVATE);
    }


    void SaveData(String UserID){
        this.UserID = UserID;
        SharedPreferences.Editor editor = sharedRef.edit();
        editor.putString("UserID", UserID);
        editor.commit();
        LoadData();
    }

    void LoadData(){
        UserID = sharedRef.getString("UserID", "0");

        if(UserID.equals("0")){
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        //else User.getInstance(context);
    }

    void ClearData(){
        sharedRef.edit().clear().commit();
        LoadData();
    }
}
