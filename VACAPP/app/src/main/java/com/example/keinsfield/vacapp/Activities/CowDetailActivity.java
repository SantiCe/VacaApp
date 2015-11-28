package com.example.keinsfield.vacapp.Activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.keinsfield.vacapp.Mundo.Cow;
import com.example.keinsfield.vacapp.Mundo.CowKey;
import com.example.keinsfield.vacapp.Mundo.CowPersistenceManager;
import com.example.keinsfield.vacapp.Mundo.Utilities;
import com.example.keinsfield.vacapp.R;


import java.io.File;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import android.widget.Button;
import android.media.MediaPlayer;
import android.media.MediaRecorder;


public class CowDetailActivity extends Activity {
    private static final int PROGRESS_ID = 0;
    private TextView nameText, textUltimoParto, textHato, textLoc, textPartos, textDiasLac, textLitros, textPrimerServicio;
    private EditText cowNumberText;
    private Spinner spinner;
    private ImageView imageView;
    private ProgressDialog progressDialog;
    private ArrayList<TextView> textViews;
    private MediaRecorder myRecorder;
    private MediaPlayer myPlayer;
    private String outputFile = null;
    private Button startBtn;
    private Button stopBtn;
    private Button playBtn;
    private Button detenerReproduccion;
    private TextView rectext;
    private String outputPlayFile;

