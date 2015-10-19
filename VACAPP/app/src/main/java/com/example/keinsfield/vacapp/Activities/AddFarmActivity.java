package com.example.keinsfield.vacapp.Activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.example.keinsfield.vacapp.Mundo.Utilities;
import com.example.keinsfield.vacapp.R;

import java.io.File;

/**
 * Created by Galapagos on 11/09/2015.
 */
public class AddFarmActivity extends Activity {

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
        intent.putExtra("photo",filePath);
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
        File root = Utilities.GetStorageDirectory(this);
        File dir = new File(root,t);
        dir.mkdirs();
        Intent intent = makeIntent();
        intent.putExtra("farm",t);
        startActivity(intent);
        finish();
    }
}
