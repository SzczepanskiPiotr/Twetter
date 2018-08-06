package sdp.project.twitter.Activities;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import sdp.project.tweeter.R;
import sdp.project.twitter.API.APIService;
import sdp.project.twitter.API.APIUrl;
import sdp.project.twitter.Result;
import sdp.project.twitter.SaveSettings;
import sdp.project.twitter.SearchType;
import sdp.project.twitter.TweetItem;

public class MainActivity extends AppCompatActivity {

    ArrayList<TweetItem> tweetWall = new ArrayList<>();
    int StartFrom = 0;
    int TweetsType = SearchType.MyFollowing;
    //int totalItemCountVisible = 0; //totalItems visible
    LinearLayout ChannelInfo;
    TextView txtNameFollowers;
    ImageView iv_channel_icon;
    int SelectedUserID = 0;
    Button buFollow;
    TweetWall myTweetWall;

    //firebase
    private static final String TAG = "AnonymousAuth";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        //Info of other users, hidden on main screen
        ChannelInfo = findViewById(R.id.ChannelInfo);
        ChannelInfo.setVisibility(View.GONE);
        txtNameFollowers = findViewById(R.id.txtNameFollowers);
        iv_channel_icon = findViewById(R.id.iv_channel_icon);
        //button
        buFollow = findViewById(R.id.buFollow);
        //load user
        if (!SaveSettings.getInstance(getApplicationContext()).isLoggedIn()) {
            finish();
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            return;
        }
        Log.i("MAINACTIVITY: ", "LOGGING IN USER");
        //tweetWall
        myTweetWall = new TweetWall(this, tweetWall);
        ListView lsNews = findViewById(R.id.LVNews);
        lsNews.setItemsCanFocus(true);
        lsNews.setAdapter(myTweetWall);//initial with data
        LoadTweets(0, SearchType.MyFollowing);
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
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
        hideProgressDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
            Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());
            if (!task.isSuccessful()) {
                Log.w(TAG, "signInAnonymously", task.getException());
            }
        });
    }

    SearchView searchView;
    Menu myMenu;
    String Query = "null";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // add menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        myMenu = menu;
        // searchView code
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (android.widget.SearchView) menu.findItem(R.id.searchbar).getActionView();
        searchView.setSearchableInfo(searchManager != null ? searchManager.getSearchableInfo(getComponentName()) : null);
        //final Context co=this;
        searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Toast.makeText(co, query, Toast.LENGTH_LONG).show();
                Query = null;
                LoadTweets(0, SearchType.SearchIn);// search
                searchView.setIconified(true);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        //   searchView.setOnCloseListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle item selection
        switch (item.getItemId()) {
            case R.id.home: {
                searchView.setIconified(true);
                LoadTweets(0, SearchType.MyFollowing);
                return true;
            }
            case R.id.logout: {
                SaveSettings.getInstance(this.getApplicationContext()).logout();
                finish();
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void buFollowers(View view) {
        int Operation; // 1- subsribe 2- unsubscribe
        String Follow = buFollow.getText().toString();
        if (Follow.equalsIgnoreCase("Follow")) {
            Operation = 1;
            buFollow.setSelected(true);
            buFollow.setText(R.string.buFollow_unFollow);
            //buFollow.set
        } else {
            buFollow.setSelected(false);
            Operation = 2;
            buFollow.setText(R.string.buFollow_follow);
        }
        //building retrofit object
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(APIUrl.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        //Defining retrofit api service
        APIService service = retrofit.create(APIService.class);

        //defining the call
        Call<Result> call = service.followUser(SaveSettings.getInstance(this.getApplicationContext()).getUser().getUserID(), SelectedUserID, Operation);

        //calling the api
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(Call<Result> call, Response<Result> response) {
                //hiding progress dialog
                hideProgressDialog();
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

    ImageView iv_temp;
    //TextView etCounter;
    String downloadUrl = "none";
    Boolean loadedImage = false;
    Bitmap loadImageBitmap = null;

    private class TweetWall extends BaseAdapter {

        ArrayList<TweetItem> tweetWallAdapter;
        Context context;

        TweetWall(Context context, ArrayList<TweetItem> tweetWallAdapter) {
            this.tweetWallAdapter = tweetWallAdapter;
            this.context = context;
        }

        @Override
        public int getCount() {
            return tweetWallAdapter.size();
        }

        @Override
        public String getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final TweetItem t = tweetWallAdapter.get(position);

            switch (t.tweet_date) {
                case "add": {
                    final int[] counter = {0};
                    final String[] tweets = {""};
                    LayoutInflater mInflater = getLayoutInflater();
                    View myView = mInflater.inflate(R.layout.tweet_new, parent, false);
                    TextView etCounter = myView.findViewById(R.id.etCounter);
                    etCounter.setText(getString(R.string.character_counter,counter[0]));
                    EditText etPost = myView.findViewById(R.id.etPost);
                    etPost.setText(tweets[0]);
                    etPost.setSelection(etPost.getText().length());
                    ImageView iv_post = myView.findViewById(R.id.iv_post);
                    ImageView iv_attach = myView.findViewById(R.id.iv_attach);
                    iv_temp = myView.findViewById(R.id.iv_temp);
                    iv_temp.setVisibility(ImageView.GONE);
                    if (loadedImage) {
                        iv_temp.setImageBitmap(loadImageBitmap);
                        iv_temp.setVisibility(ImageView.VISIBLE);
                    }
                    iv_attach.setOnClickListener(view -> CheckUserPermission());
                    iv_post.setOnClickListener(view -> {
                        if (etPost.length() <= 0)
                            Toast.makeText(context, "Tweet is empty.", Toast.LENGTH_SHORT).show();
                            //TODO: MERGE INTO ONE RETROFIT CALL
                        else if (etPost.length() <= 150) {
                            if (iv_temp.getVisibility() == View.VISIBLE) {
                                showProgressDialog();
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                // Create a storage reference from our app
                                StorageReference storageRef = storage.getReferenceFromUrl("gs://tweeter-55347.appspot.com/");
                                DateFormat df = DateFormat.getDateTimeInstance();
                                Date dateobj = new Date();
                                // System.out.println(df.format(dateobj));
                                // Create a reference to "mountains.jpg"
                                String myDownloadUrl = SaveSettings.getInstance(getApplicationContext()).getUser().getUserID() + "_" + df.format(dateobj) + ".jpg";
                                final StorageReference picRef = storageRef.child(myDownloadUrl);
                                iv_temp.setDrawingCacheEnabled(true);
                                iv_temp.buildDrawingCache();
                                BitmapDrawable drawable = (BitmapDrawable) iv_temp.getDrawable();
                                Bitmap bitmap = drawable.getBitmap();
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                byte[] data = baos.toByteArray();

                                final UploadTask uploadTask = picRef.putBytes(data);
                                uploadTask.addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Image could not be uploaded: " + e.getMessage(), Toast.LENGTH_LONG).show())
                                        .addOnSuccessListener(taskSnapshot -> picRef.getDownloadUrl().
                                                addOnSuccessListener(uri -> {
                                                    downloadUrl = uri.toString();
                                                    hideProgressDialog();
                                                    Toast.makeText(context, "Tweet added", Toast.LENGTH_SHORT).show();
                                                    //building retrofit object
                                                    Retrofit retrofit = new Retrofit.Builder()
                                                            .baseUrl(APIUrl.BASE_URL)
                                                            .addConverterFactory(GsonConverterFactory.create())
                                                            .build();

                                                    //Defining retrofit api service
                                                    APIService service = retrofit.create(APIService.class);

                                                    //defining the call
                                                    Call<Result> call = service.tweetAdd(SaveSettings.getInstance(getApplicationContext()).getUser().getUserID(), etPost.getText().toString(), downloadUrl);

                                                    //calling the api
                                                    call.enqueue(new Callback<Result>() {
                                                        @Override
                                                        public void onResponse(Call<Result> call, Response<Result> response) {
                                                            //hiding progress dialog
                                                            hideProgressDialog();
                                                            //displaying the message from the response as toast
                                                            Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
                                                            etPost.setText("");
                                                            loadedImage = false;
                                                            loadImageBitmap = null;
                                                            downloadUrl = "none";
                                                            LoadTweets(0, SearchType.MyFollowing);
                                                        }

                                                        @Override
                                                        public void onFailure(Call<Result> call, Throwable t1) {
                                                            hideProgressDialog();
                                                            Toast.makeText(getApplicationContext(), t1.getMessage(), Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                                }));
                            } else {
                                //building retrofit object
                                Retrofit retrofit = new Retrofit.Builder()
                                        .baseUrl(APIUrl.BASE_URL)
                                        .addConverterFactory(GsonConverterFactory.create())
                                        .build();

                                //Defining retrofit api service
                                APIService service = retrofit.create(APIService.class);

                                //defining the call
                                Call<Result> call = service.tweetAdd(SaveSettings.getInstance(getApplicationContext()).getUser().getUserID(), etPost.getText().toString(), downloadUrl);

                                //calling the api
                                call.enqueue(new Callback<Result>() {
                                    @Override
                                    public void onResponse(Call<Result> call, Response<Result> response) {
                                        //hiding progress dialog
                                        hideProgressDialog();
                                        //displaying the message from the response as toast
                                        Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
                                        etPost.setText("");
                                        loadedImage = false;
                                        downloadUrl = "none";
                                        loadImageBitmap = null;
                                        LoadTweets(0, SearchType.MyFollowing);
                                    }

                                    @Override
                                    public void onFailure(Call<Result> call, Throwable t1) {
                                        hideProgressDialog();
                                        Toast.makeText(getApplicationContext(), t1.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        } else
                            Toast.makeText(context, "Tweet is too long", Toast.LENGTH_SHORT).show();
                    });

                    final TextWatcher mTextEditorWatcher = new TextWatcher() {
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                            counter[0] = s.length();
                            tweets[0] = s.toString();
                            etCounter.setText(getString(R.string.character_counter,counter[0]));
                            if (s.length() > 150) {
                                etCounter.setTextColor(Color.RED);
                            } else {
                                etCounter.setTextColor(Color.WHITE);
                            }
                        }

                        public void afterTextChanged(Editable s) {
                        }
                    };
                    etPost.addTextChangedListener(mTextEditorWatcher);
                    return myView;

                }
                case "loading": {
                    LayoutInflater mInflater = getLayoutInflater();
                    return mInflater.inflate(R.layout.tweet_load, parent, false);
                }
                case "notweet": {
                    LayoutInflater mInflater = getLayoutInflater();
                    return mInflater.inflate(R.layout.tweet_info, parent, false);
                }
                default: {
                    LayoutInflater mInflater = getLayoutInflater();
                    View myView = mInflater.inflate(R.layout.tweet_single, parent, false);

                    TextView txtUserName = myView.findViewById(R.id.txtUserName);
                    txtUserName.setText(t.username);
                    txtUserName.setOnClickListener(view -> {
                        SelectedUserID = t.user_id;
                        if (SaveSettings.getInstance(getApplicationContext()).getUser().getUserID() != SelectedUserID) {
                            LoadTweets(0, SearchType.OnePerson);
                            txtNameFollowers.setText(t.username);
                            //Picasso.get().load(t.picture_path).into(iv_channel_icon);
                            Glide.with(getApplicationContext()).load(t.picture_path).into(iv_channel_icon);
                            //TODO: I THINK FOLLOWING STATUS IS ALREADY IN 'tweetlist' REST CALL
                            //building retrofit object
                            Retrofit retrofit = new Retrofit.Builder()
                                    .baseUrl(APIUrl.BASE_URL)
                                    .addConverterFactory(GsonConverterFactory.create())
                                    .build();

                            //Defining retrofit api service
                            APIService service = retrofit.create(APIService.class);

                            //defining the call
                            Call<Result> call = service.checkFollowing(SaveSettings.getInstance(getApplicationContext()).getUser().getUserID(), SelectedUserID);

                            //calling the api
                            call.enqueue(new Callback<Result>() {
                                @Override
                                public void onResponse(Call<Result> call, Response<Result> response) {
                                    //hiding progress dialog
                                    hideProgressDialog();
                                    if (response.body().getError()) {
                                        buFollow.setText(R.string.buFollow_follow);
                                    } else {
                                        buFollow.setText(R.string.buFollow_unFollow);
                                        buFollow.setSelected(true);
                                    }
                                    //displaying the message from the response as toast
                                    Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(Call<Result> call, Throwable t12) {
                                    hideProgressDialog();
                                    Toast.makeText(getApplicationContext(), t12.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    });
                    TextView txt_tweet = myView.findViewById(R.id.txt_tweet);
                    txt_tweet.setText(t.tweet_text);

                    TextView txt_tweet_date = myView.findViewById(R.id.txt_tweet_date);
                    txt_tweet_date.setText(t.tweet_date);

                    ImageView tweet_picture = myView.findViewById(R.id.tweet_picture);
                   // Picasso.get().load(t.tweet_picture).into(tweet_picture);
                    Glide.with(getApplicationContext()).load(t.tweet_picture).into(tweet_picture);
                    ImageView picture_path = myView.findViewById(R.id.picture_path);
                    //Picasso.get().load(t.picture_path).into(picture_path);
                    Glide.with(getApplicationContext()).load(t.picture_path).into(picture_path);

                    final ImageView iv_share = myView.findViewById(R.id.iv_share);
                    final TextView favouriteCount = myView.findViewById(R.id.txt_favouriteCount);

                    iv_share.setOnClickListener(v -> {
                        int Operation;
                        if (iv_share.getBackground().getConstantState() == getResources().getDrawable(R.drawable.favourite).getConstantState()) {
                            Operation = 1;
                            iv_share.setBackgroundResource(R.drawable.favourited);
                        } else {
                            iv_share.setBackgroundResource(R.drawable.favourite);
                            Operation = 2;
                        }
                        //Log.i("URL",""+url);
                        //building retrofit object
                        Retrofit retrofit = new Retrofit.Builder()
                                .baseUrl(APIUrl.BASE_URL)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();

                        //Defining retrofit api service
                        APIService service = retrofit.create(APIService.class);

                        //defining the call
                        Call<Result> call = service.favourite(SaveSettings.getInstance(getApplicationContext()).getUser().getUserID(), t.tweet_id, Operation);

                        //calling the api
                        call.enqueue(new Callback<Result>() {
                            @Override
                            public void onResponse(Call<Result> call, Response<Result> response) {
                                //hiding progress dialog
                                hideProgressDialog();
                                if (!response.body().getError()) {
                                    t.isFavourite = !t.isFavourite;
                                    if (t.isFavourite)
                                        t.favouriteCount++;
                                    else
                                        t.favouriteCount--;
                                }
                                //displaying the message from the response as toast
                                Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
                                favouriteCount.setText(String.valueOf(t.favouriteCount));
                            }

                            @Override
                            public void onFailure(Call<Result> call, Throwable t13) {
                                hideProgressDialog();
                                Toast.makeText(getApplicationContext(), t13.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    });

                    if (!t.isFavourite)
                        iv_share.setBackgroundResource(R.drawable.favourite);
                    else
                        iv_share.setBackgroundResource(R.drawable.favourited);

                    favouriteCount.setText(String.valueOf(t.favouriteCount));

                    return myView;
                }
            }
        }
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


    void CheckUserPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                                android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
                return;
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
                    Toast.makeText(this, "your message", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    int RESULT_LOAD_IMAGE = 346;

    void LoadImage() {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            // postImage.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            Bitmap b = BitmapFactory.decodeFile(picturePath,options);
            if(getExifRotation(picturePath)!=0){
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
            Log.i("IMG", "ACT_WIDTH:"+actualWidth+"ACT_HEIGHT:"+actualHeight+"IMG_RATIO"+imgRatio);

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
            Log.i("IMG", "NEW_WIDTH:"+Math.round(actualWidth)+"NEW_HEIGHT:"+Math.round(actualHeight)+"IMG_RATIO"+imgRatio+"ROTATION:"+getExifRotation(picturePath));

            iv_temp.setImageBitmap(Bitmap.createScaledBitmap(b,Math.round(actualWidth),Math.round(actualHeight),false));
            iv_temp.setVisibility(ImageView.VISIBLE);
            loadedImage = true;
            loadImageBitmap = Bitmap.createScaledBitmap(b,Math.round(actualWidth),Math.round(actualHeight),false);
        }
    }

    public static int getExifRotation(String filePath) {
        if (filePath == null) return 0;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            // We only recognize a subset of orientation tag values
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return ExifInterface.ORIENTATION_UNDEFINED;
            }
        } catch (IOException e) {
            //  Log.e("Error getting Exif data", e);
            return 0;
        }
    }

    void LoadTweets(int StartFrom, int TweetType) {
        Log.i("MAINACTIVITY: ", "LOADING TWEETS");
        int user_id = SaveSettings.getInstance(getApplicationContext()).getUser().getUserID();
        this.StartFrom = StartFrom;
        this.TweetsType = TweetType;
        //display loading
        if (StartFrom == 0) // add loading at beggining
            tweetWall.add(0, new TweetItem(0, null, null,
                    "loading", 0, null, null, 0, false));
        else // add loading at end
            tweetWall.add(new TweetItem(0, null, null,
                    "loading", 0, null, null, 0, false));

        myTweetWall.notifyDataSetChanged();

        if (TweetType == SearchType.OnePerson)
            user_id = SelectedUserID;

        //building retrofit object
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(APIUrl.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        //Defining retrofit api service
        APIService service = retrofit.create(APIService.class);

        //defining the call
        Log.i("TEST: ", "USER ID: " + user_id + " STARTFROM: " + StartFrom + " QUERY: " + Query + " TWEETTYPE: " + TweetType);
        Call<Result> call = service.tweetList(user_id, StartFrom, Query, TweetType);

        //calling the api
        call.enqueue(new Callback<Result>() {
            @Override
            public void onResponse(Call<Result> call, Response<Result> response) {
                //hiding progress dialog
                hideProgressDialog();
                if (response.body().getError()) {
                    //remove we are loading now
                    if (StartFrom == 0) {
                        tweetWall.clear();
                        tweetWall.add(new TweetItem(0, null, null,
                                "add", 0, null, null, 0, false));
                    } else {
                        //remove we are loading now
                        tweetWall.remove(tweetWall.size() - 1);
                    }
                    // listnewsData.remove(listnewsData.size()-1);
                    tweetWall.add(new TweetItem(0, null, null,
                            "notweet", 0, null, null, 0, false));
                } else {
                    if (StartFrom == 0) {
                        tweetWall.clear();
                        tweetWall.add(new TweetItem(0, null, null,
                                "add", 0, null, null, 0, false));

                    } else {
                        //remove we are loading now
                        tweetWall.remove(tweetWall.size() - 1);
                    }

                    // try to add the resourcess
                    //add data and view it
                    tweetWall.addAll(response.body().getTweets());
                }
                myTweetWall.notifyDataSetChanged();
                //displaying the message from the response as toast
                Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<Result> call, Throwable t) {
                hideProgressDialog();
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        if (TweetType == SearchType.OnePerson)
            ChannelInfo.setVisibility(View.VISIBLE);
        else
            ChannelInfo.setVisibility(View.GONE);
    }

}