/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.camera2.CameraCharacteristics;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import com.common.thermalimage.HotImageCallback;
import com.common.thermalimage.TemperatureBitmapData;
import com.common.thermalimage.TemperatureData;
import com.common.thermalimage.ThermalImageUtil;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.tensorflow.lite.examples.detection.Api.ApiService;
import org.tensorflow.lite.examples.detection.Api.Server;
import org.tensorflow.lite.examples.detection.Constant.Constant;
import org.tensorflow.lite.examples.detection.Interface.customCallback;
import org.tensorflow.lite.examples.detection.Model.Json.JsonInfran;
import org.tensorflow.lite.examples.detection.Model.Response.ResponseDataInfran;
import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.tensorflow.lite.examples.detection.Helper.FLIP_HORIZONTAL;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();

  // Configuration values for the prepackaged SSD model.
  //private static final int TF_OD_API_INPUT_SIZE = 300;
  //private static final boolean TF_OD_API_IS_QUANTIZED = true;
  //private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
  //private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

  //private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

  // Face Mask
  private static final int TF_OD_API_INPUT_SIZE = 224;
  private static final boolean TF_OD_API_IS_QUANTIZED = false;
  private static final String TF_OD_API_MODEL_FILE = "mask_detector.tflite";
  private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/mask_labelmap.txt";

  private static final DetectorMode MODE = DetectorMode.TF_OD_API;
  // Minimum detection confidence to track a detection.
  private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
  private static final boolean MAINTAIN_ASPECT = false;

  public static String temperatureValue = "";

  private String TAG = "TAG";

  public static String INFRAN_NAME = "";
  public static String INFRAN_Message = "";

  private static final Size DESIRED_PREVIEW_SIZE = new Size(800, 600);
  //private static final int CROP_SIZE = 320;
  //private static final Size CROP_SIZE = new Size(320, 320);

  private ApiService API;
  private Context mContext ;
  public List<JsonInfran> EntityInfran = new ArrayList<>();

  ThermalImageUtil temperatureUtil;
  private static final boolean SAVE_PREVIEW_BITMAP = false;
  private static final float TEXT_SIZE_DIP = 10;
  OverlayView trackingOverlay;
  private Integer sensorOrientation;

  private Classifier detector;

  private long lastProcessingTimeMs;
  private Bitmap rgbFrameBitmap = null;
  private Bitmap croppedBitmap = null;
  private Bitmap cropCopyBitmap = null;

  private boolean computingDetection = false;

  private long timestamp = 0;

  private Matrix frameToCropTransform;
  private Matrix cropToFrameTransform;

  private MultiBoxTracker tracker;

  private BorderedText borderedText;

  // Face detector
  private FaceDetector faceDetector;

  // here the preview image is drawn in portrait way
  private Bitmap portraitBmp = null;
  // here the face is cropped and drawn
  private Bitmap faceBmp = null;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    API = Server.getAPIServiceInfran();
    mContext = this ;
    temperatureUtil = new ThermalImageUtil(DetectorActivity.this);

    // Real-time contour detection of multiple faces
    FaceDetectorOptions options =
            new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build();


    FaceDetector detector = FaceDetection.getClient(options);

    faceDetector = detector;


    //checkWritePermission();

  }

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
            TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    tracker = new MultiBoxTracker(this);


    try {
      detector =
              TFLiteObjectDetectionAPIModel.create(
                      getAssets(),
                      TF_OD_API_MODEL_FILE,
                      TF_OD_API_LABELS_FILE,
                      TF_OD_API_INPUT_SIZE,
                      TF_OD_API_IS_QUANTIZED);
      //cropSize = TF_OD_API_INPUT_SIZE;
    } catch (final IOException e) {
      e.printStackTrace();
      LOGGER.e(e, "Exception initializing classifier!");
      Toast toast =
              Toast.makeText(
                      getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
      toast.show();
      finish();
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    int screenOrientation = getScreenOrientation();
    sensorOrientation = rotation - screenOrientation;
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);




    int targetW, targetH;
    if (sensorOrientation == 90 || sensorOrientation == 270) {
      targetH = previewWidth;
      targetW = previewHeight;
    }
    else {
      targetW = previewWidth;
      targetH = previewHeight;
    }
    int cropW = (int) (targetW / 2.0);
    int cropH = (int) (targetH / 2.0);

    croppedBitmap = Bitmap.createBitmap(cropW, cropH, Config.ARGB_8888);

    portraitBmp = Bitmap.createBitmap(targetW, targetH, Config.ARGB_8888);
    faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Config.ARGB_8888);

    frameToCropTransform =
            ImageUtils.getTransformationMatrix(
                    previewWidth, previewHeight,
                    cropW, cropH,
                    sensorOrientation, MAINTAIN_ASPECT);

