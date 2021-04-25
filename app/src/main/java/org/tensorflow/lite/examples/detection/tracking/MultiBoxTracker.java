/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.lite.examples.detection.tracking;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.tensorflow.lite.examples.detection.CameraActivity;
import org.tensorflow.lite.examples.detection.DetectorActivity;
import org.tensorflow.lite.examples.detection.R;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;

import sensor_msgs.Image;
import std_msgs.Float32MultiArray;
import tflite.Detector;
import tflite.Detector.Recognition;

import static java.util.Collections.emptyList;

/** A tracker that handles non-max suppression and matches existing objects to new detections. */
public class MultiBoxTracker {

  private static final float TEXT_SIZE_DIP = 18;
  private static final float MIN_SIZE = 16.0f;
  private static final int[] COLORS = {
    Color.BLUE,
    Color.RED,
    Color.GREEN,
    Color.YELLOW,
    Color.CYAN,
    Color.MAGENTA,
    Color.WHITE,
    Color.parseColor("#55FF55"),
    Color.parseColor("#FFA500"),
    Color.parseColor("#FF8888"),
    Color.parseColor("#AAAAFF"),
    Color.parseColor("#FFFFAA"),
    Color.parseColor("#55AAAA"),
    Color.parseColor("#AA33AA"),
    Color.parseColor("#0D0068")
  };
  final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();
  private final Logger logger = new Logger();
  private final Queue<Integer> availableColors = new LinkedList<Integer>();
  private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
  private final Paint boxPaint = new Paint();
  private final float textSizePx;
  private final BorderedText borderedText;
  private Matrix frameToCanvasMatrix;
  private int frameWidth;
  private int frameHeight;
  private int sensorOrientation;
  private Context context;
  public MultiBoxTracker(final Context context) {
    for (final int color : COLORS) {
      availableColors.add(color);
    }

    boxPaint.setColor(Color.RED);
    boxPaint.setStyle(Style.STROKE);
    boxPaint.setStrokeWidth(10.0f);
    boxPaint.setStrokeCap(Cap.ROUND);
    boxPaint.setStrokeJoin(Join.ROUND);
    boxPaint.setStrokeMiter(100);

    textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    this.context = context;
  }

  public synchronized void setFrameConfiguration(
      final int width, final int height, final int sensorOrientation) {
    frameWidth = width;
    frameHeight = height;
    this.sensorOrientation = sensorOrientation;
  }


  public synchronized void drawDebug(final Canvas canvas) {
    final Paint textPaint = new Paint();
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(60.0f);

    final Paint boxPaint = new Paint();
    boxPaint.setColor(Color.RED);
    boxPaint.setAlpha(200);
    boxPaint.setStyle(Style.STROKE);

    for (final Pair<Float, RectF> detection : screenRects) {
      final RectF rect = detection.second;
      canvas.drawRect(rect, boxPaint);
      canvas.drawText("" + detection.first, rect.left, rect.top, textPaint);
      borderedText.drawText(canvas, rect.centerX(), rect.centerY(), "" + detection.first);
    }
  }

  public synchronized void trackResults(final List<Recognition> results, final long timestamp) {
    logger.i("Processing %d results from %d", results.size(), timestamp);
    processResults(results);
  }

  private Matrix getFrameToCanvasMatrix() {
    return frameToCanvasMatrix;
  }

