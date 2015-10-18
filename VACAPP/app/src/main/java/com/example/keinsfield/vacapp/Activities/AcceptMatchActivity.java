package com.example.keinsfield.vacapp.Activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.keinsfield.vacapp.R;

import java.io.File;

public class AcceptMatchActivity extends Activity {

    public static final int ACCEPT = 0;
    public static final int REMATCH = 1;
    public static final int EXIT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_match);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        Intent intent = getIntent();
        String taken = intent.getStringExtra("taken");
        String match = intent.getStringExtra("match");
        double wRatio = 0.4;
        double hRatio = 0.75;
        ImageView takenView = (ImageView)findViewById(R.id.takenImageView);
        takenView.setImageBitmap(Utilities.fileToBitmap(taken,this,wRatio,hRatio));

        ImageView matchView = (ImageView)findViewById(R.id.matchedImageView);
        matchView.setImageBitmap(Utilities.fileToBitmap(match,this,wRatio,hRatio));

        TextView tv = (TextView)findViewById(R.id.lblMejorMatch);
        tv.setText("Mejor match: "+new File(match).getName());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_accept_match, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void returnAnswerIntent(int answer){
        Intent resultIntent = new Intent();
        setResult(answer, resultIntent);
        finish();
    }

    public void onAcceptButton(View v){
        returnAnswerIntent(ACCEPT);
    }

    public void onRematchButton(View v){
        returnAnswerIntent(REMATCH);
    }

    public void onCancelButton(View v){
        returnAnswerIntent(EXIT);
    }
}
