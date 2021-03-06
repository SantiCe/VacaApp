package com.example.keinsfield.vacapp.Activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.example.keinsfield.vacapp.Mundo.Utilities;
import com.example.keinsfield.vacapp.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.List;

public class AddCowActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private Bitmap image;
    private String filePath;
    private double lat,lon;
    private ArrayAdapter<String> spinAdapter;
    private Spinner spinner;
    private static GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lat = Utilities.ubate.latitude;
        lon = Utilities.ubate.longitude;

        setContentView(R.layout.activity_add_cow);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //Set up spinner
        spinner = (Spinner) findViewById(R.id.spinner);
        spinAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Utilities.GetFarms(this));
        spinner.setAdapter(spinAdapter);
        Intent intent = getIntent();
        String caller = intent.getStringExtra("caller");
        int number = -1;

        try {
            filePath = intent.getStringExtra("photo");
            RandomAccessFile f = new RandomAccessFile(filePath, "r");
            byte[] data = new byte[(int) f.length()];
            f.read(data);
            image = Utilities.byteArrayToBitmap(data,this,0.33,0.33);
            if (image != null) {
                ImageView imageView = (ImageView) findViewById(R.id.imageView);
                imageView.setImageBitmap(image);
            }
            if (caller.equals("AddFarm")) {
                // Fill info if called after adding a farm.
                String selected = intent.getStringExtra("farm");
                number = intent.getIntExtra("number", -1);
                afterFarmCreated(selected, number);
            } else {
                number = intent.getIntExtra("number", -1);
            }

            //Connect to location API
            getLocation();
        }
        catch(Exception e){

        }
        EditText txtNumber = (EditText)findViewById(R.id.cowNumberText);
        if(number != -1)txtNumber.setText(""+number);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    protected void onStart(){
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }
    public void  afterFarmCreated(String selected, int number){
        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        if(selected == null) return;
        SpinnerAdapter adapter = spinner.getAdapter();

        for(int i = 0; i < adapter.getCount(); i ++) {
            String name = (String) adapter.getItem(i);
            if (name.equals(selected)) {
                spinner.setSelection(i);
                return;
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_cow, menu);
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

    public Bitmap fromBytes(byte[] data) throws Exception{
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    public void SaveCow(View v) throws Exception{
        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        EditText numberField = (EditText)findViewById(R.id.cowNumberText);

        if(spinner.getSelectedItem() == null || numberField.getText() == null || numberField.getText().toString().isEmpty()){
            Utilities.showDialog("Error","Debe seleccionar una finca y un numero.",this);
            return;
        }
        String farm = spinner.getSelectedItem().toString();

        File rootFolder = Utilities.GetPictureStorageDirectory(this);
        //Log.d("SC","Root: "+rootFolder.toString());
        String folderName = farm;
        int number = -1;
        String filename = "";
        File file = new File(rootFolder,folderName);
        try{
            number = Integer.parseInt(numberField.getText().toString());
            if(!file.mkdirs() && !file.isDirectory()) throw new Exception();
            filename = number+".jpeg";
            file = new File(file,filename);
        }
        catch(Exception e){
            Utilities.showDialog("ERROR","VacApp detecto un error en almacenamiento. Asegurese de seleccionar una finca y un numero.",this);
            return;
        }

        if(file.exists()){
            Utilities.showYesNoDialog("Vaca ya existe.", "Ya existe una vaca con el numero " + number + " en la finca " + farm + ". Desea sobreescribir?", this, overWriteClickListener(file, v, true), "Sobreescribir.", overWriteClickListener(null, null, false), "Cancelar.");
            return;
        }
        writeImage(file,v);

    }

    @Override
    public void onBackPressed() {
        BackToMain(null);
        finish();
    }

    private void writeImage(File file, View v){
        try {
            //TODO: Aca se mete la ruta de la imagen de checho.
            long l = System.currentTimeMillis();
            OutputStream out = new FileOutputStream(file,false);
            image.compress(Bitmap.CompressFormat.JPEG,100,out);
            /*// Transfer bytes from in to out
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();*/
            out.flush();
            out.close();
            Log.d("SC", "Saved: " + file.toString()+"\n"+(System.currentTimeMillis()-l));
            Utilities.GalleryAddPics(file.toString(), this);
            Utilities.showDialog("Vaca agregada.", "Se agrego la vaca con exito.", this, backToMainClickListener(v));
            return;
        }
        catch(Exception e){
            Utilities.showDialog("Error.", "La vaca no pudo ser agregada. " + e.getLocalizedMessage(), this,backToMainClickListener(v));
            Log.d("SC","Mistakes were made. "+e.getLocalizedMessage());
            return;
        }
    }

    public void BackToMain(View v){
        Intent intent=new Intent(getApplicationContext(),MainActivity.class);
        startActivity(intent);
        finish();
    }

    private DialogInterface.OnClickListener overWriteClickListener(final File file, final View view, boolean write) {

        if (write) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try{
                        file.delete();
                    }
                    catch(Exception e){
                        Log.d("SC","Tried deleting "+file.toString()+" and mistakes were made.");
                        e.printStackTrace();
                    }
                    Log.d("SC","Deleted "+file.toString());
                    writeImage(file,view);
                }
            };
            return listener;
        }
        else{
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            };
            return listener;
        }

    }

    private DialogInterface.OnClickListener backToMainClickListener(final View v){
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                BackToMain(v);
            }
        };
        return listener;
    }

    public void AddFarm(View v){
        EditText numberField = (EditText)findViewById(R.id.cowNumberText);
        String s = numberField.getText().toString();
        int a = s.equals("")?-1:Integer.parseInt(s);
        Intent intent = new Intent(this,AddFarmActivity.class);
        intent.putExtra("number",a);
        intent.putExtra("photo",filePath);
        startActivity(intent);
    }

    private void selectClosestFarm(){
        try{
            Location curLoc = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if(curLoc == null){

                curLoc.setLatitude(lat);
                curLoc.setLongitude(lon);
            }
            Log.d("SC","Identified currLoc:"+lat+" "+lon);
            double maxD = Double.MAX_VALUE - 3;
            int winFarm = -1;
            List<String> farms = Utilities.GetFarms(this);
            for(int i = 0; i < spinAdapter.getCount(); i ++){
                String farm = spinAdapter.getItem(i);
                Location floc = Utilities.getFarmLocation(farm,this);
                Log.d("SC",farm+" "+floc);

                if(floc.distanceTo(curLoc) < maxD){
                    maxD = floc.distanceTo(curLoc);
                    winFarm = i;
                }
            }
            if(winFarm == -1) return;
            else{
                spinner.setSelection(winFarm);
            }
        }
        catch(Exception e){
            Log.d("SC","Unable to find closest farm. "+e.getMessage());
            e.printStackTrace();
        }
    }
    private synchronized void getLocation(){
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        buildGoogleApiClient();
    }
    @Override
    public void onConnected(Bundle bundle) {
        //Try to select the closest farm.
        Log.d("SC","Does this ever get called?");
        Log.d("SC","Is client connected? "+mGoogleApiClient.isConnected());
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,locationRequest,this);
        selectClosestFarm();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, "location :" + location.getLatitude() + " , " + location.getLongitude(), Toast.LENGTH_SHORT).show();
    }
}