  public synchronized void draw(final Canvas canvas) {
    final boolean rotated = sensorOrientation % 180 == 90;

    final float multiplier =
        Math.min(
            canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
            canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
    frameToCanvasMatrix =
        ImageUtils.getTransformationMatrix(
            frameWidth,
            frameHeight,
            (int) (multiplier * (rotated ? frameHeight : frameWidth)),
            (int) (multiplier * (rotated ? frameWidth : frameHeight)),
            sensorOrientation,
            false);
    for (final TrackedRecognition recognition : trackedObjects) {
      final RectF trackedPos = new RectF(recognition.location);

      getFrameToCanvasMatrix().mapRect(trackedPos);
      boxPaint.setColor(recognition.color);

      float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
      canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);
      double desiredDistance;
      final String labelString;
      if(CameraActivity.masterConnection.getConnectedOrNot()) {
        Float32MultiArray distance = CameraActivity.masterConnection.getData();
        Log.i("DISTANCE", "The distance is: " + distance.getData()[200000]);
        desiredDistance = distance.getData()[(int) (1280*recognition.location.centerY() + recognition.location.centerX())];

      } else{
        desiredDistance = -1;
      }
      Log.i("SEG", "Width: " + CameraActivity.segmentation.getWidth());
      Log.i("SEG", "Height: " + CameraActivity.segmentation.getHeight());
     // collisionDetection(CameraActivity.carSpeed, desiredDistance, CameraActivity.segmentation,(int) recognition.location.centerX(),
     //        (int)  recognition.location.centerY());



          if (!TextUtils.isEmpty(recognition.title))
            labelString =  String.format("%s %.2f Dist: %.2f", recognition.title, (100 * recognition.detectionConfidence), desiredDistance);
           // labelString =  String.format("%s %.2f D: %.2f", recognition.title, (100 * recognition.detectionConfidence));
          else{
            labelString = String.format("%.2f D: %.2f", (100 * recognition.detectionConfidence), desiredDistance);
           //labelString = String.format("%.2f ", (100 * recognition.detectionConfidence));
          }
      //            borderedText.drawText(canvas, trackedPos.left + cornerSize, trackedPos.top,
      // labelString);
     borderedText.drawText(
          canvas, trackedPos.left + cornerSize, trackedPos.top, labelString , boxPaint);
     // borderedText.drawText(
     //         canvas, recognition.location.centerX(), recognition.location.centerY(), labelString , boxPaint);
    }
  }

 /* private float calculateAbsoluteDistance(int[] imageDepth, int X, int Y){
    int width = 256;
    int height = 256;
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    for (int ii = 0; ii < width; ii++) //pass the screen pixels in 2 directions
    {
      for (int jj = 0; jj < height; jj++) {
        //int val = img_normalized[ii + jj * width];
        int index = (width - ii - 1) + (height - jj - 1) * width;
        if(index < imageDepth.length) {
          int val = imageDepth[index];
          bitmap.setPixel(ii, jj, Color.rgb(val, val, val));
        }
      }
    }

    Bitmap resized = Bitmap.createScaledBitmap(bitmap, 640, 480, true);
    float[] ab = getParameters();

    if(Y < 256 && X < 256 && X > 0 && Y > 0) {
      int disparity = (int) Math.cbrt(Math.abs((resized.getPixel(X, Y))));
      float inverseDepth = (float) (2.64 * disparity + 249.57);
      float inverseDistance = (float) (0.00203 * DetectorActivity.img_array_depth[Y*256 + X] - 1.0906);
      return 1 / inverseDistance;
    } else {
      return 0;
    }

     }*/

  /*private float[] getParameters(){
    float[] ab = new float[2];
    if(DetectorActivity.image_normalized[3000] ==  DetectorActivity.image_normalized[2500]) {
      ab[0] = (DetectorActivity.img_array_depth[3000] - DetectorActivity.img_array_depth[1000]) / (DetectorActivity.image_normalized[3000] - DetectorActivity.image_normalized[1000]);
    }
    else if(DetectorActivity.image_normalized[3000] >  DetectorActivity.image_normalized[2500]) {
       ab[0] = (DetectorActivity.img_array_depth[3000] - DetectorActivity.img_array_depth[2500]) / (DetectorActivity.image_normalized[3000] - DetectorActivity.image_normalized[2500]);
    } else if (DetectorActivity.image_normalized[3000] <  DetectorActivity.image_normalized[2500]){
      ab[0] = (DetectorActivity.img_array_depth[2500] - DetectorActivity.img_array_depth[3000]) / (DetectorActivity.image_normalized[2500] - DetectorActivity.image_normalized[3000]);
    }

    ab[1] = DetectorActivity.img_array_depth[3000] - ab[0] * DetectorActivity.image_normalized[3000];
    return ab;
  } */


