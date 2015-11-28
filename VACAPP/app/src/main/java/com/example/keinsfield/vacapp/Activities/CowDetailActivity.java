package com.example.keinsfield.vacapp.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.example.keinsfield.vacapp.Mundo.Cow;
import com.example.keinsfield.vacapp.Mundo.CowKey;
import com.example.keinsfield.vacapp.Mundo.CowPersistenceManager;
import com.example.keinsfield.vacapp.Mundo.Utilities;
import com.example.keinsfield.vacapp.R;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import android.widget.Button;
import android.media.MediaPlayer;
import android.media.MediaRecorder;


public class CowDetailActivity extends Activity {
    private static final int PROGRESS_ID = 0;
    private EditText nameText, textUltimoParto, textHato, textLoc, textPartos, textDiasLac, textLitros, textPrimerServicio;
    private EditText cowNumberText;
    private int currCowNumber;
    private String currFinca;
    private Spinner spinner;
    private ArrayAdapter<String> arrayAdapter;
    private Button editButton, saveButton, cancelButton, findButton;
    private ImageView imageView;
    private ArrayList<EditText> editTexts;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;
    private boolean onEditMode = false;
    private MediaRecorder myRecorder;
    private MediaPlayer myPlayer;
    private String outputFile = null;
    private Button startBtn;
    private Button stopBtn;
    private Button playBtn;
    private Button detenerReproduccion;
    private TextView rectext;
    private String outputPlayFile;

