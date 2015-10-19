package com.example.keinsfield.vacapp.Activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import com.example.keinsfield.vacapp.Camera.CameraEngine;
import com.example.keinsfield.vacapp.Images.Tools;
import com.example.keinsfield.vacapp.Mundo.Utilities;
import com.example.keinsfield.vacapp.OCR.Tesseract;
import com.example.keinsfield.vacapp.R;
import com.example.keinsfield.vacapp.Views.FocusBoxView;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.Boolean;import java.lang.Exception;import java.lang.Integer;import java.lang.Override;import java.lang.String;

/**
 * GUI by PJSolutions
 * Image matching by Mustafa Akin
 * Camera Engine by Fadi
 * Icons from: thenounproject
 */
public class MainActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener, Camera.PictureCallback, Camera.ShutterCallback {


    static final String TAG = "DBG_" + MainActivity.class.getName();

    static{ System.loadLibrary("opencv_java");
    Log.d("SC","OPENCV IMPORTED");}

    Button shutterButton;
    Button addCow;
    Button recogCow;
    Button gDriveButton;
    FocusBoxView focusBox;
    SurfaceView cameraFrame;
    CameraEngine cameraEngine;
    Boolean photoTaken;
    SurfaceHolder surfaceHolder;
    Bitmap numberBmp;
    File fullPicFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utilities.wakey();
        setContentView(R.layout.activity_main);
        cameraFrame = (SurfaceView) findViewById(R.id.camera_frame);
        shutterButton = (Button) findViewById(R.id.shutter_button);
        shutterButton.setOnClickListener(this);
        gDriveButton = (Button) findViewById(R.id.gdrive_button);
        gDriveButton.setOnClickListener(this);
        addCow = (Button) findViewById(R.id.add_button);
        addCow.setOnClickListener(this);
        recogCow = (Button) findViewById(R.id.recog_button);
        recogCow.setOnClickListener(this);
        focusBox = (FocusBoxView) findViewById(R.id.focus_box);
        photoTaken = false;
        surfaceHolder = cameraFrame.getHolder();

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.d(TAG, "Surface Created - starting camera");

        if (cameraEngine != null && !cameraEngine.isOn()) {
            cameraEngine.start();
        }

        if (cameraEngine != null && cameraEngine.isOn()) {
            Log.d(TAG, "Camera engine already on");
            return;
        }

        cameraEngine = CameraEngine.New(holder);
        cameraEngine.start();

        Log.d(TAG, "Camera engine started");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed");


        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        cameraFrame.setOnClickListener(this);

        if(cameraEngine!= null)
            cameraEngine.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "Paused");
        if (cameraEngine != null && cameraEngine.isOn()) {
            cameraEngine.stop();
        }
        SurfaceHolder surfaceHolder = cameraFrame.getHolder();
        surfaceHolder.removeCallback(this);
    }

    @Override
    public void onClick(View v) {
        if (v == shutterButton) {
            if (cameraEngine != null && cameraEngine.isOn()) {
                if(!photoTaken) {
                    cameraEngine.takeShot(this, this, this);
                    shutterButton.setBackgroundResource(R.drawable.back);
                    photoTaken = true;
                }
                else{
                    shutterButton.setBackgroundResource(R.drawable.camera);
                    photoTaken = false;
                    cameraEngine.restartPreview();
                }

            }
        }
        else if(v == addCow)
        {
            if(!photoTaken || fullPicFile == null){
                Utilities.showDialog("Error","Debe tomar una foto antes de agregar una vaca al sistema.",this);
                return;
            }
            Intent intent = new Intent(this,AddCowActivity.class);
            String number = new Tesseract(this).getText(numberBmp);

            int sel = -1;
            try{
                sel = Integer.parseInt(number);
            }
            catch(Exception e){}
            intent.putExtra("photo",fullPicFile.toString());
            intent.putExtra("number",sel);
            intent.putExtra("caller","main");
            startActivity(intent);
        }
        else if(v == recogCow){
            if(!photoTaken || fullPicFile == null){
                Utilities.showDialog("Error","Debe tomar una foto para empezar el reconocimiento.",this);
                return;
            }
            Intent intent = new Intent(this,RecognizeCowActivity.class);
            intent.putExtra("photo",fullPicFile.toString());
            intent.putExtra("caller","main");
            startActivity(intent);
            finish();
        }

        else if (v == gDriveButton){
            Intent intent = new Intent(this,GDriveImportExportActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        Log.d(TAG, "Picture taken");

        if (data == null) {
            Log.d(TAG, "Got null data");
            return;
        }

        if(focusBox.getBox()==null)
            Log.d(TAG, "NULL");
        numberBmp = Tools.getFocusedBitmap(this, camera, data, focusBox.getBox());
        File dir = Utilities.GetStorageDirectory(this,true);
        fullPicFile = new File(dir,"tmpimg");
        if(fullPicFile.exists()) fullPicFile.delete();
        boolean success = false;

        // Save bytearray to temp file
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(fullPicFile,false);
            outStream.write(data);
        /* 100 to keep full quality of the numberImage */

            outStream.flush();
            outStream.close();
            Log.d("SC","Saved temp file: "+fullPicFile.toString());
        } catch (Exception e) {
            Utilities.showDialog("Error","Hubo un error guardando los datos de la foto.",this);
            fullPicFile = null;
            e.printStackTrace();
        }
    }

    @Override
    public void onShutter() {

    }

}
