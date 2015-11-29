package com.example.keinsfield.vacapp.Camera;

/**
 * Created by Keinsfield on 11-Sep-15.
 */

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

/**
 * Created by Fadi on 5/11/2014.
 */
public class CameraEngine {

    static final String TAG = "DBG_";//+ CameraUtils.class.getName();

    boolean on;
    Camera camera;
    SurfaceHolder surfaceHolder;


    private CameraEngine(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
    }

    static public CameraEngine New(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "Creating camera engine");
        return new CameraEngine(surfaceHolder);
    }

    public static Camera getCamera() {
        try {
            return Camera.open();
        } catch (Exception e) {
            Log.e(TAG, "Cannot getCamera()");
            return null;
        }
    }

    public boolean isOn() {
        return on;
    }

    public void restartPreview() {
        this.camera.stopPreview();
        this.camera.startPreview();
    }

    public void start() {

        Log.d(TAG, "Entered CameraEngine - start()");
        this.camera = getCamera();

        if (this.camera == null)
            return;

        Log.d(TAG, "Got camera hardware");

        try {

            Camera.Parameters params = this.camera.getParameters();
            params.setJpegQuality(70);
            params.setPictureFormat(ImageFormat.JPEG);
            List<Camera.Size> sizes = params.getSupportedPictureSizes();
            Camera.Size size = sizes.get(Integer.valueOf((sizes.size() - 1) / 2)); //choose a medium resolution
            params.setPictureSize(size.width, size.height);
            camera.setParameters(params);
            if(params.getSupportedFocusModes() != null && params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            else{
                Log.d("SC","Camera does not support ContinuesPicture.");
            }
            this.camera.setParameters(params);
            this.camera.setPreviewDisplay(this.surfaceHolder);
            this.camera.startPreview();

            on = true;

            Log.d(TAG, "CameraEngine preview started");

        } catch (IOException e) {
            Log.e(TAG, "Error in setPreviewDisplay");
        }
    }

    public void stop() {

        if (camera != null) {
            //this.autoFocusEngine.stop();
            camera.release();
            camera = null;
        }

        on = false;

        Log.d(TAG, "CameraEngine Stopped");
    }

    public void takeShot(Camera.ShutterCallback shutterCallback,
                         Camera.PictureCallback rawPictureCallback,
                         Camera.PictureCallback jpegPictureCallback) {
        if (isOn()) {
            camera.takePicture(shutterCallback, rawPictureCallback, jpegPictureCallback);
        }
    }

    public void takeShotWithFlash(Camera.ShutterCallback shutterCallback,
                                  Camera.PictureCallback rawPictureCallback,
                                  Camera.PictureCallback jpegPictureCallback){
        if(!isOn()) return;
        Log.d("SC","Taking pic with flash.");
        Camera.Parameters params = camera.getParameters();
        if(params.getSupportedSceneModes() != null && params.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_NIGHT))
            params.setSceneMode(Camera.Parameters.SCENE_MODE_NIGHT);
        if(params.getSupportedFlashModes() != null && params.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_ON))
            params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
        camera.setParameters(params);
        takeShot(shutterCallback,
                rawPictureCallback,
                jpegPictureCallback);
        if(params.getSupportedSceneModes() != null && params.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_AUTO))
            params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
        if(params.getSupportedFlashModes() != null && params.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_OFF))
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
    }

}
