package sdp.project.twitter.API;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class APIUrl {
        public static final String BASE_URL = "https://pszczepanski.000webhostapp.com/";

        public static APIService getApi(){
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(APIUrl.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            //Defining retrofit api service
            APIService service = retrofit.create(APIService.class);
            return  service;
        }
    }

