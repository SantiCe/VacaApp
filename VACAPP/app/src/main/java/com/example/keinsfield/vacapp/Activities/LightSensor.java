package com.example.keinsfield.vacapp.Activities;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
/**
 * Created by Santiago on 29/11/2015.
 */
public class LightSensor {
        public double light;
        public static final double FLASH_THOLD = 80.0;
        public LightSensor(MainActivity caller) {

            SensorManager mySensorManager = (SensorManager)(caller.getSystemService(caller.SENSOR_SERVICE));

            Sensor LightSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if(LightSensor != null){
                Log.d("SCLight","Light sensor detected and available.");
                mySensorManager.registerListener(
                        LightSensorListener,
                        LightSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);

            }else{
                Log.d("SCLight","No light sensor available.");
            }
        }

        private final SensorEventListener LightSensorListener
                = new SensorEventListener(){

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                if(event.sensor.getType() == Sensor.TYPE_LIGHT){
                    //Log.d("SCLight:","LIGHT: " + (light = event.values[0]));
                    light = event.values[0];
                }
            }

        };


}
