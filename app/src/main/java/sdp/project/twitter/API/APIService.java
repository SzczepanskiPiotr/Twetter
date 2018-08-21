package sdp.project.twitter.API;


import retrofit2.Call;

import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import sdp.project.twitter.Result;

public interface APIService {

    //The register call
    @FormUrlEncoded
    @POST("register")
    Call<Result> createUser(
            @Field("name") String name,
            @Field("email") String email,
            @Field("password") String password,
            @Field("picture_path") String picture_path);

    //The login call
    @FormUrlEncoded
    @POST("login")
    Call<Result> loginUser(
            @Field("username") String username,
            @Field("password") String password);

    //Follow other user call
    @FormUrlEncoded
    @POST("followuser")
    Call<Result> followUser(
            @Field("user_id") int userId,
            @Field("followUser_id") int followUserId,
            @Field("op") int op);

    //Add new tweet call
    @FormUrlEncoded
    @POST("checkfollowing")
    Call<Result> checkFollowing(
            @Field("user_id") int user_id,
            @Field("followUser_id") int followUser_id);

    //Add new tweet call
    @FormUrlEncoded
    @POST("tweetadd")
    Call<Result> tweetAdd(
            @Field("user_id") int user_id,
            @Field("tweet_text") String tweet_text,
            @Field("tweet_picture") String tweet_picture);

    //Add new tweet call
    @FormUrlEncoded
    @POST("tweetlist")
    Call<Result> tweetList(
            @Field("user_id") int user_id,
            @Field("startFrom") int startFrom,
            @Field("query") String query,
            @Field("op") int op);


    //Add new tweet call
    @FormUrlEncoded
    @POST("favourite")
    Call<Result> favourite(
            @Field("user_id") int user_id,
            @Field("tweet_id") int tweet_id);

}
