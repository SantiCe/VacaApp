package com.example.keinsfield.vacapp.Activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.keinsfield.vacapp.ImageMatcher.Scene;
import com.example.keinsfield.vacapp.ImageMatcher.SceneDetectData;
import com.example.keinsfield.vacapp.R;

import org.opencv.core.Mat;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.SortedSet;

/**
 * Created by ricardo on 9/13/15.
 */
public class RecognizeCowActivity extends Activity {

    private static String ALL_FARMS;
    public static final int INT_IDENTIFIER = 1;
    private String selectedFarm = ALL_FARMS;
    private ListView listView;
    private ArrayList<PhotoToMatch> files;
    private ProgressDialog progress;
    private String refCowPath;
    private Scene refCow;
    private boolean matched;
    private final static int PROGRESS_ID = 1;

    /**
     * By Mustafa AKIN. https://github.com/mustafaakin/image-matcher
     */

    public static class PhotoToMatch implements Comparable<PhotoToMatch>{
        public Scene scene;
        public File file;
        public int homoMatches;

        public PhotoToMatch(Scene s, File f){
            scene = s;
            file = f;
            homoMatches = 0;
        }

        @Override
        public int compareTo(PhotoToMatch another) {
            return another.homoMatches-homoMatches;
        }
    }

    public void doMatching(){
        for (PhotoToMatch ptm: files) {
            Scene scn = ptm.scene;
            SceneDetectData data = refCow.compare(scn, true, true);
            int currDist = data.homo_matches;
            ptm.homoMatches = currDist;
            progress.incrementProgressBy(1);
        }
        Collections.sort(files);
        matched = true;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog){
        Log.d("SC","Calls on prepare");
        Log.d("SC","creating progress dialog");
        progress.setCancelable(false);
        progress.setMessage("Comparando imagenes. By: Mustafa AKIN Image Matcher - OpenCV");
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setProgress(0);
        progress.setMax(files.size());
        progress.show();
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        Log.d("SC","Calls on create.");
        if(progress == null) progress = new ProgressDialog(this);
        return progress;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognize);
        ALL_FARMS = getString(R.string.all_farms);
        listView = (ListView)findViewById(R.id.farmListView_recognizeActivity);
        AdapterView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.setSelected(true);
                onListViewClick(i);
            }
        };
        listView.setOnItemClickListener(listener);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ArrayList<String> list = Utilities.GetFarms(this);
        list.add(0, ALL_FARMS);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.custom_list_item,list);
        listView.setAdapter(adapter);

        Intent intent = getIntent();
        String auxFarm = intent.getStringExtra("farm");
        int posToSelect = 0;
        boolean shouldBlock = false;
        if(auxFarm != null){
            for(int i = 1; i < list.size(); i ++){
                String s = list.get(i);
                if(s.equals(auxFarm)){
                    posToSelect = i;
                    shouldBlock = true;
                    break;
                }
            }
        }
        if(shouldBlock) onListViewClick(posToSelect);
        try{
            refCowPath = intent.getStringExtra("photo");
            RandomAccessFile f = new RandomAccessFile(refCowPath, "r");
            byte[] data = new byte[(int) f.length()];
            f.read(data);
            refCow = new Scene(Utilities.bitmapToMat(BitmapFactory.decodeByteArray(data, 0, data.length)));
        }
        catch(Exception e){
            Utilities.showDialog("Error","Error procesando imagen de referencia.",this);
            BackToMain(null);
        }
    }

    public void onListViewClick(final int selection){
        if(!listView.isEnabled()) return;
        selectedFarm = (String)listView.getItemAtPosition(selection);
        if(selectedFarm == null) {
            selectedFarm = ALL_FARMS;

            Log.d("SC", "It was null");
        }
        EditText et = (EditText)findViewById(R.id.textSelectedFarm);
        et.setText(selectedFarm);
        listView.setEnabled(false);
    }

    public void onResetClick(View v){
        listView.setEnabled(true);
        selectedFarm = getString(R.string.all_farms);
        listView.setSelection(0);
        files = null;
        matched = false;
    }


    private void BackToMain(View v){
        Intent intent=new Intent(getApplicationContext(),MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        BackToMain(null);
    }

    private void showNoCowsDialog(){
        Utilities.showDialog("No hay vacas","No hay más vacas disponibles para analizar. La finca no tiene vacas, o todas las vacas de la finca ya han sido descartadas como posible match.",this);
    }

    public void onMatchClick(View v){
        File root = Utilities.GetStorageDirectory(this);

        // If files is null then there was no previous matching call made or the reset button was pressed.
        if(files == null){
            matched = false;
            files = new ArrayList<>();
            if(ALL_FARMS.equals(selectedFarm)){
                ArrayList<String> farmList = Utilities.GetFarms(this);
                Log.d("SC","Entering farms loop.");
                for(String s:farmList){
                    Log.d("SC",s);
                    File farmDir = new File(root,s);
                    if(!farmDir.isDirectory()) continue;
                    File[] flist = farmDir.listFiles();
                    if(flist == null)continue;
                    for(File f: flist){
                        try {
                            Log.d("SC", "Looking at item. " + f.toString());
                            if (Utilities.isVacappImage(f)) {
                                files.add(new PhotoToMatch(Utilities.fileToScene(f), f));
                            }
                        }
                        catch(Exception e){
                            Log.d("SC", "Failed with: "+f.toString());
                        }
                    }
                }
            }
            else{
                File farmDir = new File(root,selectedFarm);
                if(!farmDir.isDirectory()) {
                    showNoCowsDialog();
                    BackToMain(null);
                    return;
                }
                File[] flist = farmDir.listFiles();
                if(flist == null){
                    showNoCowsDialog();
                    BackToMain(null);
                    return;
                }
                for(File f: flist){
                    try {
                        Log.d("SC", "Looking at item. " + f.toString());
                        if (Utilities.isVacappImage(f)) {
                            files.add(new PhotoToMatch(Utilities.fileToScene(f), f));
                        }
                    }
                    catch(Exception e){
                        Log.d("SC", "Failed with: "+f.toString());

                    }
                }
            }
        }

        if(files.isEmpty()){
            showNoCowsDialog();
            onResetClick(null);
            return;
        }

        // TODO: MAKE MATCHING
        if(!matched) {
            Log.d("SC", "Starting match with : " + files.size());
            showDialog(PROGRESS_ID);
            Thread t = new Thread(new Runnable()
            {
                public void run()
                {
                    doMatching();
                    progress.dismiss();
                    Intent intent = new Intent(RecognizeCowActivity.this,AcceptMatchActivity.class);
                    intent.putExtra("match", files.remove(0).file.toString());
                    intent.putExtra("taken", refCowPath);
                    Log.d("SC", this.toString());
                    RecognizeCowActivity.this.startActivityForResult(intent, INT_IDENTIFIER);

                }
            });
            t.start();
        }
        else{
            Intent intent = new Intent(RecognizeCowActivity.this,AcceptMatchActivity.class);
            intent.putExtra("match", files.remove(0).file.toString());
            intent.putExtra("taken", refCowPath);
            Log.d("SC", this.toString());
            RecognizeCowActivity.this.startActivityForResult(intent, INT_IDENTIFIER);
        }
        //createProgressDialog();
        //new ComparisonTask().execute();

        //
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == INT_IDENTIFIER){
            Log.d("SC","Comes back with. "+requestCode);
            if(resultCode == AcceptMatchActivity.REMATCH){
                if(files == null){
                    onMatchClick(null);
                    return;
                }
                Log.d("SC", "Files remaining: "+files.size());
                onMatchClick(null);
            }
            else{
                BackToMain(null);
            }
        }
    }
}