//    frameToCropTransform =
//            ImageUtils.getTransformationMatrix(
//                    previewWidth, previewHeight,
//                    previewWidth, previewHeight,
//                    sensorOrientation, MAINTAIN_ASPECT);

    cropToFrameTransform = new Matrix();
    frameToCropTransform.invert(cropToFrameTransform);



    trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
    trackingOverlay.addCallback(
            new DrawCallback() {
              @Override
              public void drawCallback(final Canvas canvas) {
                tracker.draw(canvas);
                if (isDebug()) {
                  tracker.drawDebug(canvas);
                }
              }
            });

    tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
  }


  @Override
  protected void processImage() {
    ++timestamp;
    final long currTimestamp = timestamp;
    trackingOverlay.postInvalidate();

    // No mutex needed as this method is not reentrant.
    if (computingDetection) {
      readyForNextImage();
      return;
    }
    computingDetection = true;
    LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

    readyForNextImage();

    final Canvas canvas = new Canvas(croppedBitmap);
    canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
    // For examining the actual TF input.
    if (SAVE_PREVIEW_BITMAP) {
      ImageUtils.saveBitmap(croppedBitmap);
    }

    InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
    faceDetector
            .process(image)
            .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
              @Override
              public void onSuccess(List<Face> faces) {
                if (faces.size() == 0) {
                  updateResults(currTimestamp, new LinkedList<>());

                  return;
                }



                Log.e(TAG, "onSuccess: faces size"+faces.size() );
                runInBackground(
                        new Runnable() {
                          @Override
                          public void run() {
                            onFacesDetected(currTimestamp, faces);


                          }
                        });
              }

            });


  }

  @Override
  protected int getLayoutId() {
    return R.layout.tfe_od_camera_connection_fragment_tracking;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  // Which detection model to use: by default uses Tensorflow Object Detection API frozen
  // checkpoints.
  private enum DetectorMode {
    TF_OD_API;
  }

  @Override
  protected void setUseNNAPI(final boolean isChecked) {
    runInBackground(() -> detector.setUseNNAPI(isChecked));
  }

  @Override
  protected void setNumThreads(final int numThreads) {
    runInBackground(() -> detector.setNumThreads(numThreads));
  }


  // Face Mask Processing
  private Matrix createTransform(
          final int srcWidth,
          final int srcHeight,
          final int dstWidth,
          final int dstHeight,
          final int applyRotation) {

    Matrix matrix = new Matrix();
    if (applyRotation != 0) {
      if (applyRotation % 90 != 0) {
        LOGGER.w("Rotation of %d % 90 != 0", applyRotation);
      }

      // Translate so center of image is at origin.
      matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

      // Rotate around origin.
      matrix.postRotate(applyRotation);
    }

//        // Account for the already applied rotation, if any, and then determine how
//        // much scaling is needed for each axis.
//        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;
//
//        final int inWidth = transpose ? srcHeight : srcWidth;
//        final int inHeight = transpose ? srcWidth : srcHeight;

    if (applyRotation != 0) {

      // Translate back from origin centered reference to destination frame.
      matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
    }

    return matrix;

  }

  private void updateResults(long currTimestamp, final List<Classifier.Recognition> mappedRecognitions) {

    tracker.trackResults(mappedRecognitions, currTimestamp);
    trackingOverlay.postInvalidate();
    computingDetection = false;


    runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                showFrameInfo(previewWidth + "x" + previewHeight);
                showCropInfo(croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());
                showInference(lastProcessingTimeMs + "ms");
              }
            });

  }

  public void onFacesDetected(long currTimestamp, List<Face> faces ) {




        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);

        final Canvas canvas = new Canvas(cropCopyBitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(1.0f);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        switch (MODE) {
          case TF_OD_API:
            minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
            break;
        }

        final List<Classifier.Recognition> mappedRecognitions =
                new LinkedList<Classifier.Recognition>();


        //final List<Classifier.Recognition> results = new ArrayList<>();

        // Note this can be done only once
        int sourceW = rgbFrameBitmap.getWidth();
        int sourceH = rgbFrameBitmap.getHeight();
        int targetW = portraitBmp.getWidth();
        int targetH = portraitBmp.getHeight();
        Matrix transform = createTransform(
                sourceW,
                sourceH,
                targetW,
                targetH,
                sensorOrientation);
        final Canvas cv = new Canvas(portraitBmp);

        // draws the original image in portrait mode.
        cv.drawBitmap(rgbFrameBitmap, transform, null);


        final Canvas cvFace = new Canvas(faceBmp);

        boolean saved = false;

        for (int i = 0; i<faces.size(); i++) {


          checkTemp();

          LOGGER.i("FACE" + faces.get(i).toString());
          LOGGER.i("Running detection on face " + currTimestamp);

          //results = detector.recognizeImage(croppedBitmap);

          final RectF boundingBox = new RectF(faces.get(i).getBoundingBox());

          //final boolean goodConfidence = result.getConfidence() >= minimumConfidence;
          final boolean goodConfidence = true; //face.get;
          if (boundingBox != null && goodConfidence) {

            // maps crop coordinates to original
            cropToFrameTransform.mapRect(boundingBox);

            // maps original coordinates to portrait coordinates
            RectF faceBB = new RectF(boundingBox);
            transform.mapRect(faceBB);

            // translates portrait to origin and scales to fit input inference size
            //cv.drawRect(faceBB, paint);
            float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
            float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();


            Matrix matrix = new Matrix();
            matrix.postTranslate(-faceBB.left, -faceBB.top);
            matrix.postScale(sx, sy);

            cvFace.drawBitmap(portraitBmp, matrix, null);

            //Bitmap bmp2 = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Config.ARGB_8888);

            try {
              //Bitmap bmp2 = Bitmap.createBitmap(portraitBmp,Math.round(faceBB.centerX()),Math.round(faceBB.centerY()), TF_OD_API_INPUT_SIZE,TF_OD_API_INPUT_SIZE, matrix, true);
              Bitmap bmpWajah = Bitmap.createBitmap(portraitBmp,Math.round(faceBB.left),Math.round(faceBB.top), TF_OD_API_INPUT_SIZE,TF_OD_API_INPUT_SIZE, matrix, true);



            String data = Helper.getStringImage(Helper.flip(bmpWajah ,FLIP_HORIZONTAL));
            //String data = Helper.getStringImage(faceBmp);
            Helper.writeToFile(data,this);
            String hash = Helper.Hash_SHA256(data);

            requestInfran(data, hash, new customCallback() {
              @Override
              public void onSucess(String name) {

            String label = "";
            float confidence = -1f;
            Integer color = Color.BLUE;

            final long startTime = SystemClock.uptimeMillis();
            final List<Classifier.Recognition> resultsAux = detector.recognizeImage(faceBmp);
            lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

            if (resultsAux.size() > 0) {

              Classifier.Recognition result = resultsAux.get(0);

              float conf = result.getConfidence();
              if (conf >= 0.6f) {

                confidence = conf;

                label =   name +"#"+temperatureValue+"#"+result.getTitle();
                Log.e(TAG, "label "+ name );

                if (result.getId().equals("0")) {
                  color = Color.GREEN;
                }
                else {
                  color = Color.RED;
                }

              }

            }

              // camera is frontal so the image is flipped horizontally
              // flips horizontally
              Matrix flip = new Matrix();
              if (sensorOrientation == 90 || sensorOrientation == 270) {
                flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
              }
              else {
                flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
              }
              //flip.postScale(1, -1, targetW / 2.0f, targetH / 2.0f);
              flip.mapRect(boundingBox);



              final Classifier.Recognition result = new Classifier.Recognition(
                      "0", label, confidence, boundingBox);

              result.setColor(color);
              result.setLocation(boundingBox);
              mappedRecognitions.add(result);

              updateResults(currTimestamp, mappedRecognitions);


              }


            });

            }
            catch(Exception e){
              Log.e(TAG, "onFacesDetected: "+e.getMessage() );
            }


          }


        }

        //    if (saved) {
//      lastSaved = System.currentTimeMillis();
//    }



  }




  private void requestInfran(String data, String hash , customCallback customCallback){

    float threshold  = 0.43f ;
    EntityInfran.clear();
    //Helper.writeToFile(data,this);
    Call<ResponseDataInfran> call = API.requestInfran(new JsonInfran(data,hash,threshold));
    call.enqueue(new Callback<ResponseDataInfran>() {
      @Override
      public void onResponse(Call<ResponseDataInfran> call, Response<ResponseDataInfran> response) {
        if(response.isSuccessful()) {
          if (response.body().getStatus() != null) {

            String err_code = response.body().getErr_code() ;
            String status = response.body().getStatus();

            if(err_code.equals(Constant.ERR_0)){

              String pip = response.body().getData().get(0).getPerson_in_picture();

              //solve nama = unknow
              if (!pip.equals("unknown")){

                try {

                  INFRAN_Message = response.body().getData().get(0).getPerson_in_picture() ;
                  INFRAN_NAME = response.body().getData().get(0).getPerson_in_picture() ;
                  String name = response.body().getData().get(0).getPerson_in_picture() ;
                  customCallback.onSucess(name);

                }catch (Exception ex){
                  Log.e(TAG, "onResponse: "+ ex.getMessage() );
                }

                //sessionManager.saveUserObjectSiswa(Helper.ConvertResponseDataInfranToJson(response.body()));
                //startActivity(new Intent(CameraFaceIdentity.this, paymentSiswa.class));
                //finish();

              }else{
                INFRAN_Message = "Tidak Dikenali" ;
                customCallback.onSucess(pip);
                //INFRAN_NAME = "Tidak Dikenali" ;
              }

            }else{
              INFRAN_Message = status ;
              INFRAN_NAME = "Tidak Dikenali" ;
              customCallback.onSucess(status);
              //Toast.makeText(mContext, status, Toast.LENGTH_SHORT).show();
            }

          }else{
            INFRAN_NAME = "error server" ;
            customCallback.onSucess("error server");

          }

        }else{
          INFRAN_NAME = "error server" ;
          customCallback.onSucess("error server");

        }
      }

      @Override
      public void onFailure(Call<ResponseDataInfran> call, Throwable t) {
        INFRAN_NAME = "error server" ;
        customCallback.onSucess("error server");


      }
    });

  }

  private void checkTemp() {
    float distance=50;
    final float distances = distance;
    new Thread(new Runnable() {
      @Override
      public void run() {

        TemperatureData temperatureData = temperatureUtil.getDataAndBitmap(distances,true, new HotImageCallback.Stub() {
          @Override
          public void onTemperatureFail(String e) {
            Log.i("getDataAndBitmap", "onTemperatureFail " + e);
            // showTip("Failed to get temperature:  " + e, temp);
          }

          @Override
          public void getTemperatureBimapData(final TemperatureBitmapData data) {
            Log.i("tagnya", "getTemperatureBimapData: Buat Ngeluarin gambar bitmapnya");
          }

        });
        if (temperatureData != null) {
          // String text = temperatureData.isUnusualTem()?"Abnormal body temperature!":"Normal body temperature";
          String text = "";
          if(temperatureData.isUnusualTem()){
            text = "Temperature anormaly!";
//                                playAbnormalSound();
          }else {
            text = "Temperature normal";
//                                playNormalSound();
          }
          //showTip(text+"\nTemperature: " + temperatureData.getTemperature()+" ℃", temp);
          temperatureValue ="Temp : "+ temperatureData.getTemperature()+" ℃";


        }
      }
    }).start();
  }





}
