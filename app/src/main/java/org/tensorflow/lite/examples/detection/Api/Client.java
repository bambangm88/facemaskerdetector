package org.tensorflow.lite.examples.detection.Api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


import org.tensorflow.lite.examples.detection.Helper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static org.tensorflow.lite.examples.detection.Constant.Utility.AUTH_PWD;
import static org.tensorflow.lite.examples.detection.Constant.Utility.AUTH_USER;


public class Client {

    private static Retrofit retrofit = null;
    private static Retrofit retrofitInfran = null;




    public static Retrofit getClient(String baseUrl){
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor)
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();


        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        if (retrofit == null){
            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build();
        }
        return retrofit;
    }



    public static Retrofit getClientInfran(String baseUrl){

        String Auth = AUTH_USER+":"+AUTH_PWD ;
        String Authorized = "Basic "+ Helper.EncriptToBase64(Auth);

        Log.e("TAG", "Authorized: "+Authorized );

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor)
                .connectTimeout(1, TimeUnit.MINUTES)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Chain chain) throws IOException {
                        Request newRequest = chain.request().newBuilder()
                                .addHeader("Authorization", Authorized)
                                .build();
                        return chain.proceed(newRequest);
                    }
                })
                .addInterceptor(interceptor)
                .build();


        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        if (retrofitInfran == null){
            retrofitInfran = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)

                    .build();
        }
        return retrofitInfran;
    }








}