  private void collisionDetection(float speed, Double distance, Bitmap bitmap, int centerX, int centerY){
    int inOntheRoad = onRoad(bitmap, centerX, centerY);

    if(speed > 0 && !distance.isNaN() && !distance.isInfinite() && distance != -1){
      float time = (float) (distance / speed);
      if(time < 5 && inOntheRoad == 1) {
        warning();
      }
    }
  }
  private void warning(){
    MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.buzzer);
    mediaPlayer.start(); // no need to call prepare(); create() does that for you
  }



  private TensorOperator getPreprocessNormalizeOp() {
    return new NormalizeOp(115,58.0f);
  }

  private int onRoad (Bitmap bitmap, int centerX, int centerY){
    List<Integer> borders = new ArrayList<>();
    int width = 1194;
    int road;
    int count;
    if(bitmap.getPixel(0, centerY)== Color.rgb(0, 0, 0)){
       road = 0;
       count = 0;
    }
	else{
       road = 1;
       count = 1;
       borders.add(0, 0);
    }


    //int center = centerY*width+centerX;

    for(int i = 0; i < width; i++){
      Log.i("FOR_LOOP", "i is: " + i);
      Log.i("FOR_LOOP", "centerY is: " + centerY);
      if (bitmap.getPixel(i, centerY) == Color.rgb(255, 0, 0) && road == 0){
        road = 1;
      //  borders[count]= i;
        borders.add(count, i);
        count++;
      }
      if (bitmap.getPixel(i, centerY) == Color.rgb(0, 0, 0) && road == 1){
        road = 0;
       // borders[count]= i;
        borders.add(count, i);
        count++;
      }
    }

    int boxBorderLeft = -1;
    int boxBorderRight= -1;


    int j = 0;
    while(j < borders.size()){
      //if(borders[j] < centerX){
      if(borders.get(j) < centerX){
        boxBorderLeft = j;
      }
      //if(borders[j]> centerX){
      if(borders.get(j) > centerX){
        boxBorderRight = j;
        break;
      }
      j++;
    }

    if(boxBorderLeft !=-1 && boxBorderRight !=-1){
      if(boxBorderLeft%2!=0 && boxBorderRight%2==0){		//odd border ==> from white to black; even border ==> from black to white
        return 1;
      }
      else{
        return 0;
      }
    }
    else {
      return 0;
    }
  }






  private void processResults(final List<Recognition> results) {
    final List<Pair<Float, Recognition>> rectsToTrack = new LinkedList<Pair<Float, Recognition>>();

    screenRects.clear();
    final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

    for (final Recognition result : results) {
      if (result.getLocation() == null) {
        continue;
      }
      final RectF detectionFrameRect = new RectF(result.getLocation());

      final RectF detectionScreenRect = new RectF();
      rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

      logger.v(
          "Result! Frame: " + result.getLocation() + " mapped to screen:" + detectionScreenRect);

      screenRects.add(new Pair<Float, RectF>(result.getConfidence(), detectionScreenRect));

      if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
        logger.w("Degenerate rectangle! " + detectionFrameRect);
        continue;
      }

      rectsToTrack.add(new Pair<Float, Recognition>(result.getConfidence(), result));
    }

    trackedObjects.clear();
    if (rectsToTrack.isEmpty()) {
      logger.v("Nothing to track, aborting.");
      return;
    }

    for (final Pair<Float, Recognition> potential : rectsToTrack) {
      final TrackedRecognition trackedRecognition = new TrackedRecognition();
      trackedRecognition.detectionConfidence = potential.first;
      trackedRecognition.location = new RectF(potential.second.getLocation());
      trackedRecognition.title = potential.second.getTitle();
      trackedRecognition.color = COLORS[trackedObjects.size()];
      trackedObjects.add(trackedRecognition);

      if (trackedObjects.size() >= COLORS.length) {
        break;
      }
    }
  }

  private static class TrackedRecognition {
    RectF location;
    float detectionConfidence;
    int color;
    String title;
  }
}
