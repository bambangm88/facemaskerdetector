package org.tensorflow.lite.examples.detection.Model.Data;

import com.google.gson.annotations.SerializedName;

public class ModelDataInfran {


    @SerializedName("person_in_picture")
    private String person_in_picture;

    @SerializedName("pip_nik")
    private String pip_nik;

    @SerializedName("pip_loc")
    private String[] pip_loc;

    @SerializedName("pip_real")
    private String[] pip_real;



    public String getPerson_in_picture() {
        return person_in_picture;
    }

    public void setPerson_in_picture(String person_in_picture) {
        this.person_in_picture = person_in_picture;
    }

    public String getPip_nik() {
        return pip_nik;
    }

    public void setPip_nik(String pip_nik) {
        this.pip_nik = pip_nik;
    }

    public String[] getPip_loc() {
        return pip_loc;
    }

    public void setPip_loc(String[] pip_loc) {
        this.pip_loc = pip_loc;
    }

    public String[] getPip_real() {
        return pip_real;
    }

    public void setPip_real(String[] pip_real) {
        this.pip_real = pip_real;
    }


}
