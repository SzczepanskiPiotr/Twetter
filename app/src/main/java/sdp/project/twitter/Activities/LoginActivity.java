package sdp.project.twitter.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.UnsupportedEncodingException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import sdp.project.tweeter.R;
import sdp.project.twitter.API.APIUrl;
import sdp.project.twitter.API.Result;
import sdp.project.twitter.Utils.SaveSettings;


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // UI references.
    private EditText etName;
    private EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        etName = findViewById(R.id.etName);
        etPassword = findViewById(R.id.etPassword);
        TextView goToRegister = findViewById(R.id.goToRegister);
        goToRegister.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(i);
        });

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(instanceIdResult -> {
            String token = instanceIdResult.getToken();
            SaveSettings.getInstance(getApplicationContext()).storeToken(token);
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
        String name = "";
        try {
            //for space with name
            name = java.net.URLEncoder.encode(etName.getText().toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {

        }
        if (etName.getText().toString().equals("") || etPassword.getText().toString().equals("")) {
            hideProgressDialog();
            Toast.makeText(getApplicationContext(), "One of the fields is empty!", Toast.LENGTH_SHORT).show();

        } else {
            //defining the call
            Call<Result> call = APIUrl.getApi().loginUser(name, etPassword.getText().toString(), SaveSettings.getInstance(getApplicationContext()).getToken());
            //calling the api
            call.enqueue(new Callback<Result>() {

                @Override
                public void onResponse(Call<Result> call, Response<Result> response) {
                    //hiding progress dialog
                    hideProgressDialog();
                    Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    if (!response.body().getError()) {
                        SaveSettings.getInstance(getApplicationContext()).userLogin(response.body().getUser());
                        SaveSettings.getInstance(getApplicationContext()).saveArrayList(response.body().getFollowing(),"FOLLOWING");
                        for(int i : response.body().getFollowing())
                        {
                            FirebaseMessaging.getInstance().subscribeToTopic(String.valueOf(i))
                                    .addOnCompleteListener(task -> {
                                        String msg = "Following on login: " + i;
                                        if (!task.isSuccessful()) {
                                            msg = "Failed following on login: " + i;
                                        }
                                        Log.d(TAG, msg);
                                        //Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                                    });
                        }
                        finish();
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    }
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

