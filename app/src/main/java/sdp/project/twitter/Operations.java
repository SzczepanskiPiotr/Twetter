package sdp.project.twitter;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Operations {
    Context context;
    public  Operations(Context context){
        this.context=context;
    }
    // this method convert any stream to string
    public static String ConvertInputToStringNoChange(InputStream inputStream) {

        BufferedReader bureader = new BufferedReader( new InputStreamReader(inputStream));
        String line;
        String finalInput="";

        try{
            while((line=bureader.readLine())!=null) {

                finalInput+=line;

            }
            inputStream.close();


        }catch (Exception ex){}

        return finalInput;
    }
}
