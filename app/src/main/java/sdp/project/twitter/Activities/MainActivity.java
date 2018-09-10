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
import android.graphics.drawable.AnimationDrawable;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.iid.FirebaseInstanceId;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
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
import sdp.project.twitter.Utils.CustomAnimationDrawable;
import sdp.project.twitter.Utils.GlideApp;
import sdp.project.twitter.API.Result;
import sdp.project.twitter.Utils.SaveSettings;
import sdp.project.twitter.Model.SearchType;
import sdp.project.twitter.Model.TweetItem;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    int StartFrom = 0;
    int TweetsType = SearchType.MyFollowing;
    boolean LoadMore = false;
    //int totalItemCountVisible = 0; //totalItems visible
    LinearLayout ChannelInfo;
    TextView txtNameFollowers;
    ImageView iv_channel_icon;
    int SelectedUserID = 0;
    Button buFollow;

    ArrayList<TweetItem> tweetWall;
    TweetWall myTweetWall;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
            Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());
            if(!task.isSuccessful()){
                Log.w(TAG, "signInAnonymously", task.getException());
            }
        });

        Log.i( TAG, "LOGGING IN USER");

        tweetWall = new ArrayList<>();
        myTweetWall = new TweetWall(tweetWall,this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, OrientationHelper.VERTICAL, false);
        RecyclerView mRecyclerView = findViewById(R.id.RV_tweets);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(myTweetWall);
        LoadTweets(0, TweetsType);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(linearLayoutManager.findLastVisibleItemPosition() == tweetWall.size()-1 && LoadMore && !tweetWall.get(tweetWall.size()-1).tweet_date.equals("notweet"))
                {
                    LoadMore = false;
                    LoadTweets(tweetWall.size()-1,TweetsType);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mAuthListener != null){
            mAuth.removeAuthStateListener(mAuthListener);
        }
        hideProgressDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
                Query = query;
                TweetsType = SearchType.SearchIn;
                LoadTweets(0, TweetsType);
                searchView.setIconified(true);
                searchView.setIconified(true);
                searchView.clearFocus();
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
                if(TweetsType != SearchType.MyFollowing) {
                    searchView.setIconified(true);
                    TweetsType = SearchType.MyFollowing;
                    LoadTweets(0, TweetsType);
                }
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
        showProgressDialog();
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

    Boolean loadedImage = false;
    Bitmap loadImageBitmap = null;

    public class TweetWall extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        ArrayList<TweetItem> tweetWallAdapter;
        Context context;

        public class addTweetViewHolder extends RecyclerView.ViewHolder{

            TextView etCounter;
            EditText etPost;
            ImageView iv_post;
            ImageView iv_attach;
            ImageView iv_temp;

            addTweetViewHolder(View viewItem){
                super(viewItem);

                etCounter = viewItem.findViewById(R.id.etCounter);
                etPost = viewItem.findViewById(R.id.etPost);
                iv_post = viewItem.findViewById(R.id.iv_post);
                iv_attach = viewItem.findViewById(R.id.iv_attach);
                iv_temp = viewItem.findViewById(R.id.iv_temp);
                iv_temp.setVisibility(ImageView.GONE);
            }
        }

        class loadingTweetViewHolder extends RecyclerView.ViewHolder{

            loadingTweetViewHolder(View viewItem){
                super(viewItem);
            }
        }

        class noTweetViewHolder extends RecyclerView.ViewHolder{

            noTweetViewHolder(View viewItem){
                super(viewItem);
            }
        }

        class singleTweetHolder extends RecyclerView.ViewHolder{

            TextView txtUserName;
            TextView txt_tweet;
            TextView txt_tweet_date;
            ImageView tweet_picture;
            ImageView picture_path;
            ImageView iv_share;
            TextView favouriteCount;


            singleTweetHolder(View viewItem){
                super(viewItem);

                txtUserName = viewItem.findViewById(R.id.txtUserName);
                txt_tweet = viewItem.findViewById(R.id.txt_tweet);
                txt_tweet_date = viewItem.findViewById(R.id.txt_tweet_date);
                tweet_picture = viewItem.findViewById(R.id.tweet_picture);
                picture_path = viewItem.findViewById(R.id.picture_path);
                iv_share = viewItem.findViewById(R.id.iv_share);
                favouriteCount = viewItem.findViewById(R.id.txt_favouriteCount);
            }
        }

        TweetWall(ArrayList<TweetItem> tweetWallAdapter, Context context) {
            this.context = context;
            this.tweetWallAdapter = tweetWallAdapter;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view;
            switch (viewType) {
                case 0:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tweet_new, parent, false);
                    return new addTweetViewHolder(view);
                case 1:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tweet_load, parent, false);
                    return new loadingTweetViewHolder(view);
                case 2:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tweet_info, parent, false);
                    return new noTweetViewHolder(view);
                case 3:
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tweet_single, parent, false);
                    return new singleTweetHolder(view);
            }
            return null;
        }

        @Override
        public int getItemViewType(int position) {

            switch (tweetWallAdapter.get(position).tweet_date) {
                case "add" :  return 0;
                case "loading":  return 1;
                case "notweet" :  return  2;
                default : return 3;
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

            TweetItem t = tweetWallAdapter.get(position);
            if (t != null) {
                switch (t.tweet_date) {
                    case "add": {
                        final int[] counter = {0};
                        final String[] tweets = {""};
                        ((addTweetViewHolder) holder).etCounter.setText(getString(R.string.character_counter, counter[0]));
                        ((addTweetViewHolder) holder).etPost.setText(tweets[0]);
                        ((addTweetViewHolder) holder).etPost.setSelection(((addTweetViewHolder) holder).etPost.getText().length());


                        if (loadedImage) {
                            ((addTweetViewHolder) holder).iv_temp.setImageBitmap(loadImageBitmap);
                        }else{
                            ((addTweetViewHolder) holder).iv_temp.setImageBitmap(loadImageBitmap);
                            ((addTweetViewHolder) holder).iv_temp.setVisibility(ImageView.INVISIBLE);
                        }
                        ((addTweetViewHolder) holder).iv_attach.setOnClickListener(view -> CheckUserPermission());
                        ((addTweetViewHolder) holder).iv_post.setOnClickListener(view -> {
                            if (((addTweetViewHolder) holder).etPost.length() <= 0)
                                Toast.makeText(context, "Tweet is empty.", Toast.LENGTH_SHORT).show();
                                //TODO: MERGE INTO ONE RETROFIT CALL
                            else if (((addTweetViewHolder) holder).etPost.length() <= 150) {
                                if (((addTweetViewHolder) holder).iv_temp.getVisibility() == View.VISIBLE) {
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
                                    ((addTweetViewHolder) holder).iv_temp.setDrawingCacheEnabled(true);
                                    ((addTweetViewHolder) holder).iv_temp.buildDrawingCache();
                                    BitmapDrawable drawable = (BitmapDrawable) ((addTweetViewHolder) holder).iv_temp.getDrawable();
                                    Bitmap bitmap = drawable.getBitmap();
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                    byte[] data = baos.toByteArray();

                                    final UploadTask uploadTask = picRef.putBytes(data);
                                    uploadTask.addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Image could not be uploaded: " + e.getMessage(), Toast.LENGTH_LONG).show())
                                            .addOnSuccessListener(taskSnapshot -> picRef.getDownloadUrl().
                                                    addOnSuccessListener(uri -> {
                                                        String downloadUrl = uri.toString();
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
                                                        Call<Result> call = service.tweetAdd(SaveSettings.getInstance(getApplicationContext()).getUser().getUserID(), ((addTweetViewHolder) holder).etPost.getText().toString(), downloadUrl);

                                                        //calling the api
                                                        call.enqueue(new Callback<Result>() {
                                                            @Override
                                                            public void onResponse(Call<Result> call, Response<Result> response) {
                                                                //hiding progress dialog
                                                                hideProgressDialog();
                                                                //displaying the message from the response as toast
                                                                Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
                                                                ((addTweetViewHolder) holder).etPost.setText("");
                                                                loadedImage = false;
                                                                loadImageBitmap = null;
                                                                TweetsType = SearchType.MyFollowing;
                                                                LoadTweets(0, TweetsType);
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
                                    Call<Result> call = service.tweetAdd(SaveSettings.getInstance(getApplicationContext()).getUser().getUserID(), ((addTweetViewHolder) holder).etPost.getText().toString(), "none");

                                    //calling the api
                                    call.enqueue(new Callback<Result>() {
                                        @Override
                                        public void onResponse(Call<Result> call, Response<Result> response) {
                                            //hiding progress dialog
                                            hideProgressDialog();
                                            //displaying the message from the response as toast
                                            Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
                                            ((addTweetViewHolder) holder).etPost.setText("");
                                            loadedImage = false;
                                            loadImageBitmap = null;
                                            TweetsType = SearchType.MyFollowing;
                                            LoadTweets(0, TweetsType);
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
                                ((addTweetViewHolder) holder).etCounter.setText(getString(R.string.character_counter, counter[0]));
                                if (s.length() > 150) {
                                    ((addTweetViewHolder) holder).etCounter.setTextColor(Color.RED);
                                } else {
                                    ((addTweetViewHolder) holder).etCounter.setTextColor(Color.WHITE);
                                }
                            }

                            public void afterTextChanged(Editable s) {
                            }
                        };
                        ((addTweetViewHolder) holder).etPost.addTextChangedListener(mTextEditorWatcher);
                        break;
                    }

                    case "loading": {
                        Log.i(TAG,"TWEETLOADING");
                        break;
                    }
                    case "notweet": {
                        break;
                    }
                    default: {

                        ((singleTweetHolder) holder).tweet_picture.setVisibility(View.VISIBLE);
                        ((singleTweetHolder) holder).txtUserName.setText(t.username);
                        ((singleTweetHolder) holder).txtUserName.setOnClickListener((View view) -> {
                            SelectedUserID = t.user_id;
                            if (SaveSettings.getInstance(getApplicationContext()).getUser().getUserID() != SelectedUserID) {
                                TweetsType = SearchType.OnePerson;
                                LoadTweets(0, TweetsType);
                                txtNameFollowers.setText(t.username);
                                //Picasso.get().load(t.picture_path).into(iv_channel_icon);
                                //Glide.with(getApplicationContext()).load(t.picture_path).into(iv_channel_icon);
                                GlideApp.with(getApplicationContext()).load(t.picture_path).optionalCenterCrop().into(iv_channel_icon);

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
                                            buFollow.setSelected(false);
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

                        ((singleTweetHolder) holder).txt_tweet.setText(t.tweet_text);

                        ((singleTweetHolder) holder).txt_tweet_date.setText(t.tweet_date);

                        //Picasso.get().load(t.tweet_picture).into(((singleTweetHolder) holder).tweet_picture);
                        //Glide.with(getApplicationContext()).load(t.tweet_picture).into(((singleTweetHolder) holder).tweet_picture);

                        if(t.tweet_picture.equals("none") || t.tweet_picture.equals("null") )
                            ((singleTweetHolder) holder).tweet_picture.setVisibility(View.GONE);
                        else
                            GlideApp.with(getApplicationContext()).load(t.tweet_picture).placeholder(R.drawable.round_background_white).optionalCenterCrop().into(((singleTweetHolder) holder).tweet_picture);
                            //Picasso.get().load(t.tweet_picture).into(((singleTweetHolder) holder).tweet_picture);
                            //Glide.with(getApplicationContext()).load(t.picture_path).into(((singleTweetHolder) holder).picture_path);
                            //GlideApp.with(getApplicationContext()).load(t.picture_path).optionalCenterCrop().into(((singleTweetHolder) holder).picture_path);


                        ((singleTweetHolder) holder).iv_share.setOnClickListener(v -> {
                            if (!t.isFavourite) {
                                CustomAnimationDrawable cad = new CustomAnimationDrawable((AnimationDrawable) getResources().getDrawable(R.drawable.favourite_animation)) {
                                    @Override
                                    protected void onAnimationStart() {
                                        ((singleTweetHolder) holder).iv_share.setEnabled(false);
                                    }

                                    @Override
                                    protected void onAnimationFinish() {
                                        ((singleTweetHolder) holder).iv_share.setEnabled(true);
                                    }
                                };
                                ((singleTweetHolder) holder).iv_share.setImageDrawable(cad);
                                cad.setOneShot(true);
                                cad.start();
                                t.favouriteCount++;
                            } else {
                                CustomAnimationDrawable cad = new CustomAnimationDrawable((AnimationDrawable) getResources().getDrawable(R.drawable.unfavourite_animation)) {
                                    @Override
                                    protected void onAnimationStart() {
                                        ((singleTweetHolder) holder).iv_share.setEnabled(false);
                                    }

                                    @Override
                                    protected void onAnimationFinish() {
                                        ((singleTweetHolder) holder).iv_share.setEnabled(true);
                                    }
                                };
                                ((singleTweetHolder) holder).iv_share.setImageDrawable(cad);
                                cad.setOneShot(true);
                                cad.start();
                                t.favouriteCount--;
                            }

                            t.isFavourite = !t.isFavourite;
                            ((singleTweetHolder) holder).favouriteCount.setText(String.valueOf(t.favouriteCount));

                            //Log.i("URL",""+url);
                            //building retrofit object
                            Retrofit retrofit = new Retrofit.Builder()
                                    .baseUrl(APIUrl.BASE_URL)
                                    .addConverterFactory(GsonConverterFactory.create())
                                    .build();

                            //Defining retrofit api service
                            APIService service = retrofit.create(APIService.class);

                            //defining the call
                            Call<Result> call = service.favourite(SaveSettings.getInstance(getApplicationContext()).getUser().getUserID(), t.tweet_id);

                            //calling the api
                            call.enqueue(new Callback<Result>() {


                                @Override
                                public void onResponse(Call<Result> call, Response<Result> response) {
                                    Log.i(TAG,"restcall");
                                    //hiding progress dialog
                                    hideProgressDialog();
                                    /*if (response.body().getError()) {
                                        GlideApp.with(getApplicationContext()).load(R.drawable.favourite1).into(((singleTweetHolder) holder).iv_share);
                                        t.isFavourite = false;
                                        t.favouriteCount--;
                                    }
                                    else{
                                        //GlideApp.with(getApplicationContext()).load(R.drawable.favourite22).into(((singleTweetHolder) holder).iv_share);
                                        ((singleTweetHolder) holder).iv_share.setImageResource(R.drawable.favourite_animation);
                                        AnimationDrawable favouriteAnimation = (AnimationDrawable)((singleTweetHolder) holder).iv_share.getDrawable();
                                        favouriteAnimation.start();
                                        t.isFavourite = true;
                                        t.favouriteCount++;
                                    }
                                    ((singleTweetHolder) holder).favouriteCount.setText(String.valueOf(t.favouriteCount));*/
                                    //displaying the message from the response as toast
                                    Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(Call<Result> call, Throwable t13) {
                                    hideProgressDialog();
                                    Toast.makeText(getApplicationContext(), t13.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        });

                        if (!t.isFavourite)
                            GlideApp.with(getApplicationContext()).load(R.drawable.favourite1).into(((singleTweetHolder) holder).iv_share);
                        else
                            GlideApp.with(getApplicationContext()).load(R.drawable.favourite22).into(((singleTweetHolder) holder).iv_share);

                        ((singleTweetHolder) holder).favouriteCount.setText(String.valueOf(t.favouriteCount));

                        break;
                    }
                }
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            if(tweetWallAdapter == null)
                return  0;
            return tweetWallAdapter.size();
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
        Log.i(TAG, "Requesting permission");
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
                    Log.i(TAG, "Permission granted");
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
            Log.i(TAG, "ACT_WIDTH:"+actualWidth+"ACT_HEIGHT:"+actualHeight+"IMG_RATIO"+imgRatio);

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
            Log.i(TAG, "NEW_WIDTH:"+Math.round(actualWidth)+"NEW_HEIGHT:"+Math.round(actualHeight)+"IMG_RATIO"+imgRatio+"ROTATION:"+getExifRotation(picturePath));

            //iv_temp.setImageBitmap(Bitmap.createScaledBitmap(b,Math.round(actualWidth),Math.round(actualHeight),false));
            //iv_temp.setVisibility(ImageView.VISIBLE);
            loadedImage = true;
            loadImageBitmap = Bitmap.createScaledBitmap(b,Math.round(actualWidth),Math.round(actualHeight),false);
            myTweetWall.notifyItemChanged(0);
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
        //setTitle(SaveSettings.getInstance(getApplicationContext()).getUser().getUsername());
        //getActionBar().setTitle(SaveSettings.getInstance(getApplicationContext()).getUser().getUsername());
        Log.i( TAG, "LOADING TWEETS");
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

        //building retrofit object
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(APIUrl.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        //Defining retrofit api service
        APIService service = retrofit.create(APIService.class);

        //defining the call
        Log.i(TAG, "USER ID: " + user_id + " STARTFROM: " + StartFrom + " QUERY: " + Query + " TWEETTYPE: " + TweetType + " CHECK USER ID: " + SelectedUserID);
        Call<Result> call = service.tweetList(user_id, StartFrom, Query, TweetType, SelectedUserID);

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
                        tweetWall.add(new TweetItem(0, null, null,
                                "notweet", 0, null, null, 0, false));
                    }
                    // listnewsData.remove(listnewsData.size()-1);
                    //tweetWall.add(new TweetItem(0, null, null,
                            //"notweet", 0, null, null, 0, false));
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
                LoadMore = true;
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

