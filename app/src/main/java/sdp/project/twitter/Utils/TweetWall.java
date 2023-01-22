package sdp.project.twitter.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
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
import sdp.project.twitter.API.Result;
import sdp.project.twitter.Model.SearchType;
import sdp.project.twitter.Model.TweetItem;

/*
public class TweetWall extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final static String TAG = "TweetWall";
    ArrayList<TweetItem> tweetWallAdapter;
    Context context;

    private class addTweetViewHolder extends RecyclerView.ViewHolder{

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
                    ((addTweetViewHolder) holder).etCounter.setText(context.getString(R.string.character_counter, counter[0]));
                    ((addTweetViewHolder) holder).etPost.setText(tweets[0]);
                    ((addTweetViewHolder) holder).etPost.setSelection(((addTweetViewHolder) holder).etPost.getText().length());


                    if (loadedImage) {
                        hideProgressDialog();
                        ((addTweetViewHolder) holder).iv_temp.setVisibility(ImageView.VISIBLE);
                        ((addTweetViewHolder) holder).iv_temp.setImageBitmap(loadImageBitmap);
                    }else{
                        ((addTweetViewHolder) holder).iv_temp.setVisibility(ImageView.INVISIBLE);
                        ((addTweetViewHolder) holder).iv_temp.setImageBitmap(null);
                    }
                    ((addTweetViewHolder) holder).iv_attach.setOnClickListener(view -> CheckUserPermission());
                    ((addTweetViewHolder) holder).iv_post.setOnClickListener(view -> {
                        if (((addTweetViewHolder) holder).etPost.length() <= 0)
                            Toast.makeText(context, "Tweet is empty.", Toast.LENGTH_SHORT).show();
                            //TODO: MERGE INTO ONE RETROFIT CALL
                        else if (((addTweetViewHolder) holder).etPost.length() <= 150) {
                            if (((addTweetViewHolder) holder).iv_temp.getVisibility() == View.VISIBLE) {
                                showProgressDialog(context);
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                // Create a storage reference from our app
                                StorageReference storageRef = storage.getReferenceFromUrl("gs://tweeter-55347.appspot.com/");
                                DateFormat df = DateFormat.getDateTimeInstance();
                                Date dateobj = new Date();
                                // System.out.println(df.format(dateobj));
                                // Create a reference to "mountains.jpg"
                                String myDownloadUrl = SaveSettings.getInstance(context.getApplicationContext()).getUser().getUserID() + "_" + df.format(dateobj) + ".jpg";
                                final StorageReference picRef = storageRef.child(myDownloadUrl);
                                ((addTweetViewHolder) holder).iv_temp.setDrawingCacheEnabled(true);
                                ((addTweetViewHolder) holder).iv_temp.buildDrawingCache();
                                BitmapDrawable drawable = (BitmapDrawable) ((addTweetViewHolder) holder).iv_temp.getDrawable();
                                Bitmap bitmap = drawable.getBitmap();
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                byte[] data = baos.toByteArray();

                                final UploadTask uploadTask = picRef.putBytes(data);
                                uploadTask.addOnFailureListener(e -> Toast.makeText(context, "Image could not be uploaded: " + e.getMessage(), Toast.LENGTH_LONG).show())
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
                                                    Call<Result> call = service.tweetAdd(SaveSettings.getInstance(context).getUser().getUserID(), ((addTweetViewHolder) holder).etPost.getText().toString(), downloadUrl);

                                                    //calling the api
                                                    call.enqueue(new Callback<Result>() {
                                                        @Override
                                                        public void onResponse(Call<Result> call, Response<Result> response) {
                                                            //hiding progress dialog
                                                            hideProgressDialog();
                                                            //displaying the message from the response as toast
                                                            Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_LONG).show();
                                                            ((addTweetViewHolder) holder).etPost.setText("");
                                                            loadedImage = false;
                                                            loadImageBitmap = null;
                                                            TweetsType = SearchType.MyFollowing;
                                                            LoadTweets(0, TweetsType);
                                                        }

                                                        @Override
                                                        public void onFailure(Call<Result> call, Throwable t1) {
                                                            hideProgressDialog();
                                                            Toast.makeText(context, t1.getMessage(), Toast.LENGTH_LONG).show();
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
                                Call<Result> call = service.tweetAdd(SaveSettings.getInstance(context).getUser().getUserID(), ((addTweetViewHolder) holder).etPost.getText().toString(), "none");

                                //calling the api
                                call.enqueue(new Callback<Result>() {
                                    @Override
                                    public void onResponse(Call<Result> call, Response<Result> response) {
                                        //hiding progress dialog
                                        hideProgressDialog();
                                        //displaying the message from the response as toast
                                        Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_LONG).show();
                                        ((addTweetViewHolder) holder).etPost.setText("");
                                        loadedImage = false;
                                        loadImageBitmap = null;
                                        TweetsType = SearchType.MyFollowing;
                                        LoadTweets(0, TweetsType);
                                    }

                                    @Override
                                    public void onFailure(Call<Result> call, Throwable t1) {
                                        hideProgressDialog();
                                        Toast.makeText(context, t1.getMessage(), Toast.LENGTH_LONG).show();
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
                            ((addTweetViewHolder) holder).etCounter.setText(context.getString(R.string.character_counter, counter[0]));
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
                        if (SaveSettings.getInstance(context).getUser().getUserID() != SelectedUserID) {
                            TweetsType = SearchType.OnePerson;
                            LoadTweets(0, TweetsType);
                            txtNameFollowers.setText(t.username);
                            //Picasso.get().load(t.picture_path).into(iv_channel_icon);
                            //Glide.with(getApplicationContext()).load(t.picture_path).into(iv_channel_icon);
                            GlideApp.with(context).load(t.picture_path).optionalCenterCrop().into(iv_channel_icon);

                            //TODO: I THINK FOLLOWING STATUS IS ALREADY IN 'tweetlist' REST CALL
                            //building retrofit object
                            Retrofit retrofit = new Retrofit.Builder()
                                    .baseUrl(APIUrl.BASE_URL)
                                    .addConverterFactory(GsonConverterFactory.create())
                                    .build();

                            //Defining retrofit api service
                            APIService service = retrofit.create(APIService.class);

                            //defining the call
                            Call<Result> call = service.checkFollowing(SaveSettings.getInstance(context).getUser().getUserID(), SelectedUserID);

                            //calling the api
                            call.enqueue(new Callback<Result>() {
                                @Override
                                public void onResponse(Call<Result> call, Response<Result> response) {
                                    //hiding progress dialog
                                    hideProgressDialog();
                                    if (response.body().getError()) {
                                        buFollow.setText(R.string.buFollow_follow);
                                        buFollow.setSelected(false);
                                        FirebaseMessaging.getInstance().unsubscribeFromTopic(String.valueOf(SelectedUserID));
                                    } else {
                                        FirebaseMessaging.getInstance().subscribeToTopic(String.valueOf(SelectedUserID));
                                        buFollow.setText(R.string.buFollow_unFollow);
                                        buFollow.setSelected(true);
                                    }
                                    //displaying the message from the response as toast
                                    Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(Call<Result> call, Throwable t12) {
                                    hideProgressDialog();
                                    Toast.makeText(context, t12.getMessage(), Toast.LENGTH_LONG).show();
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
                        GlideApp.with(context).load(t.tweet_picture).placeholder(R.drawable.round_background_white).optionalCenterCrop().into(((singleTweetHolder) holder).tweet_picture);
                    //Picasso.get().load(t.tweet_picture).into(((singleTweetHolder) holder).tweet_picture);
                    //Glide.with(getApplicationContext()).load(t.picture_path).into(((singleTweetHolder) holder).picture_path);
                    //GlideApp.with(getApplicationContext()).load(t.picture_path).optionalCenterCrop().into(((singleTweetHolder) holder).picture_path);


                    ((singleTweetHolder) holder).iv_share.setOnClickListener(v -> {
                        if (!t.isFavourite) {
                            CustomAnimationDrawable cad = new CustomAnimationDrawable((AnimationDrawable) context.getResources().getDrawable(R.drawable.favourite_animation)) {
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
                            CustomAnimationDrawable cad = new CustomAnimationDrawable((AnimationDrawable) context.getResources().getDrawable(R.drawable.unfavourite_animation)) {
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
                        Call<Result> call = service.favourite(SaveSettings.getInstance(context).getUser().getUserID(), t.tweet_id);

                        //calling the api
                        call.enqueue(new Callback<Result>() {


                            @Override
                            public void onResponse(Call<Result> call, Response<Result> response) {
                                Log.i(TAG,"restcall");
                                //hiding progress dialog
                                hideProgressDialog();
                                //displaying the message from the response as toast
                                Toast.makeText(context, response.body().getMessage(), Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onFailure(Call<Result> call, Throwable t13) {
                                hideProgressDialog();
                                Toast.makeText(context, t13.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    });

                    if (!t.isFavourite)
                        GlideApp.with(context).load(R.drawable.favourite1).into(((singleTweetHolder) holder).iv_share);
                    else
                        GlideApp.with(context).load(R.drawable.favourite22).into(((singleTweetHolder) holder).iv_share);

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
*/