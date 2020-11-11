package org.tensorflow.lite.examples.detection.Api;


import static org.tensorflow.lite.examples.detection.Constant.Utility.BASE_URL_API;
import static org.tensorflow.lite.examples.detection.Constant.Utility.BASE_URL_API_INFRAN;

public class Server {
    public static ApiService getAPIService() {
        return Client.getClient(BASE_URL_API).create(ApiService.class);
    }


    public static ApiService getAPIServiceInfran() {
        return Client.getClientInfran(BASE_URL_API_INFRAN).create(ApiService.class);
    }


}
