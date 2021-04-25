package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class Speed{
    List<Double> speedlist = new ArrayList();
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Context context;
    Double lastLat;
    Double lastLng;
    Double lastAlt;
    Long lastTime;
    long startTime = 0;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {


            timerHandler.postDelayed(this, 500);
        }
    };

    public Speed(Context context){
        this.context = context;
    }
    private float averagespeed = 0.0f;
    public void setupLocationAndSpeed(){
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Double newLat = location.getLatitude();
                Double newLng = location.getLongitude();
                Double newAlt = location.getAltitude();
                Long newTime = location.getTime();

                Float accuracy = location.getAccuracy();

                if(lastLat == null){
                    startTime = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnable, 0);
                    lastLat = newLat;
                    lastLng = newLng;
                    lastAlt = newAlt;
                    lastTime = newTime;
                } else{
                    Double dist = distance(lastLat, newLat, lastLng, newLng, newAlt, newAlt);
                    float timediff = (newTime - lastTime) / 1000;

                    if(timediff == 0){

                    } else {
                     Double speed = Math.abs(dist) / timediff;
                        speedlist.add(speed);
                        if (speedlist.size() > 5) {
                            speedlist.remove(0);
                            averagespeed = (float) Math.abs(averageSpeed(speedlist));
                            if (averagespeed < 0.5) { averagespeed = 0.0f; }
                        }
                        lastLat = newLat;
                        lastLng = newLng;
                        lastAlt = newAlt;
                        lastTime = newTime;

                    }
                    CameraActivity.carSpeed = averagespeed;
                    Log.i("LOCATION_SPEED", "The location is: " + averagespeed);
                    CameraActivity.showInference(String.valueOf(averagespeed));


                }

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

    }

    public Double distance(double lat1, double lat2, double lon1,
                           double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }
    public Double averageSpeed(List speeds) {
        Double total = 0.0;
        for (int i = 0; i < speeds.size(); i++) {
            total = total + (double)speeds.get(i);
        }
        return (total / speeds.size());
    }



    public void requestUpdates(){
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    public void timerRemoveCallbacks(){
        timerHandler.removeCallbacks(timerRunnable);
    }







}