    private void setUnknown() {
        String s = "- -";
        for (TextView view : textViews) view.setText(s);
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 4;
        Bitmap bmp = BitmapFactory.decodeResource(getResources(),R.drawable.app_icon,opt);
        imageView.setImageBitmap(bmp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cow_detail);

        startBtn = (Button)findViewById(R.id.record);
        startBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                start(v);
            }
        });

        stopBtn = (Button)findViewById(R.id.detener);
        stopBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stop(v);
            }
        });

        playBtn = (Button)findViewById(R.id.reproducir);
        playBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                play(v);
            }
        });

        detenerReproduccion = (Button) findViewById(R.id.detenerReproduccion);
        detenerReproduccion.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPlay(v);
            }
        });

        rectext = (TextView) findViewById(R.id.rectext);


        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            imageView = (ImageView) findViewById(R.id.imageView);
            nameText = (TextView) findViewById(R.id.nameText);
            textUltimoParto = (TextView) findViewById(R.id.textLastParto);
            textHato = (TextView) findViewById(R.id.textHato);
            textLoc = (TextView) findViewById(R.id.textLoc);
            textPartos = (TextView) findViewById(R.id.textPartos);
            textDiasLac = (TextView) findViewById(R.id.textDiasLac);
            textLitros = (TextView) findViewById(R.id.textLitros);
            textPrimerServicio = (TextView) findViewById(R.id.textPrimerServ);
            textViews = new ArrayList<>();
            textViews.add(nameText);
            textViews.add(textUltimoParto);
            textViews.add(textHato);
            textViews.add(textLoc);
            textViews.add(textPartos);
            textViews.add(textDiasLac);
            textViews.add(textPrimerServicio);
            setUnknown();
            cowNumberText = (EditText) findViewById(R.id.cowNumberEditText);
            spinner = (Spinner) findViewById(R.id.farmSpinner);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Utilities.GetFarms(this));
            spinner.setAdapter(adapter);

            //crear grabar
            myRecorder = new MediaRecorder();
            myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            myRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            myRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/VacappGrabaciones/";

        } catch (Exception e) {
            Log.d("SC", "FUCKED UP " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }


    public void start(View view){
        Log.d("DBG", "Start recording" );
        Log.d("DBG",  outputFile );

        try {
            if(!nameText.getText().toString().equals("- -")) {
                Log.d("DBG", "entro con " + spinner.getSelectedItem() + "/" + cowNumberText.getText().toString());
                File output = new File(outputFile + spinner.getSelectedItem() + "/");
                if (!output.exists()) {
                    output.mkdirs();
                }
                outputPlayFile = output.getAbsolutePath() + "/" + cowNumberText.getText().toString() + ".3gpp";
                myRecorder.setOutputFile(outputPlayFile);
                myRecorder.prepare();
                myRecorder.start();

                rectext.setText("Recording Point: Recording");
                startBtn.setEnabled(false);
                stopBtn.setEnabled(true);

            }
            else
            {
                Utilities.showDialog("Error","Por favor seleccionar una vaca válida del sistema",this);
            }
        } catch (IllegalStateException e) {
            // start:it is called before prepare()
            // prepare: it is called after start() or before setOutputFormat()
            e.printStackTrace();
        } catch (IOException e) {
            // prepare() fails
            e.printStackTrace();
        }

        /*text.setText("Recording Point: Recording");
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);

        Toast.makeText(getApplicationContext(), "Start recording...",
                Toast.LENGTH_SHORT).show();*/
    }

    public void stop(View view){
        Log.d("DBG", "Stop recording" );
        try {
            myRecorder.stop();
            myRecorder.release();
            myRecorder  = null;

            stopBtn.setEnabled(false);
            playBtn.setEnabled(true);
            rectext.setText("Recording Point: Stop recording");

            /*text.setText("Recording Point: Stop recording");

            Toast.makeText(getApplicationContext(), "Stop recording...",
                    Toast.LENGTH_SHORT).show();*/
        } catch (IllegalStateException e) {
            //  it is called before start()
            e.printStackTrace();
        } catch (RuntimeException e) {
            // no valid audio/video data has been received
            e.printStackTrace();
        }
    }

    public void play(View view) {
        try{

            if(!nameText.getText().toString().equals("- -")) {
                File output = new File(outputFile + spinner.getSelectedItem() + "/");
                outputPlayFile = output.getAbsolutePath() + "/" + cowNumberText.getText().toString() + ".3gpp";
                myPlayer = new MediaPlayer();
                myPlayer.setDataSource(outputPlayFile);
                myPlayer.prepare();
                myPlayer.start();

                playBtn.setEnabled(false);
                detenerReproduccion.setEnabled(true);
                rectext.setText("Recording Point: Playing");

            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void stopPlay(View view) {
        try {
            if (myPlayer != null) {
                myPlayer.stop();
                myPlayer.release();
                myPlayer = null;
                playBtn.setEnabled(true);
                detenerReproduccion.setEnabled(false);
                rectext.setText("Recording Point: Stop playing");

            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }




    public void onFindClick(View v) {
        HashMap<CowKey, Cow> cows = CowPersistenceManager.getCows(this);
        String snv = cowNumberText.getText().toString();
        if (snv == null || snv.isEmpty() || spinner.getSelectedItem() == null) return;
        int nv = -1;
        try {
            nv = Integer.parseInt(snv);
        } catch (Exception e) {
            return;
        }
        String finca = spinner.getSelectedItem().toString();
        CowKey key = new CowKey(nv, finca);
        Log.d("SC", "MYKEY: " + key.nv + "," + key.farm);
        for (CowKey ckey : cows.keySet()) {
            Log.d("SC", "CKEY: " + ckey.nv + "," + ckey.farm + " " + ckey.equals(key));
        }
        if (!cows.containsKey(key)) {
            setUnknown();
            Utilities.showDialog("Busqueda finalizada", "No se encontro ninguna vaca con numero " + nv + " en la finca " + finca + ".", this);
            return;
        }
        Cow cow = cows.get(key);
        updateCow(cow);
    }

    private void updateCow(Cow cow) {
        if (cow.nombre.isEmpty()) nameText.setText("No name specificed.");
        else nameText.setText(cow.nombre);
        textUltimoParto.setText(cow.ultimo_parto);
        String s = (cow.hato == -1) ? "- -" : cow.hato + "";
        textHato.setText(s);
        textLoc.setText(cow.loc);
        s = (cow.partos == -1) ? "- -" : cow.partos + "";
        textPartos.setText(s);
        s = (cow.dias_lac == -1) ? "- -" : cow.dias_lac + "";
        textDiasLac.setText(s);
        textPrimerServicio.setText(cow.primer_servicio);
        s = (cow.lts_dia == -1) ? "- -" : cow.lts_dia + "";
        textLitros.setText(s);

        //Look for image.
        File farm = new File(Utilities.GetStorageDirectory(this), cow.finca);
        if (!farm.exists() || !farm.isDirectory()) imageView.setImageResource(R.drawable.app_icon);
        else {
            for (File file : farm.listFiles()) {
                if (Utilities.isVacappImage(file)) {
                    if (Utilities.getCowNumberFromFile(file) == cow.nv) {
                        imageView.setImageBitmap(Utilities.fileToBitmap(file.toString(), this, 0.4, 0.8));
                    }
                }
            }
        }
    }

    public void BackToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void onBackPressed() {
        BackToMain();
        finish();
    }
}
