package org.tensorflow.lite.examples.detection.Model.Json;


import com.google.gson.annotations.SerializedName;

public class JsonInfran {

    @SerializedName("data")
    private String data;

    @SerializedName("hash")
    private String hash;

    @SerializedName("threshold")
    private float threshold;


    public JsonInfran(String data , String hash , float threshold) {
        this.data= data;
        this.hash= hash;
        this.threshold= threshold;
    }


}


