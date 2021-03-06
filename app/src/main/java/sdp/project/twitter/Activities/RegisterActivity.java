package sdp.project.twitter.Activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.apache.commons.validator.routines.EmailValidator;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import sdp.project.tweeter.R;
import sdp.project.twitter.API.*;
import sdp.project.twitter.API.Result;
import sdp.project.twitter.Utils.SaveSettings;

import static sdp.project.twitter.Activities.MainActivity.getExifRotation;

public class RegisterActivity extends AppCompatActivity {

    EditText etName;
    EditText etEmail;
    EditText etPassword;
    ImageView ivUserImage;

    //firebase
    private static final String TAG = "RegisterActivity";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        getSupportActionBar().hide();
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivUserImage = findViewById(R.id.ivUserImage);
        ivUserImage.setOnClickListener(v -> CheckUserPermission());

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                // User is signed in
                Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
            } else {
                // User is signed out
                Log.d(TAG, "onAuthStateChanged:signed_out");
            }
        };
    }

    public void buRegister(View view) {
        showProgressDialog();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        // Create a storage reference
        StorageReference storageRef = storage.getReferenceFromUrl("gs://tweeter-55347.appspot.com");
        DateFormat df = DateFormat.getDateTimeInstance();
        Date dateobj = new Date();
        final String ImagePath = df.format(dateobj) + ".jpg";
        final StorageReference avatarsStorage = storageRef.child(ImagePath);
        ivUserImage.setDrawingCacheEnabled(true);
        ivUserImage.buildDrawingCache();
        // Bitmap bitmap = imageView.getDrawingCache();
        BitmapDrawable drawable = (BitmapDrawable) ivUserImage.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = avatarsStorage.putBytes(data);
        uploadTask.addOnFailureListener(exception -> {
            // Handle unsuccessful uploads
        }).addOnSuccessListener(taskSnapshot -> avatarsStorage.getDownloadUrl().addOnSuccessListener(uri -> {
            // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
            String downloadUrl = uri.toString();
            String name = "";
            try {
                //for space with name
                name = java.net.URLEncoder.encode(etName.getText().toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {

            }
            if (etName.getText().toString().equals("") || etEmail.getText().toString().equals("") || etPassword.getText().toString().equals("")) {
                hideProgressDialog();
                Toast.makeText(getApplicationContext(), "One of the fields is empty!", Toast.LENGTH_SHORT).show();
            } else if (!EmailValidator.getInstance().isValid(etEmail.getText().toString())) {
                hideProgressDialog();
                Toast.makeText(getApplicationContext(), "Wrong email form!", Toast.LENGTH_SHORT).show();
            } else {
                //building retrofit object
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(APIUrl.BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                //Defining retrofit api service
                APIService service = retrofit.create(APIService.class);

                //defining the call
                Call<Result> call = service.createUser(name, etEmail.getText().toString(), etPassword.getText().toString(),downloadUrl);

                //calling the api
                call.enqueue(new Callback<Result>() {
                    @Override
                    public void onResponse(Call<Result> call, Response<Result> response) {
                        //hiding progress dialog
                        hideProgressDialog();
                        if(!response.body().getError()){
                            finish();
                            Log.i("i",response.body().getUser().getUsername());
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
        }));
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        signInAnonymously();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mAuthListener != null){
            mAuth.removeAuthStateListener(mAuthListener);
        }
        hideProgressDialog();
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
            Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());
            if(!task.isSuccessful()){
                Log.w(TAG, "signInAnonymously", task.getException());
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

    void CheckUserPermission(){
        if ( Build.VERSION.SDK_INT >= 23){
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED  ){
                requestPermissions(new String[]{
                                android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
                return ;
            }
        }
        LoadImage();
    }

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LoadImage();
                } else {
                    Toast.makeText( this,"your message" , Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    int RESULT_LOAD_IMAGE=346;

    void LoadImage(){
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            showProgressDialog();
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            assert selectedImage != null;
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            assert cursor != null;
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            // postImage.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            Bitmap b = BitmapFactory.decodeFile(picturePath, options);
            if (getExifRotation(picturePath) != 0) {
                Matrix matrix = new Matrix();

                matrix.postRotate(getExifRotation(picturePath));

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(b, b.getWidth(), b.getHeight(), true);

                b = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
            }
            float maxHeight = 512;
            float maxWidth = 512;
            float actualWidth = b.getWidth();
            float actualHeight = b.getHeight();
            float imgRatio = (actualWidth / actualHeight);
            float maxRatio = maxWidth / maxHeight;
            Log.i(TAG, "ACT_WIDTH:" + actualWidth + "ACT_HEIGHT:" + actualHeight + "IMG_RATIO" + imgRatio);

            //      width and height values are set maintaining the aspect ratio of the image
            if (actualHeight > maxHeight || actualWidth > maxWidth) {
                if (imgRatio < maxRatio) {
                    imgRatio = maxHeight / actualHeight;
                    actualWidth = (imgRatio * actualWidth);
                    actualHeight = maxHeight;
                } else if (imgRatio > maxRatio) {
                    imgRatio = maxWidth / actualWidth;
                    actualHeight = (imgRatio * actualHeight);
                    actualWidth = maxWidth;
                }
            }
            Log.i(TAG, "NEW_WIDTH:" + Math.round(actualWidth) + "NEW_HEIGHT:" + Math.round(actualHeight) + "IMG_RATIO" + imgRatio + "ROTATION:" + getExifRotation(picturePath));

            //iv_temp.setImageBitmap(Bitmap.createScaledBitmap(b,Math.round(actualWidth),Math.round(actualHeight),false));
            //iv_temp.setVisibility(ImageView.VISIBLE);
            ivUserImage.setImageBitmap(Bitmap.createScaledBitmap(b, Math.round(actualWidth), Math.round(actualHeight), false));
            hideProgressDialog();
        }
    }
}