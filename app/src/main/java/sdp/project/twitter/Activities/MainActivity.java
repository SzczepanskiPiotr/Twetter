package sdp.project.twitter.Activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
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
import sdp.project.tweeter.R;
import sdp.project.twitter.API.APIUrl;
import sdp.project.twitter.API.Result;
import sdp.project.twitter.Model.Location;
import sdp.project.twitter.Model.SearchType;
import sdp.project.twitter.Model.TweetItem;
import sdp.project.twitter.Utils.CustomAnimationDrawable;
import sdp.project.twitter.Utils.GlideApp;
import sdp.project.twitter.Utils.MyGeocoderUtil;
import sdp.project.twitter.Utils.SaveSettings;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    int StartFrom = 0;
    int TweetsType = SearchType.MyFollowing;
    boolean LoadMore = false;
    LinearLayout ChannelInfo;
    TextView txtNameFollowers;
    ImageView iv_channel_icon;
    int SelectedUserID = 0;
    Button buFollow;

    ArrayList<TweetItem> tweetWall;
    TweetWall myTweetWall;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TweetsType == SearchType.MyFollowing)
                LoadTweets(0, SearchType.MyFollowing);
        }
    };

    LinearLayoutManager linearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "protected void onCreate(Bundle savedInstanceState)");

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
            if (!task.isSuccessful()) {
                Log.w(TAG, "signInAnonymously", task.getException());
            }
        });
        Log.i(TAG, "LOGGING IN USER");

        tweetWall = new ArrayList<>();
        myTweetWall = new TweetWall(tweetWall, this);
        linearLayoutManager = new LinearLayoutManager(this, OrientationHelper.VERTICAL, false);
        RecyclerView mRecyclerView = findViewById(R.id.RV_tweets);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(myTweetWall);
        LoadTweets(0, TweetsType);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (linearLayoutManager.findLastVisibleItemPosition() == tweetWall.size() - 1 && LoadMore && !tweetWall.get(tweetWall.size() - 1).getTweet_date().equals("notweet")) {
                    LoadMore = false;
                    LoadTweets(tweetWall.size() - 1, TweetsType);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        LocalBroadcastManager.getInstance(this).registerReceiver((mMessageReceiver),
                new IntentFilter("MESSAGE_RECEIVED")
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
        hideProgressDialog();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
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
        searchView = (android.support.v7.widget.SearchView) menu.findItem(R.id.searchbar).getActionView();
        searchView.setSearchableInfo(searchManager != null ? searchManager.getSearchableInfo(getComponentName()) : null);
        //final Context co=this;
        searchView.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Toast.makeText(co, query, Toast.LENGTH_LONG).show();
                Query = query;
                TweetsType = SearchType.SearchIn;
                LoadTweets(0, TweetsType);
                //searchView.setIconified(true);
                // searchView.setIconified(true);
                searchView.clearFocus();
                myMenu.getItem(0).collapseActionView();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setOnQueryTextFocusChangeListener(new SearchView.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                myMenu.getItem(0).collapseActionView();
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
                //if (TweetsType != SearchType.MyFollowing) {
                    searchView.setIconified(true);
                    TweetsType = SearchType.MyFollowing;
                    LoadTweets(0, TweetsType);
               // }
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
            FirebaseMessaging.getInstance().subscribeToTopic(String.valueOf(SelectedUserID));
        } else {
            buFollow.setSelected(false);
            Operation = 2;
            buFollow.setText(R.string.buFollow_follow);
            FirebaseMessaging.getInstance().unsubscribeFromTopic(String.valueOf(SelectedUserID));
        }
        showProgressDialog();
        //defining the call
        Call<Result> call = APIUrl.getApi().followUser(SaveSettings.getInstance(this.getApplicationContext()).getUser().getUserID(), SelectedUserID, Operation);

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

        private class addTweetViewHolder extends RecyclerView.ViewHolder {

            TextView etCounter;
            EditText etPost;
            ImageView iv_post;
            ImageView iv_attach;
            ImageView iv_temp;

            addTweetViewHolder(View viewItem) {
                super(viewItem);

                etCounter = viewItem.findViewById(R.id.etCounter);
                etPost = viewItem.findViewById(R.id.etPost);
                iv_post = viewItem.findViewById(R.id.iv_post);
                iv_attach = viewItem.findViewById(R.id.iv_attach);
                iv_temp = viewItem.findViewById(R.id.iv_temp);
                iv_temp.setVisibility(ImageView.GONE);
            }
        }

        class loadingTweetViewHolder extends RecyclerView.ViewHolder {

            loadingTweetViewHolder(View viewItem) {
                super(viewItem);
            }
        }

        class noTweetViewHolder extends RecyclerView.ViewHolder {

            noTweetViewHolder(View viewItem) {
                super(viewItem);
            }
        }

        class singleTweetHolder extends RecyclerView.ViewHolder {

            TextView txtUserName;
            TextView txt_tweet;
            TextView txt_tweet_date;
            ImageView tweet_picture;
            ImageView picture_path;
            ImageView iv_share;
            TextView favouriteCount;
            TextView txt_location;
            ImageView iv_location;


            singleTweetHolder(View viewItem) {
                super(viewItem);

                txtUserName = viewItem.findViewById(R.id.txtUserName);
                txt_tweet = viewItem.findViewById(R.id.txt_tweet);
                txt_tweet_date = viewItem.findViewById(R.id.txt_tweet_date);
                tweet_picture = viewItem.findViewById(R.id.tweet_picture);
                picture_path = viewItem.findViewById(R.id.picture_path);
                iv_share = viewItem.findViewById(R.id.iv_share);
                favouriteCount = viewItem.findViewById(R.id.txt_favouriteCount);
                txt_location = viewItem.findViewById(R.id.txt_location);
                iv_location = viewItem.findViewById(R.id.iv_location);
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

            switch (tweetWallAdapter.get(position).getTweet_date()) {
                case "add":
                    return 0;
                case "loading":
                    return 1;
                case "notweet":
                    return 2;
                default:
                    return 3;
            }
        }

        void sendTweetToDb(String tweetText, String imageUrl, Location location) {
            //defining the call
            Call<Result> call = APIUrl.getApi().tweetAdd(SaveSettings.getInstance(getApplicationContext()).getUser().getUserID(), tweetText, imageUrl, location.getLatitude(), location.getLongitude(), location.getCountry(), location.getCity());
            //calling the api
            call.enqueue(new Callback<Result>() {
                @Override
                public void onResponse(Call<Result> call, Response<Result> response) {
                    LoadTweets(0, TweetsType);
                    linearLayoutManager.scrollToPositionWithOffset(0, 0);
                }

                @Override
                public void onFailure(Call<Result> call, Throwable t1) {
                    hideProgressDialog();
                    Toast.makeText(getApplicationContext(), t1.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        void getLocationForTweet(String tweetText, String imageUrl, boolean addLocation) {
            if (addLocation) {
                MyGeocoderUtil.requestSingleUpdate(context,
                        new MyGeocoderUtil.LocationCallback() {
                            @Override
                            public void onNewLocationAvailable(Location location) {
                                Log.d("Location", "my location is " + location.getCountry() + " " + location.getCity());
                                sendTweetToDb(tweetText, imageUrl, new Location(location.getLatitude(), location.getLongitude(), location.getCountry(), location.getCity()));
                            }

                            @Override
                            public void failedToGetLocation() {
                                Toast.makeText(getApplicationContext(),"Could not obtain location",Toast.LENGTH_SHORT).show();
                                sendTweetToDb(tweetText, imageUrl, new Location(0f, 0f, "XX", "XX"));
                            }
                        });
            } else
                sendTweetToDb(tweetText, imageUrl, new Location(0f, 0f, "XX", "XX"));
        }

        void prepareTweet(String tweetText, boolean hasImage) {
            Log.d(TAG, "TweetAdd with picture");
            showProgressDialog();
            if (hasImage) {
                //Upload immage to firebase
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl("gs://tweeter-55347.appspot.com/");
                DateFormat df = DateFormat.getDateTimeInstance();
                Date dateobj = new Date();
                String myDownloadUrl = SaveSettings.getInstance(getApplicationContext()).getUser().getUserID() + "_" + df.format(dateobj) + ".jpg";
                final StorageReference picRef = storageRef.child(myDownloadUrl);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                loadImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] data = baos.toByteArray();
                final UploadTask uploadTask = picRef.putBytes(data);
                uploadTask.addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Image could not be uploaded: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, e.getMessage());
                })
                        .addOnSuccessListener(taskSnapshot -> picRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    CheckGpsPermission(tweetText, uri.toString());
                                }));
            } else {
                CheckGpsPermission(tweetText, "none");
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

            TweetItem t = tweetWallAdapter.get(position);
            if (t != null) {
                switch (t.getTweet_date()) {
                    case "add": {
                        final int[] counter = {0};
                        final String[] tweets = {""};
                        ((addTweetViewHolder) holder).etCounter.setText(getString(R.string.character_counter, counter[0]));
                        //((addTweetViewHolder) holder).etPost.setText(tweets[0]);
                        //((addTweetViewHolder) holder).etPost.setSelection(((addTweetViewHolder) holder).etPost.getText().length());

                        if (loadedImage) {
                            hideProgressDialog();
                            ((addTweetViewHolder) holder).etPost.setText(tweetText);
                            ((addTweetViewHolder) holder).iv_temp.setVisibility(ImageView.VISIBLE);
                            ((addTweetViewHolder) holder).iv_temp.setImageBitmap(loadImageBitmap);
                        } else {
                            ((addTweetViewHolder) holder).etPost.setText(tweets[0]);
                            ((addTweetViewHolder) holder).iv_temp.setVisibility(ImageView.INVISIBLE);
                            ((addTweetViewHolder) holder).iv_temp.setImageBitmap(null);
                        }
                        ((addTweetViewHolder) holder).iv_attach.setOnClickListener(view -> {
                            tweetText = ((addTweetViewHolder) holder).etPost.getText().toString();
                            CheckUserPermission();
                        });
                        ((addTweetViewHolder) holder).iv_post.setOnClickListener(view -> {
                            if (((addTweetViewHolder) holder).etPost.length() <= 0)
                                Toast.makeText(context, "Tweet is empty.", Toast.LENGTH_SHORT).show();
                            else if (((addTweetViewHolder) holder).etPost.length() <= 150) {
                                if (((addTweetViewHolder) holder).iv_temp.getVisibility() == View.VISIBLE)
                                    prepareTweet(((addTweetViewHolder) holder).etPost.getText().toString(), true);
                                else
                                    prepareTweet(((addTweetViewHolder) holder).etPost.getText().toString(), false);
                                //TODO: może od razu wyświetlić tweeta i 'uzupełnić' po pozytywnym wrzuceniu do bazy?
                                // tweetWallAdapter.add(1,new TweetItem(999,((addTweetViewHolder) holder).etPost.getText().toString(),"none","noDate", SaveSettings.getInstance(getApplicationContext()).getUser().getUserID(),SaveSettings.getInstance(getApplicationContext()).getUser().getUsername(),SaveSettings.getInstance(getApplicationContext()).getUser().getPicture_path(),0,false));
                                // notifyItemInserted(1);
                                //GlideApp.with(getApplicationContext()).load(t.tweet_picture).placeholder(R.drawable.round_background_white).optionalCenterCrop().into(((singleTweetHolder) holder).tweet_picture);
                                //GlideApp.with(getApplicationContext()).load(tweetWallAdapter.get(1).tweet_picture).placeholder(R.drawable.round_background_white).optionalCenterCrop().into(((singleTweetHolder) holder).tweet_picture);
                                //  tweetWallAdapter.get(1)

                                //hiding progress dialog
                                hideProgressDialog();
                                //displaying the message from the response as toast
                                //Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
                                ((addTweetViewHolder) holder).etPost.setText("");
                                //TODO: brzydkie, ale chowa klawiature
                                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                                loadedImage = false;
                                loadImageBitmap = null;
                                TweetsType = SearchType.MyFollowing;
                                LoadTweets(0, TweetsType);
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
                        break;
                    }
                    case "notweet": {
                        break;
                    }
                    default: {

                        ((singleTweetHolder) holder).tweet_picture.setVisibility(View.VISIBLE);
                        ((singleTweetHolder) holder).txtUserName.setText(t.getUsername());
                        ((singleTweetHolder) holder).txtUserName.setOnClickListener((View view) -> {
                            SelectedUserID = t.getUser_id();
                            if (SaveSettings.getInstance(getApplicationContext()).getUser().getUserID() != SelectedUserID) {
                                TweetsType = SearchType.OnePerson;
                                LoadTweets(0, TweetsType);
                                txtNameFollowers.setText(t.getUsername());
                                GlideApp.with(getApplicationContext()).load(t.getPicture_path()).placeholder(R.drawable.logo1).optionalCenterCrop().apply(RequestOptions.bitmapTransform(new RoundedCorners(8))).into(iv_channel_icon);

                                //TODO: I THINK FOLLOWING STATUS IS ALREADY IN 'tweetlist' REST CALL
                                Call<Result> call = APIUrl.getApi().checkFollowing(SaveSettings.getInstance(getApplicationContext()).getUser().getUserID(), SelectedUserID);
                                //calling the api
                                call.enqueue(new Callback<Result>() {
                                    @Override
                                    //TODO: SHOULD LIST IN SHAREDPREFERENCES ALSO UPDATE? POSSIBLY
                                    public void onResponse(Call<Result> call, Response<Result> response) {
                                        //hiding progress dialog
                                        hideProgressDialog();
                                        if (response.body().getError()) {
                                            buFollow.setText(R.string.buFollow_follow);
                                            buFollow.setSelected(false);
                                            Log.d(TAG, "Unfollowing user: " + SelectedUserID);
                                            FirebaseMessaging.getInstance().unsubscribeFromTopic(String.valueOf(SelectedUserID));

                                        } else {
                                            FirebaseMessaging.getInstance().subscribeToTopic(String.valueOf(SelectedUserID));
                                            Log.d(TAG, "Following user: " + SelectedUserID);
                                            //SaveSettings.getInstance(getApplicationContext()).saveArrayList(SaveSettings.getInstance(getApplicationContext()).getArrayList("FOLLOWING").,"FOLLOWING");
                                            buFollow.setText(R.string.buFollow_unFollow);
                                            buFollow.setSelected(true);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Result> call, Throwable t12) {
                                        hideProgressDialog();
                                        Toast.makeText(getApplicationContext(), t12.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        });

                        ((singleTweetHolder) holder).txt_tweet.setText(t.getTweet_text());
                        ((singleTweetHolder) holder).txt_tweet_date.setText(t.getTweet_date());
                        if (t.getTweetLocation() != null && t.getTweetLocation().getCity() != null && t.getTweetLocation().getCountry() != null) {
                            GlideApp.with(getApplicationContext()).load(R.drawable.gps_icon).into(((singleTweetHolder) holder).iv_location);
                            ((singleTweetHolder) holder).txt_location.setText(t.getTweetLocation().getCity() + ", " + t.getTweetLocation().getCountry());
                        } else {
                            GlideApp.with(getApplicationContext()).load(R.drawable.no_gps_icon).into(((singleTweetHolder) holder).iv_location);
                            ((singleTweetHolder) holder).txt_location.setText("");

                        }


                        if (t.getTweet_picture().equals("none") || t.getTweet_picture().equals("null"))
                            ((singleTweetHolder) holder).tweet_picture.setVisibility(View.GONE);
                        else
                            GlideApp.with(getApplicationContext()).load(t.getTweet_picture()).placeholder(R.drawable.round_background_white).optionalCenterCrop().into(((singleTweetHolder) holder).tweet_picture);


                        GlideApp.with(getApplicationContext()).load(t.getPicture_path()).placeholder(R.drawable.no_name_user).optionalCenterCrop().apply(RequestOptions.bitmapTransform(new RoundedCorners(8))).into(((singleTweetHolder) holder).picture_path);


                        ((singleTweetHolder) holder).iv_share.setOnClickListener(v -> {
                            if (!t.isFavourite()) {
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
                                t.setFavouriteCount(t.getFavouriteCount() + 1);
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
                                t.setFavouriteCount(t.getFavouriteCount() - 1);
                            }

                            t.setFavourite(!t.isFavourite());
                            ((singleTweetHolder) holder).favouriteCount.setText(String.valueOf(t.getFavouriteCount()));
                            //defining the call
                            Call<Result> call = APIUrl.getApi().favourite(SaveSettings.getInstance(getApplicationContext()).getUser().getUserID(), t.getTweet_id());
                            //calling the api
                            call.enqueue(new Callback<Result>() {


                                @Override
                                public void onResponse(Call<Result> call, Response<Result> response) {
                                    //hiding progress dialog
                                    hideProgressDialog();
                                }

                                @Override
                                public void onFailure(Call<Result> call, Throwable t13) {
                                    hideProgressDialog();
                                    Toast.makeText(getApplicationContext(), t13.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        });

                        if (!t.isFavourite())
                            GlideApp.with(getApplicationContext()).load(R.drawable.favourite1).into(((singleTweetHolder) holder).iv_share);
                        else
                            GlideApp.with(getApplicationContext()).load(R.drawable.favourite22).into(((singleTweetHolder) holder).iv_share);

                        ((singleTweetHolder) holder).favouriteCount.setText(String.valueOf(t.getFavouriteCount()));
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
            if (tweetWallAdapter == null)
                return 0;
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
        Log.d(TAG, "CheckUserPermission()");
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

    void CheckGpsPermission(String tweetText, String downloadUrl) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                this.tweetText = tweetText;
                this.downloadUrl = downloadUrl;
                requestPermissions(new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_ASK_GPSPERMISSIONS);
                return;
            }
            Log.d(TAG, "Gps permissions already granted");
        }
        myTweetWall.getLocationForTweet(tweetText, downloadUrl, true);
    }

    String tweetText;
    String downloadUrl;

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    final private int REQUEST_CODE_ASK_GPSPERMISSIONS = 321;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Log.d(TAG, "onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) <<" + requestCode);

        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Storage permission granted");
                    LoadImage();
                } else {
                    Toast.makeText(this, "your message", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_CODE_ASK_GPSPERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "GPS permission granted");
                    myTweetWall.getLocationForTweet(tweetText, downloadUrl, true);
                } else {
                    myTweetWall.getLocationForTweet(tweetText, downloadUrl, false);
                }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    int RESULT_LOAD_IMAGE = 346;

    void LoadImage() {

        Log.d(TAG, "LoadImage()");
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult(int requestCode, int resultCode, Intent data) >> " + requestCode + " >> " + resultCode + " >> " + data);

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
            loadedImage = true;
            loadImageBitmap = Bitmap.createScaledBitmap(b, Math.round(actualWidth), Math.round(actualHeight), false);
            myTweetWall.notifyItemChanged(0);
        }
    }

    public static int getExifRotation(String filePath) {

        Log.d(TAG, "getExifRotation(String filePath) >> " + filePath);

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
        Log.i(TAG, "LOADING TWEETS");
        int user_id = SaveSettings.getInstance(getApplicationContext()).getUser().getUserID();
        this.StartFrom = StartFrom;
        this.TweetsType = TweetType;
        //display loading
        if (StartFrom == 0) // add loading at beggining
            tweetWall.add(0, new TweetItem(0, null, null,
                    "loading", 0, null, null, 0, false, 1, 2, "country", "city"));
        else // add loading at end
            tweetWall.add(new TweetItem(0, null, null,
                    "loading", 0, null, null, 0, false, 1, 2, "country", "city"));

        myTweetWall.notifyDataSetChanged();

        //defining the call
        Log.i(TAG, "USER ID: " + user_id + " STARTFROM: " + StartFrom + " QUERY: " + Query + " TWEETTYPE: " + TweetType + " CHECK USER ID: " + SelectedUserID);
        Call<Result> call = APIUrl.getApi().tweetList(user_id, StartFrom, Query, TweetType, SelectedUserID);

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
                                "add", 0, null, null, 0, false, 1, 2, "country", "city"));
                    } else {
                        //remove we are loading now
                        tweetWall.remove(tweetWall.size() - 1);
                        tweetWall.add(new TweetItem(0, null, null,
                                "notweet", 0, null, null, 0, false, 1, 2, "country", "city"));
                    }
                } else {
                    if (StartFrom == 0) {
                        tweetWall.clear();
                        tweetWall.add(new TweetItem(0, null, null,
                                "add", 0, null, null, 0, false, 1, 2, "country", "city"));

                    } else {
                        //remove we are loading now
                        tweetWall.remove(tweetWall.size() - 1);
                    }

                    // try to add the resourcess
                    //add data and view it
                    for (TweetItem t : response.body().getTweets()) {
                        t.setTweetLocationFromData();
                        tweetWall.add(t);
                    }
                }
                LoadMore = true;
                myTweetWall.notifyDataSetChanged();
                //displaying the message from the response as toast
                //Toast.makeText(getApplicationContext(), response.body().getMessage(), Toast.LENGTH_LONG).show();
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

