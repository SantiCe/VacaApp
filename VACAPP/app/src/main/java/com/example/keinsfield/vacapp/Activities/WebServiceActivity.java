package com.example.keinsfield.vacapp.Activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.example.keinsfield.vacapp.Mundo.Cow;
import com.example.keinsfield.vacapp.Mundo.CowPersistenceManager;
import com.example.keinsfield.vacapp.Mundo.Utilities;
import com.example.keinsfield.vacapp.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class WebServiceActivity extends Activity {

    private static final int PROGRESS_ID = 0;
    private ListView listView;
    private EditText editText;
    private ProgressDialog progressDialog;
    private ArrayAdapter<String> errorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_service);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        errorAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, new String[]{"No hay informacion para mostrar."});
        listView = (ListView)findViewById(R.id.listView);
        listView.setAdapter(errorAdapter);
        editText = (EditText)findViewById(R.id.textUrl);
    }

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

    public void onSyncClick(View v) {
        if (editText.getText() == null) return;
        String url = editText.getText().toString();
        url = "http://private-516ee-vacapp.apiary-mock.com/cows";
        DownloadTask task = new DownloadTask(this);
        task.execute(url);
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
                listView.setAdapter(errorAdapter);
                return;
            }
            try {
                ArrayList<Cow> list = CowPersistenceManager.syncWithWebService(result, context);
                Utilities.showDialog("Sincronizacion Completa", "La informacion en el webservice ahora está disponible localmente. Mostrando informacion de "
                        +"vacas importadas.", context);
                String[] stringAr = new String[list.size()];
                for(int i = 0; i < stringAr.length; i ++){
                    stringAr[i] = list.get(i).toString();
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1,stringAr);
                listView.setAdapter(adapter);

            } catch (Exception e) {
                Utilities.showDialog("Error", "Se pudo adquirir informacion del webservice, pero hubo problemas leyendola. " + e.getMessage(), context);
                listView.setAdapter(errorAdapter);
                error = e;
            }

        }
    }

}


