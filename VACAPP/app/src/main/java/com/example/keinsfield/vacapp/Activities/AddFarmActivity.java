package com.example.keinsfield.vacapp.Activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.example.keinsfield.vacapp.Mundo.Utilities;
import com.example.keinsfield.vacapp.R;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Created by Galapagos on 11/09/2015.
 */
public class AddFarmActivity extends Activity {
    private LatLng curPlace = Utilities.ubate;
    private int cowNumber=-1;
    private String filePath;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_farm);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ListView view = (ListView) findViewById(R.id.farmListView);


        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.custom_list_item, Utilities.GetFarms(this));
        view.setAdapter(adapter);

        Intent intent= getIntent();
        cowNumber = intent.getIntExtra("number",-1);
        filePath = intent.getStringExtra("photo");
    }

    private Intent makeIntent(){
        Intent intent = new Intent(getApplicationContext(),AddCowActivity.class);
        intent.putExtra("number", cowNumber);
        intent.putExtra("photo", filePath);
        intent.putExtra("caller", "AddFarm");
        return intent;
    }


    public void OnBackButton(View v){
        onBackPressed();
        finish();
    }

    public void OnSaveButton(View v){
        EditText et = (EditText)findViewById(R.id.farmText);
        if(et.getText() == null) {
            OnBackButton(v);
            return;
        }
        String t = et.getText().toString();
        if(t == null) {
            OnBackButton(v);
            return;
        }
        ArrayList<String> farms = Utilities.GetFarms(this);
        File root = Utilities.GetStorageDirectory(this);
        File dir = new File(root,t);

        dir.mkdirs();
        File info = new File(dir,"info.txt");
        if(info.exists()) info.delete();
        try {
            PrintWriter pw = new PrintWriter(info);
            pw.println(curPlace.latitude+" "+curPlace.longitude);
            pw.close();
            Log.d("SC","Written "+info.toString());
        } catch (Exception e) {
            Log.d("SC", "Error saving info file for farm " + t + " ");
            e.printStackTrace();
        }
        Intent intent = makeIntent();
        intent.putExtra("farm",t);
        startActivity(intent);
        finish();
    }

    public void onMapButton(View v){
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
                startActivityForResult(builder.build(this), 1);
        }
        catch(Exception e){
            String msg = "Servicio de mapas no disponible.";
            Utilities.showDialog("Error",msg,this);
        }
    }

    protected void onActivityResult(int reqCode, int resCode, Intent data){
        if(reqCode == 1) {
            if (resCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(data, this);
                curPlace = place.getLatLng();
                Log.d("SC","Place identified:"+curPlace.toString());
            } else {
                Utilities.showDialog("Error", "Se detectó un error. Por favor intente de nuevo.", this);
            }
        }
    }
}
