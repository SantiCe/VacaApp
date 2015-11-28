package com.example.keinsfield.vacapp.Mundo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.Display;

import com.example.keinsfield.vacapp.ImageMatcher.Scene;
import com.google.android.gms.maps.model.LatLng;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.valueOf;

/**
 * Created by Galapagos on 11/09/2015.
 */
public class Utilities {
    public static final LatLng ubate = new LatLng(5.315846, -73.820219);
    private static TessBaseAPI tessBaseAPI = new TessBaseAPI();
    static{
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "1234567890");
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-qwertyuiop[]}{POIU" +
                "YTREWQasdASDfghFGHjklJKLl;L:'\"\\|~`xcvXCVbnmBNM,./<>?");
        Log.d("SC","Created tessbaseapi");
    }

    public static void wakey(){
        Log.d("SC","Utilities wakey.");
    }
    public static void PrintShit(Activity caller){
        File file = Environment.getExternalStorageDirectory();
        Log.d("SC", valueOf(file));
        if(file.listFiles() != null)
        for(File f:file.listFiles()){
            Log.d("SC", String.valueOf(f));
        }
        Log.d("SC","Now with public external storage.");
        file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        Log.d("SC", valueOf(file));
        if(file.listFiles() != null)
        for(File f:file.listFiles()) Log.d("SC", String.valueOf(f));
        Log.d("SC","External files dir");
        file = caller.getExternalFilesDir(null);
        Log.d("SC", valueOf(file));
        file = caller.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        Log.d("SC", valueOf(file));
    }

    public static void GalleryAddPics(String fpath, Activity caller) {
        MediaScannerConnection.scanFile(caller, new String[] { fpath }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    /**
     *
     * @return The external storage directory where the App is saving its pictures.
     * If the returned value is null, there is no external storage available for R/W.
     */
    public static File GetStorageDirectory(Activity caller, boolean temp){
        try{
            // Intente primero hacer un directorio publico en la memoria externa.
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state) && !temp){

                File album = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"VACAPP_Pictures");
                if(!album.mkdirs() && !album.isDirectory() && !album.exists()){
                    // If directory can't be created in public external storage, try private external storage.
                    album = caller.getExternalFilesDir(null);
                    album = new File(album,"VACAPP_Pictures");
                    if(!album.mkdirs() && !album.isDirectory() && !album.exists()){
                        File file = caller.getDir("VACAPP", Context.MODE_PRIVATE);
                        if(!file.exists()) file.mkdirs();
                        return file;
                    }
                }
                return album;
            }
            else{
                File file = caller.getDir("VACAPP", Context.MODE_PRIVATE);
                if(temp) file = new File(file,"temp");
                else file = new File(file,"pictures");
                if(!file.exists() || !file.isDirectory()) file.mkdirs();
                return file;
            }
        }
        catch(Exception e){
            Log.d("SC", e.getMessage());
            throw e;
        }
    }

    public static File GetStorageDirectory(Activity caller){
        return GetStorageDirectory(caller,false);
    }

    public static ArrayList<String> GetFarms(Activity caller){
        ArrayList<String> ret = new ArrayList<>();
        File root = GetStorageDirectory(caller);
        if(root == null) return ret;
        else{

            File[] files = root.listFiles();
            for(File file:files){
                if(file.isDirectory()){
                    ret.add(file.getName());
                }
            }
            //Look for external farms.
            HashMap<CowKey,Cow> map = CowPersistenceManager.getCows(caller);
            for(CowKey key:map.keySet()){
                File file = new File(root,key.farm);
                if(!file.exists() && file.mkdir()) ret.add(key.farm);
            }

            for(String s:ret){
                File info = new File(root,s);
                if(!info.exists() && !info.isDirectory()) continue;
                info = new File(info,"info.txt");
                if(!info.exists()){
                    try{
                        Log.d("SC","Setting location for "+s);
                        PrintWriter pw = new PrintWriter(info);
                        pw.println(ubate.latitude+" "+ubate.longitude);
                        pw.close();
                    }
                    catch(Exception e){

                    }
                }
            }
            return ret;
        }
    }

    public static Location getFarmLocation(String farm, Activity caller){

        try {
            List<String> farms = GetFarms(caller);
            if (!farms.contains(farm)) return null;
            else{
                File root = GetStorageDirectory(caller);
                for (String f : farms) {
                    if (farm.equals(f)) {
                        File fileFarm = new File(root, f);
                        fileFarm = new File(fileFarm,"info.txt");
                        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileFarm)));
                        String[] sp = br.readLine().split(" ");
                        double lat = Double.parseDouble(sp[0]);
                        double lon = Double.parseDouble(sp[1]);
                        Location loc = new Location(farm);
                        loc.setLatitude(lat);
                        loc.setLongitude(lon);
                        return loc;
                    }
                }
            }
        }
        catch(Exception e){
            Log.d("SC","Failed to retrieve farm location info for: "+farm);
        }
        return null;
    }
    //Returns all vacapp images in the phone storage, organized by farm.
    public static HashMap<String,ArrayList<File>> getAllVacappImages(Activity caller){
        File root = Utilities.GetStorageDirectory(caller);
        ArrayList<String> farmList = Utilities.GetFarms(caller);
        HashMap<String,ArrayList<File>> files = new HashMap<>();
        Log.d("SC","Entering farms loop.");
        for(String s:farmList){
            //Log.d("SC",s);
            File farmDir = new File(root,s);
            if(!farmDir.isDirectory()) continue;
            File[] flist = farmDir.listFiles();
            if(flist == null)continue;
            ArrayList<File> pics = new ArrayList<File>();
            for(File f: flist){
                try {
                    Log.d("SC", "Looking at item. " + f.toString());
                    if (Utilities.isVacappImage(f)) {
                        pics.add(f);
                    }
                }
                catch(Exception e){
                    Log.d("SC", "Failed with: "+f.toString());
                }
            }
            files.put(s,pics);
        }
        return files;
    }

    public static void showDialog(String title, String message,Activity caller) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        };
        showDialog(title,message,caller,listener);
    }

    public static void showDialog(String title, String message, Activity caller, DialogInterface.OnClickListener listener){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(caller);
        alertDialog.setTitle(title);
        alertDialog.setCancelable(false);
        alertDialog.setMessage(message);

        alertDialog.setPositiveButton("OK", listener);
        alertDialog.show();
    }

    public static void showYesNoDialog(String title, String message, Activity caller, DialogInterface.OnClickListener trueListener, String yesMsg, DialogInterface.OnClickListener falseListener, String noMsg){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(caller);
        alertDialog.setTitle(title);
        alertDialog.setCancelable(false);
        alertDialog.setMessage(message);

        alertDialog.setPositiveButton(yesMsg, trueListener);
        alertDialog.setNegativeButton(noMsg, falseListener);
        alertDialog.show();
    }

    public static boolean isVacappFilename(String name){
        name = name.toLowerCase();
        if(!(name.endsWith(".png") || name.endsWith(".jpeg") || name.endsWith(".numberBmp") || name.endsWith(".jpg")))return false;
        try{
            String prefix = name.split("\\.")[0];
            Integer.parseInt(prefix);
            return true;
        }
        catch(Exception e){
            return false;
        }
    }
    public static boolean isVacappImage(File file){
        String name = file.getName();
        return isVacappFilename(name);
    }

    public static Mat bitmapToMat(Bitmap bmp){
        Mat ret = new Mat(bmp.getWidth(), bmp.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(bmp, ret);
        return ret;
    }

    public static Scene fileToScene (File f){
        return new Scene(bitmapToMat(BitmapFactory.decodeFile(f.toString())));
    }

    public static int getCowNumberFromFile(File file){
        String prefix = file.getName().split("\\.")[0];
        return Integer.parseInt(prefix);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, Activity context, double wRatio, double hRatio) {
        Point point = new Point();
        Display display = context.getWindowManager().getDefaultDisplay();
        display.getSize(point);
        double reqWidth = wRatio*point.y;
        double reqHeight = point.x*hRatio;

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
    /**
     * Usar este método para tomar un archivo con una imagen y devolver un bitmap scaleado.
     * @param file
     * @return
     */
    public static Bitmap fileToBitmap(String file, Activity context, double wRatio, double hRatio){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file,options);
        options.inSampleSize = calculateInSampleSize(options,context,wRatio,hRatio);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file,options);
    }

    /**
     * Usar este método para tomar un archivo con bytes y devolver un bitmap scaleado.
     * @param data
     * @return
     */
    public static Bitmap byteArrayToBitmap(byte[] data, Activity context, double wRatio, double hRatio){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        options.inSampleSize = calculateInSampleSize(options,context, wRatio, hRatio);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    public static void copyFile(InputStream in, OutputStream out) throws Exception{
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
    }

    public static File getCowBD(Activity context){
        File root = GetStorageDirectory(context);
        File look = new File(root,"cowBD.cow");
        return look;
    }
}
