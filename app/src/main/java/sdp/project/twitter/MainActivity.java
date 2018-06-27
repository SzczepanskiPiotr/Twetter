package sdp.project.twitter;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.LocalBroadcastManager;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import sdp.project.tweeter.R;

public class MainActivity extends AppCompatActivity {

    ArrayList<TweetItem> tweetWall = new ArrayList<>();
    int StartFrom = 0;
    int TweetsType = SearchType.MyFollowing;
    int totalItemCountVisible = 0; //totalItems visible
    LinearLayout ChannelInfo;
    TextView txtnamefollowers;
    ImageView iv_channel_icon;
    int SelectedUserID = 0;
    Button buFollow;
    TweetWall myTweetWall;
    SaveSettings saveSettings;

    //firebase
    private static final String TAG = "AnonymousAuth";
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };


        //Info of other users, hidden on main screen
        ChannelInfo = findViewById(R.id.ChannelInfo);
        ChannelInfo.setVisibility(View.GONE);
        txtnamefollowers = findViewById(R.id.txtnamefollowers);
        iv_channel_icon = findViewById(R.id.iv_channel_icon);
        //button
        buFollow = findViewById(R.id.buFollow);
        //load user
        saveSettings = new SaveSettings(getApplicationContext());
        if(!saveSettings.LoadData()){
            finish();
            return;
        }
        //Log.i("UserID", User.getInstance(getApplicationContext()).getUserID());
        //tweetWall
        myTweetWall = new TweetWall(this,tweetWall);
        ListView lsNews = findViewById(R.id.LVNews);
        lsNews.setItemsCanFocus(true);
        lsNews.setAdapter(myTweetWall);//intisal with data
        LoadTweets(0,SearchType.MyFollowing);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());
                if(!task.isSuccessful()){
                    Log.w(TAG, "signInAnonymously", task.getException());
                }
            }
        });
    }

    SearchView searchView;
    Menu myMenu;
    String Query;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // add menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        myMenu = menu;
        // searchView code
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (android.widget.SearchView) menu.findItem(R.id.searchbar).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        //final Context co=this;
        searchView.setOnQueryTextListener(new android.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Toast.makeText(co, query, Toast.LENGTH_LONG).show();
                Query = null;
                try {
                    //for space with name
                    Query = java.net.URLEncoder.encode(query, "UTF-8");
                } catch (UnsupportedEncodingException e) {

                }
                LoadTweets(0,SearchType.SearchIn);// search
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
                LoadTweets(0,SearchType.MyFollowing);
                return true;
            }
            case R.id.logout: {
                saveSettings.ClearData();
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
            buFollow.setText("Un Follow");
            //buFollow.set
        } else {
            buFollow.setSelected(false);
            Operation = 2;
            buFollow.setText("Follow");
        }
        String url = "https://pszczepanski.000webhostapp.com/UserFollowing.php?user_id=" + User.getInstance(getApplicationContext()).getUserID() + "&following_user_id=" + SelectedUserID + "&op=" + Operation;
        new MyAsyncTaskGetNews().execute(url);
    }

    ImageView iv_temp;
    TextView etCounter;
    int counter;
    EditText etPost;
    String downloadUrl = null;
    String tweets;

    Boolean loadedImage = false;
    String loadImagePath = "";

    int id;

    private class TweetWall extends BaseAdapter {

        public ArrayList<TweetItem> tweetWallAdapter;
        Context context;

        public TweetWall(Context context, ArrayList<TweetItem> tweetWallAdapter) {
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

            if (t.tweet_date.equals("add")) {
                LayoutInflater mInflater = getLayoutInflater();
                View myView = mInflater.inflate(R.layout.tweet_new, null);
                etCounter = myView.findViewById(R.id.etCounter);
                etCounter.setText(counter+"/150");
                etPost = myView.findViewById(R.id.etPost);
                etPost.setText(tweets);
                etPost.setSelection(etPost.getText().length());
                ImageView iv_post = myView.findViewById(R.id.iv_post);
                ImageView iv_attach = myView.findViewById(R.id.iv_attach);
                iv_temp = myView.findViewById(R.id.iv_temp);
                iv_temp.setVisibility(ImageView.GONE);
                if(loadedImage == true){
                    iv_temp.setImageBitmap(BitmapFactory.decodeFile(loadImagePath));
                    iv_temp.setVisibility(ImageView.VISIBLE);
                }
                iv_attach.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LoadImage();
                    }
                });
                iv_post.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            //for space with name
                            tweets = java.net.URLEncoder.encode(etPost.getText().toString(), "UTF-8");
                            //downloadUrl = java.net.URLEncoder.encode(downloadUrl, "UTF-8");

                        } catch (UnsupportedEncodingException e) {
                            tweets = "Error";
                        }
                        if(etPost.length()<=0) Toast.makeText(context,"Tweet is empty.",Toast.LENGTH_SHORT).show();
                        else if(etPost.length() <= 150) {
                            if(iv_temp.getVisibility()==View.VISIBLE){
                            showProgressDialog();
                            FirebaseStorage storage = FirebaseStorage.getInstance();
                            // Create a storage reference from our app
                            StorageReference storageRef = storage.getReferenceFromUrl("gs://tweeter-55347.appspot.com/");
                            DateFormat df = new SimpleDateFormat("ddMMyyHHmmss");
                            Date dateobj = new Date();
                            // System.out.println(df.format(dateobj));
                            // Create a reference to "mountains.jpg"
                            String myDownloadUrl = User.getInstance(getApplicationContext()).getUserID() + "_" + df.format(dateobj) + ".jpg";
                            final StorageReference picRef = storageRef.child(myDownloadUrl);
                            iv_temp.setDrawingCacheEnabled(true);
                            iv_temp.buildDrawingCache();
                            BitmapDrawable drawable = (BitmapDrawable)iv_temp.getDrawable();
                            Bitmap bitmap = drawable.getBitmap();
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                            byte[] data = baos.toByteArray();

                            final UploadTask uploadTask = picRef.putBytes(data);
                            uploadTask.addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "Image could not be uploaded: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                }}).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    picRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            Uri downloadUri = uri;
                                            downloadUrl = downloadUri.toString();
                                            hideProgressDialog();
                                            Toast.makeText(context,"Tweet added",Toast.LENGTH_SHORT).show();
                                            String url = "https://pszczepanski.000webhostapp.com/TweetAdd.php?user_id=" + User.getInstance(getApplicationContext()).getUserID() + "&tweet_text=" + tweets + "&tweet_picture=" + downloadUrl;
                                            new MyAsyncTaskGetNews().execute(url);
                                        }
                                    });
                                }
                            });
                            }else {
                                String url = "https://pszczepanski.000webhostapp.com/TweetAdd.php?user_id=" + User.getInstance(getApplicationContext()).getUserID() + "&tweet_text=" + tweets + "&tweet_picture=" + downloadUrl;
                                new MyAsyncTaskGetNews().execute(url);
                            }
                            //etPost.setText("");
                        }
                        else Toast.makeText(context,"Tweet is too long",Toast.LENGTH_SHORT).show();
                    }
                });

                final TextWatcher mTextEditorWatcher = new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        counter = s.length();
                        tweets = s.toString();
                        etCounter.setText(String.valueOf(s.length())+"/150");
                        if(s.length()>150){
                            etCounter.setTextColor(Color.RED);
                        }
                        else{
                            etCounter.setTextColor(Color.WHITE);
                        }
                    }
                    public void afterTextChanged(Editable s) {
                    }
                };
                etPost.addTextChangedListener(mTextEditorWatcher);
                return myView;

            } else if (t.tweet_date.equals("loading")) {
                LayoutInflater mInflater = getLayoutInflater();
                View myView = mInflater.inflate(R.layout.tweet_load, null);
                return myView;
            } else if (t.tweet_date.equals("notweet")) {
                LayoutInflater mInflater = getLayoutInflater();
                View myView = mInflater.inflate(R.layout.tweet_info, null);
                return myView;
            } else {
                LayoutInflater mInflater = getLayoutInflater();
                View myView = mInflater.inflate(R.layout.tweet_single, null);

                TextView txtUserName = myView.findViewById(R.id.txtUserName);
                txtUserName.setText(t.username);
                txtUserName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        SelectedUserID = Integer.parseInt(t.user_id);
                        if(Integer.parseInt(User.getInstance(getApplicationContext()).getUserID()) != SelectedUserID){
                            LoadTweets(0, SearchType.OnePerson);
                            txtnamefollowers.setText(t.username);
                            Picasso.get().load(t.picture_path).into(iv_channel_icon);
                            String url = "https://pszczepanski.000webhostapp.com/IsFollowing.php?user_id=" + User.getInstance(getApplicationContext()).getUserID() + "&following_user_id=" + SelectedUserID;
                            new MyAsyncTaskGetNews().execute(url);
                        }
                    }
                });
                TextView txt_tweet = myView.findViewById(R.id.txt_tweet);
                txt_tweet.setText(t.tweet_text);

                TextView txt_tweet_date = myView.findViewById(R.id.txt_tweet_date);
                txt_tweet_date.setText(t.tweet_date);

                ImageView tweet_picture = myView.findViewById(R.id.tweet_picture);
                Picasso.get().load(t.tweet_picture).into(tweet_picture);
                ImageView picture_path = myView.findViewById(R.id.picture_path);
                Picasso.get().load(t.picture_path).into(picture_path);

                final ImageView iv_share = myView.findViewById(R.id.iv_share);

                iv_share.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        id = tweetWall.indexOf(t);
                        int Operation;
                        if(iv_share.getBackground().getConstantState() == getResources().getDrawable(R.drawable.favourite).getConstantState()){
                            Operation = 1;
                            iv_share.setBackgroundResource(R.drawable.favourited);
                        }
                        else{
                            iv_share.setBackgroundResource(R.drawable.favourite);
                            Operation = 2;
                        }
                        String url = "https://pszczepanski.000webhostapp.com/Favourite.php?user_id=" + User.getInstance(getApplicationContext()).getUserID() + "&tweet_id=" + t.tweet_id + "&op=" + Operation;
                        Log.i("URL",""+url);
                        new MyAsyncTaskGetNews().execute(url);
                    }
                });

                if(!t.isFavourite)
                    iv_share.setBackgroundResource(R.drawable.favourite);
                else
                    iv_share.setBackgroundResource(R.drawable.favourited);

                TextView favouriteCount = myView.findViewById(R.id.txt_favouriteCount);
                favouriteCount.setText(""+t.favouriteCount);

                return myView;
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


        //save image
        int RESULT_LOAD_IMAGE = 233;

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

                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();
                // postImage.setImageBitmap(BitmapFactory.decodeFile(picturePath));

                iv_temp.setImageBitmap(BitmapFactory.decodeFile(picturePath));
                iv_temp.setVisibility(ImageView.VISIBLE);
                loadedImage = true;
                loadImagePath = picturePath;
            }
        }

        // get news from server
        public class MyAsyncTaskGetNews extends AsyncTask<String, String, String> {
            @Override
            protected void onPreExecute() {
                //before works
            }

            @Override
            protected String doInBackground(String... params) {
                try {
                    String NewsData;
                    //define the url we have to connect with
                    URL url = new URL(params[0]);
                    //make connect with url and send request
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    //waiting for 7000ms for response
                    urlConnection.setConnectTimeout(7000);//set timeout to 5 seconds

                    try {
                        //getting the response data
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        //convert the stream to string
                        Operations operations = new Operations(getApplicationContext());
                        NewsData = operations.ConvertInputToStringNoChange(in);
                        //send to display data
                        publishProgress(NewsData);
                    } finally {
                        //end connection
                        urlConnection.disconnect();
                    }

                } catch (Exception ex) { }
                return null;
            }

            protected void onProgressUpdate(String... progress) {
                try {
                    JSONObject json = new JSONObject(progress[0]);
                    //display response data
                    if (json.getString("msg") == null)
                        return;

                    if (json.getString("msg").equalsIgnoreCase("tweet is added")) {
                        Log.i("LOL","WHY");
                        LoadTweets(0, TweetsType);
                    } else if (json.getString("msg").equalsIgnoreCase("has tweet")) {
                        if (StartFrom == 0) {
                            tweetWall.clear();
                            tweetWall.add(new TweetItem(null, null, null,
                                    "add", null, null, null));

                        } else {
                            //remove we are loading now
                            tweetWall.remove(tweetWall.size() - 1);
                        }
                        JSONArray tweets = new JSONArray(json.getString("info"));

                        for (int i = 0; i < tweets.length(); i++) {
                            // try to add the resourcess
                            JSONObject js = tweets.getJSONObject(i);

                            //add data and view it
                            tweetWall.add(new TweetItem(js.getString("tweet_id"),
                                    js.getString("tweet_text"), js.getString("tweet_picture"),
                                    js.getString("tweet_date"), js.getString("user_id"), js.getString("username")
                                    , js.getString("picture_path")));

                            //id = Integer.parseInt(js.getString("tweet_id"));
                            String url = "https://pszczepanski.000webhostapp.com/IsFavourite.php?user_id=" + User.getInstance(getApplicationContext()).getUserID() + "&tweet_id=" + js.getString("tweet_id");
                            new MyAsyncTaskGetNews().execute(url);
                        }

                        myTweetWall.notifyDataSetChanged();

                    } else if (json.getString("msg").equalsIgnoreCase("no tweet")) {
                        //remove we are loading now
                        if (StartFrom == 0) {
                            tweetWall.clear();
                            tweetWall.add(new TweetItem(null, null, null,
                                    "add", null, null, null));
                        } else {
                            //remove we are loading now
                            tweetWall.remove(tweetWall.size() - 1);
                        }
                        // listnewsData.remove(listnewsData.size()-1);
                        tweetWall.add(new TweetItem(null, null, null,
                                "notweet", null, null, null));
                    } else if (json.getString("msg").equalsIgnoreCase("is subscriber")) {
                        buFollow.setText("Un Follow");
                        buFollow.setSelected(true);
                    } else if (json.getString("msg").equalsIgnoreCase("is not subscriber")) {
                        buFollow.setText("Follow");
                    } else if (json.getString("msg").equalsIgnoreCase("favourite is updated")){
                        tweetWall.get(id).isFavourite = !tweetWall.get(id).isFavourite ;
                        JSONObject UserInfo = new JSONObject( json.getString("info"));
                        //JSONObject UserCredential = UserInfo.getJSONObject(0);
                        //Log.i("count",UserInfo.getString("count(tweet_id)"));
                        tweetWall.get(id).favouriteCount  = UserInfo.getString("count(tweet_id)");
                    } else if (json.getString("msg").equalsIgnoreCase("is favourite")){
                        JSONArray UserInfo = new JSONArray( json.getString("info"));
                        JSONObject UserCredential = UserInfo.getJSONObject(0);
                        findTweet(tweetWall,UserCredential.getString("tweet_id")).isFavourite = true;
                        //findTweetByID(tweetWall,UserCredential.getString("tweet_id")).isFavourite = true;
                        JSONObject FavouriteCount = new JSONObject( json.getString("count"));
                        Log.i("count",FavouriteCount.getString("count(tweet_id)"));
                        findTweet(tweetWall,UserCredential.getString("tweet_id")).favouriteCount  = FavouriteCount.getString("count(tweet_id)");
                        //findTweetByID(tweetWall,UserCredential.getString("tweet_id")).favouriteCount  = FavouriteCount.getString("count(tweet_id)");
                    } else if (json.getString("msg").equalsIgnoreCase("is not favourite")){
                        JSONObject FavouriteCount = new JSONObject( json.getString("count"));
                        if(!FavouriteCount.getString("tweet_id").equals("null")){
                            Log.i("count",FavouriteCount.getString("tweet_id"));
                            findTweet(tweetWall,FavouriteCount.getString("tweet_id")).favouriteCount  = FavouriteCount.getString("count(tweet_id)");
                            //findTweetByID(tweetWall,FavouriteCount.getString("tweet_id")).favouriteCount  = FavouriteCount.getString("count(tweet_id)");
                        }
                }

                } catch (Exception ex) {
                    Log.d("er", ex.getMessage());
                    //first time
                    tweetWall.clear();
                    tweetWall.add(new TweetItem(null, null, null,
                            "add", null, null, null));
                }

                myTweetWall.notifyDataSetChanged();

            }

            protected void onPostExecute(String result2) {
                downloadUrl = null;
                loadedImage = false;
                tweets = "";
                counter = 0;
                id = 0;
            }
        }

    public static TweetItem findTweet(Collection<TweetItem> a, String tweetID) {
            if(android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.N)
            {
                return a.stream().filter(tweet_ID -> tweetID.equals(tweet_ID.tweet_id)).findFirst().orElse(null);
            }
            for(TweetItem t : a) {
                if(t.tweet_id != null) {
                    if (t.tweet_id.equals(tweetID)) {
                        Log.i("T", t.tweet_id);
                        return t;
                    }
                }
            }
            return null;
        }

        void LoadTweets(int StartFrom, int TweetType) {
            this.StartFrom = StartFrom;
            this.TweetsType = TweetType;
            //display loading
            if (StartFrom == 0) // add loading at beggining
                tweetWall.add(0, new TweetItem(null, null, null,
                        "loading", null, null, null));
            else // add loading at end
                tweetWall.add(new TweetItem(null, null, null,
                        "loading", null, null, null));

            myTweetWall.notifyDataSetChanged();

            String url = "https://pszczepanski.000webhostapp.com/TweetList.php?user_id=" + User.getInstance(getApplicationContext()).getUserID() + "&StartFrom=" + StartFrom + "&op=" + TweetType;
            if (TweetType == SearchType.SearchIn)
                url = "https://pszczepanski.000webhostapp.com/TweetList.php?user_id=" + User.getInstance(getApplicationContext()).getUserID() + "&StartFrom=" + StartFrom + "&op=" + TweetType + "&query=" + Query;
            if (TweetType == SearchType.OnePerson)
                url = "https://pszczepanski.000webhostapp.com/TweetList.php?user_id=" + SelectedUserID + "&StartFrom=" + StartFrom + "&op=" + TweetType;

            new MyAsyncTaskGetNews().execute(url);

            if (TweetType == SearchType.OnePerson)
                ChannelInfo.setVisibility(View.VISIBLE);
            else
                ChannelInfo.setVisibility(View.GONE);
        }
}