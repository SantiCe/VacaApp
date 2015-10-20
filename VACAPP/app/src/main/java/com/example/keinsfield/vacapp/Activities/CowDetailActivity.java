package com.example.keinsfield.vacapp.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class CowDetailActivity extends Activity {
    private static final int PROGRESS_ID = 0;
    private TextView nameText, textUltimoParto, textHato, textLoc, textPartos, textDiasLac, textLitros, textPrimerServicio;
    private EditText cowNumberText;
    private Spinner spinner;
    private ImageView imageView;
    private ProgressDialog progressDialog;
    private ArrayList<TextView> textViews;

    protected void onPrepareDialog(int id, Dialog dialog) {
        progressDialog.setTitle("Importando informacion.");
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setProgress(0);
        progressDialog.show();

    }

    protected Dialog onCreateDialog(int id) {
        progressDialog = new ProgressDialog(this);
        return progressDialog;
    }

    private void setUnknown() {
        String s = "- -";
        for (TextView view : textViews) view.setText(s);
        imageView.setImageResource(R.drawable.app_icon);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cow_detail);
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

        } catch (Exception e) {
            Log.d("SC", "FUCKED UP " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public void onSyncClick(View v) {
        final String[] url = {null};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar URL con informacion de vacas");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                url[0] = input.getText().toString();
                DownloadTask task = new DownloadTask(CowDetailActivity.this);
                task.execute(url);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        Dialog d = builder.show();
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

    class DownloadTask extends AsyncTask<String, Integer, String> {
        public Exception error = null;
        public Activity context;

        public DownloadTask(Activity context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            context.showDialog(PROGRESS_ID);
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                String json = "";
                String line;
                while ((line = br.readLine()) != null) {
                    json += line;
                }
                br.close();
                return json;
            } catch (Exception e) {
                e.printStackTrace();
                error = e;
                return null;
            }
        }

        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            if (result == null) {

                Utilities.showDialog("Error", "Error adquiriendo informacion del webservice. " + error.getMessage(), context);
                return;
            }
            try {
                int len =CowPersistenceManager.syncWithWebService(result, context);

                Utilities.showDialog("Syncronizacion Completa", "La informacion en el webservice ahora está disponible localmente. Importadas "+len+" vacas.", context);
            } catch (Exception e) {
                Utilities.showDialog("Error", "Se pudo adquirir informacion del webservice, pero hubo problemas leyendola. " + e.getMessage(), context);
                error = e;
            }

        }
    }

}