    private void setUnknown(boolean updateImage) {
        String s = "";
        for (EditText view : editTexts) view.setText(s);
        if(!updateImage) return;
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 4;
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.app_icon,opt);
        imageView.setImageBitmap(bmp);
    }

    private void disableAllTexts(){
        for(EditText view: editTexts) {
            disableView(view);
        }
    }

    private void enableAllTexts(){
        for(EditText view: editTexts){
            enableView(view);
        }
    }

    private void disableView(View view){
        view.setEnabled(false);
        //view.setFocusable(false);
        //view.setClickable(false);
    }

    private void enableView(View view){
        view.setEnabled(true);
        //view.setFocusable(true);
        //view.setClickable(true);
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
            nameText = (EditText) findViewById(R.id.nameText);
            textUltimoParto = (EditText) findViewById(R.id.textLastParto);
            textHato = (EditText) findViewById(R.id.textHato);
            textLoc = (EditText) findViewById(R.id.textLoc);
            textPartos = (EditText) findViewById(R.id.textPartos);
            textDiasLac = (EditText) findViewById(R.id.textDiasLac);
            textLitros = (EditText) findViewById(R.id.textLitros);
            textPrimerServicio = (EditText) findViewById(R.id.textPrimerServ);
            editTexts = new ArrayList<>();
            editTexts.add(nameText);
            editTexts.add(textUltimoParto);
            editTexts.add(textHato);
            editTexts.add(textLoc);
            editTexts.add(textPartos);
            editTexts.add(textLitros);
            editTexts.add(textDiasLac);
            editTexts.add(textPrimerServicio);
            cowNumberText = (EditText) findViewById(R.id.cowNumberEditText);
            spinner = (Spinner) findViewById(R.id.farmSpinner);
            arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, Utilities.GetFarms(this));
            spinner.setAdapter(arrayAdapter);

            editButton = (Button)findViewById(R.id.editButton);
            saveButton = (Button)findViewById(R.id.saveButton);
            cancelButton = (Button)findViewById(R.id.cancelButton);
            findButton = (Button)findViewById(R.id.buttonFind);

            saveButton.setVisibility(View.INVISIBLE);
            disableView(saveButton);
            cancelButton.setVisibility(View.INVISIBLE);
            disableView(cancelButton);
            editButton.setVisibility(View.INVISIBLE);
            disableView(editButton);
            setUnknown(true);
            disableAllTexts();

            //Shake detection:http://jasonmcreynolds.com/?p=388
            // ShakeDetector initialization
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mAccelerometer = mSensorManager
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mShakeDetector = new ShakeDetector();
            mShakeDetector.setOnShakeListener(new ShakeDetector.OnShakeListener() {

                @Override
                public void onShake(int count) {
				/*
				 * The following method, "handleShakeEvent(count):" is a stub //
				 * method you would use to setup whatever you want done once the
				 * device has been shook.
				 */
                    if(onEditMode) onCancelClick(null);
                }
            });

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




    @Override
    public void onResume() {
        super.onResume();
        // Add the following line to register the Session Manager Listener onResume
        mSensorManager.registerListener(mShakeDetector, mAccelerometer,	SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onPause() {
        // Add the following line to unregister the Sensor Manager onPause
        mSensorManager.unregisterListener(mShakeDetector);
        super.onPause();
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
        //If no cow is found, don't show the edit buttons.
        if (!cows.containsKey(key)) {
            setUnknown(true);
            Utilities.showDialog("Busqueda finalizada", "No se encontro ninguna vaca con numero " + nv + " en la finca " + finca + ".", this);
            editButton.setVisibility(View.INVISIBLE);
            disableView(editButton);
            return;
        }
        disableAllTexts();
        editButton.setVisibility(View.VISIBLE);
        enableView(editButton);
        currCowNumber = nv;
        currFinca = finca;
        Cow cow = cows.get(key);
        updateCow(cow);
    }

    private void updateCow(Cow cow) {
        editButton.setVisibility(View.VISIBLE);
        enableView(editButton);
        setUnknown(true);
        if (cow.nombre.isEmpty()) {
            nameText.setText("No name specificed.");
        }
        else {
            nameText.setText(cow.nombre);
        }
        textUltimoParto.setText(cow.ultimo_parto);
        String s = (cow.hato == -1) ? "" : cow.hato + "";
        textHato.setText(s);
         textLoc.setText(cow.loc);
        s = (cow.partos == -1) ? "" : cow.partos + "";
        textPartos.setText(s);
        s = (cow.dias_lac == -1) ? "" : cow.dias_lac + "";
        textDiasLac.setText(s);
        textPrimerServicio.setText(cow.primer_servicio);
        s = (cow.lts_dia == -1) ? "" : cow.lts_dia + "";
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

    private void refreshWithLastValues(){
        cowNumberText.setText(currCowNumber+"");
        for(int i = 0; i < arrayAdapter.getCount(); i ++){
            if(arrayAdapter.getItem(i).equals(currFinca)){
                spinner.setSelection(i);
                break;
            }
        }
        onFindClick(null);
    }
    private void afterModifySuccess(){
        Utilities.showDialog("Vaca guardada exitosamente", "Vaca guardada exitosamente", this);
        backToNormal();
        refreshWithLastValues();
    }

    private void onCancelChanges(){
        refreshWithLastValues();
        backToNormal();
    }

    private void backToNormal(){
        enableView(cowNumberText);
        enableView(spinner);
        disableAllTexts();
        disableView(saveButton);
        disableView(cancelButton);
        saveButton.setVisibility(View.INVISIBLE);
        cancelButton.setVisibility(View.INVISIBLE);
        enableView(findButton);
        onEditMode = false;

    }

    public void onEditClick(View v){
        enableAllTexts();
        saveButton.setVisibility(View.VISIBLE);
        enableView(saveButton);
        cancelButton.setVisibility(View.VISIBLE);
        disableView(editButton);
        editButton.setVisibility(View.INVISIBLE);
        enableView(cancelButton);
        disableView(cowNumberText);
        disableView(spinner);
        disableView(findButton);
        onEditMode = true;
    }

    public void onSaveClick(View view){

        try {
            String s;
            s = textHato.getText().toString();
            int hato = s.isEmpty() ? -1 : Integer.parseInt(s);
            s = textPartos.getText().toString();
            int partos = s.isEmpty() ? -1 : Integer.parseInt(s);
            s = textLitros.getText().toString();
            int litros = s.isEmpty() ? -1 : Integer.parseInt(s);
            s = textDiasLac.getText().toString();
            int diasLac = s.isEmpty() ? -1 : Integer.parseInt(s);
            String name = nameText.getText().toString();
            String primer_serv = textPrimerServicio.getText().toString();
            String loc = textLoc.getText().toString();
            String ultimoP = textUltimoParto.getText().toString();
            int number = currCowNumber;
            String finca = currFinca;
            final Cow newCow = new Cow(name, finca, number, ultimoP, hato, loc, partos, diasLac, litros, primer_serv);
            DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try{
                        CowPersistenceManager.modifyCow(newCow, CowDetailActivity.this);
                        dialogInterface.dismiss();
                        CowDetailActivity.this.afterModifySuccess();
                    }
                    catch(Exception e){
                        Log.d("SC","Mistakes were made modifying cow.");
                        e.printStackTrace();
                    }
                }
            };
            DialogInterface.OnClickListener noListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try{
                        return;
                    }
                    catch(Exception e){
                    }
                }
            };
            Utilities.showYesNoDialog("Guardar cambios","Esta seguro que desea guardar los cambios?", this, yesListener, "Guardar.", noListener, "Cancelar");

        }
        catch(Exception e){
            Utilities.showDialog("Valores no validos.", "Existen valores no validos para algunos atributos.",this);
            return;
        }
    }

    public void onCancelClick(View view){
        DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try{
                    onCancelChanges();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        };
        DialogInterface.OnClickListener noListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try{
                    return;
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        };
        Utilities.showYesNoDialog("Descartar cambios.","Seguro que desea descartar los cambios?",this,yesListener,"Si",noListener,"No");
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
