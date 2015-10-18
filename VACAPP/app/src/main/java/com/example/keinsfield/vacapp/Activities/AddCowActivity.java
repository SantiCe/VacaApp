package com.example.keinsfield.vacapp.Activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

import com.example.keinsfield.vacapp.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class AddCowActivity extends Activity {

    private Bitmap image;
    private String filePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_cow);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //Set up spinner
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Utilities.GetFarms(this));
        spinner.setAdapter(adapter);
        Intent intent = getIntent();
        String caller = intent.getStringExtra("caller");
        int number = -1;

        try {
            filePath = intent.getStringExtra("photo");
            RandomAccessFile f = new RandomAccessFile(filePath, "r");
            byte[] data = new byte[(int) f.length()];
            f.read(data);
            image = Utilities.byteArrayToBitmap(data,this,0.4,0.8);
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
        }
        catch(Exception e){

        }
        EditText txtNumber = (EditText)findViewById(R.id.cowNumberText);
        if(number != -1)txtNumber.setText(""+number);
    }

    public void afterFarmCreated(String selected, int number){
        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        if(selected == null) return;
        SpinnerAdapter adapter = spinner.getAdapter();

        for(int i = 0; i < adapter.getCount(); i ++){
            String name = (String)adapter.getItem(i);
            if(name.equals(selected)){
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
        return BitmapFactory.decodeByteArray(data,0,data.length);
    }

    public void SaveCow(View v) throws Exception{
        Spinner spinner = (Spinner)findViewById(R.id.spinner);
        EditText numberField = (EditText)findViewById(R.id.cowNumberText);

        if(spinner.getSelectedItem() == null || numberField.getText() == null || numberField.getText().toString().isEmpty()){
            Utilities.showDialog("Error","Debe seleccionar una finca y un numero.",this);
            return;
        }
        String farm = spinner.getSelectedItem().toString();

        File rootFolder = Utilities.GetStorageDirectory(this);
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


}
