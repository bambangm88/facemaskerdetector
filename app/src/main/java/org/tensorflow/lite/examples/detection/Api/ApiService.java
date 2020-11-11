package org.tensorflow.lite.examples.detection.Api;




import org.tensorflow.lite.examples.detection.Model.Json.JsonInfran;
import org.tensorflow.lite.examples.detection.Model.Response.ResponseDataInfran;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;


public interface ApiService {


    //region infran

    @Headers("Content-Type: application/json")
    @POST("api/infran/whoisit")
    Call<ResponseDataInfran> requestInfran(@Body JsonInfran body);

    //end region infran




}
