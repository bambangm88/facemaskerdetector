package org.tensorflow.lite.examples.detection.Model.Response;

import com.google.gson.annotations.SerializedName;

import org.tensorflow.lite.examples.detection.Model.Data.ModelDataInfran;

import java.util.List;


public class ResponseDataInfran {




    @SerializedName("Success")
    private String Success;

    @SerializedName("status")
    private String status;

    @SerializedName("err_code")
    private String err_code;

    @SerializedName("data")
    private List<ModelDataInfran> data;


    public List<ModelDataInfran> getData() {
        return data;
    }

    public void setData(List<ModelDataInfran> data) {
        this.data = data;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getErr_code() {
        return err_code;
    }

    public void setErr_code(String err_code) {
        this.err_code = err_code;
    }


    public String getSuccess() {
        return Success;
    }

    public void setSuccess(String success) {
        Success = success;
    }



















}







