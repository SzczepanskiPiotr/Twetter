package sdp.project.twitter;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import sdp.project.tweeter.R;
import sdp.project.twitter.API.APIService;
import sdp.project.twitter.API.APIUrl;

public class LoginActivity extends AppCompatActivity {

    // UI references.
    private EditText etName;
    private EditText etPassword;
    private TextView goToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etName = findViewById(R.id.etName);
        etPassword = findViewById(R.id.etPassword);
        goToRegister = findViewById(R.id.goToRegister);
        goToRegister.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),RegisterActivity.class);
                startActivity(i);
            }
        });
    }

    @VisibleForTesting
    public ProgressDialog mProgressDialog;

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("loading");
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public void buLogin(View view) {
        showProgressDialog();
        String name="";
        try {
            //for space with name
            name = java.net.URLEncoder.encode( etName.getText().toString() , "UTF-8");
        } catch (UnsupportedEncodingException e) {

        }
        if(etName.getText().toString().equals("") || etPassword.getText().toString().equals("")){
            hideProgressDialog();
            Toast.makeText( getApplicationContext(),"One of the fields is empty!" , Toast.LENGTH_SHORT).show();

        }else{
                //building retrofit object
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(APIUrl.BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                //Defining retrofit api service
                APIService service = retrofit.create(APIService.class);

                //defining the call
                Call<Result> call = service.loginUser(name, etPassword.getText().toString());

                //calling the api
                call.enqueue(new Callback<Result>() {
                    @Override
                    public void onResponse(Call<Result> call, Response<Result> response) {
                        //hiding progress dialog
                        hideProgressDialog();
                        if(!response.body().getError()){
                            finish();
                            SaveSettings.getInstance(getApplicationContext()).userLogin(response.body().getUser());
                        }
                        //displaying the message from the response as toast
                        Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Call<Result> call, Throwable t) {
                        hideProgressDialog();
                        Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        }
    }
}

