package com.example.keinsfield.vacapp.Activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import com.example.keinsfield.vacapp.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class GDriveImportExportActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //ATRIBUTES
    protected static GoogleApiClient mGoogleApiClient;
    protected ProgressDialog progressDialog;

    private static final int EXPORT_ID = 1;
    private static final int IMPORT_ID = 2;
    private static final int CONNECT_ID = 3;
    private static final String IMPORT_MSG ="Importando de Google Drive. \n";
    protected static final int RESOLVE_CONNECTION_REQUEST_CODE = 24;

    private ListView localListView;
    private ArrayAdapter<String> localAdapter;
    private ListView driveListView;
    private ArrayAdapter<String> driveAdapter;
    private Button importButton;
    //--------------
    //Async tasks
    //--------------

    class ExportTask extends AsyncTask<String,String,Boolean>{
        private GDriveImportExportActivity context;
        private Exception error = null;
        private ArrayList<String> failFarms = new ArrayList<>();
        public ExportTask(GDriveImportExportActivity context){
            this.context = context;
        }

        protected void onPreExecute(){
            context.showDialog(EXPORT_ID);
        }
        @Override
        protected Boolean doInBackground(String... params) {
            try{
                int count = params.length;
                DriveFolder vacappFolder = getOrCreateVacappFolder();
                HashMap<String,ArrayList<File>> allPics = Utilities.getAllVacappImages(context);
                for(String farm:params){
                    ArrayList<File> files = allPics.get(farm);
                    try{
                        exportFarm(farm,files,vacappFolder);
                        publishProgress("Exporta correctamente "+farm);
                    }
                    catch(Exception e){
                        failFarms.add(farm);
                        publishProgress("Error exportando: "+farm);
                    }
                }
                return true;
            }
            catch(Exception e){
                error = e;
                return false;
            }
        }

        protected void onProgressUpdate(String... progress){
            try{
                context.progressDialog.setMessage(progress[0]);
            }
            catch(Exception e){
                Log.d("SC",e.getMessage());
            }
        }

        protected void onPostExecute(Boolean result){
            try {
                context.progressDialog.dismiss();
                if (result) {
                    if (failFarms.size() == 0) {
                        Utilities.showDialog("Operacion finalizada", "Todas las fincas fueron exportadas satisfactoriamente.", context);
                    } else {
                        String ms = "Hubo problemas exportando las siguientes fincas: ";
                        for (String f : failFarms) {
                            ms += "\n " + f;
                        }
                        Utilities.showDialog("Operacion finalizada", ms, context);
                    }
                } else {
                    Utilities.showDialog("Error", "Problemas en la conexion con GDrive. " + error.getMessage(), context);
                }
            }
            catch(Exception e){
                Log.d("SC",e.getMessage());
            }
        }
    }

    class InitializeTask extends AsyncTask<Integer,Integer,Boolean>{
        private Exception error=null;
        private GDriveImportExportActivity context;
        private String[] driveFarms;
        private int attempts = 0;
        public InitializeTask(GDriveImportExportActivity context){
            this.context = context;
        }
        @Override
        protected Boolean doInBackground(Integer... params) {
            try{

                ArrayList<String> list = getGoogleDriveFarms();
                driveFarms = new String[list.size()];
                list.toArray(driveFarms);
                return true;
            } catch (Exception e) {
                if(attempts < 5){
                    attempts++;
                    Log.d("SC","Gets attempt "+attempts);
                    return doInBackground(params);
                }
                Log.d("SC",e.getMessage());
                error = e;
                return false;
            }
        }

        protected void onPostExecute(Boolean result){
            if(!result) Utilities.showDialog("Error", error.getMessage(),context);
            else{

                driveAdapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_multiple_choice,driveFarms);
                driveListView.setAdapter(driveAdapter);
                importButton.setEnabled(true);
                importButton.setClickable(true);
            }
        }
    }

    class ImportTask extends AsyncTask<String,String,Boolean>{
        private GDriveImportExportActivity context;
        private Exception error = null;
        private ArrayList<String> failFarms = new ArrayList<>();
        public ImportTask(GDriveImportExportActivity context){
            this.context = context;
        }

        protected void onPreExecute(){
            context.showDialog(IMPORT_ID);
        }
        @Override
        protected Boolean doInBackground(String... params) {
            try{
                int count = params.length;
                for(String farm:params){
                    try{
                        publishProgress("Importando: "+farm);
                        importFarm(farm);
                        publishProgress("Importa correctamente "+farm);
                    }
                    catch(Exception e){
                        failFarms.add(farm);
                        publishProgress("Error importando: "+farm);
                    }
                }
                return true;
            }
            catch(Exception e){
                error = e;
                return false;
            }
        }

        protected void onProgressUpdate(String... progress){
            try{
                context.progressDialog.setMessage(progress[0]);
            }
            catch(Exception e){
                Log.d("SC",e.getMessage());
            }
        }

        protected void onPostExecute(Boolean result){
            try {
                context.progressDialog.dismiss();
                if (result) {
                    if (failFarms.size() == 0) {
                        Utilities.showDialog("Operacion finalizada", "Todas las fincas fueron importadas satisfactoriamente.", context);
                    } else {
                        String ms = "Hubo problemas importando las siguientes fincas: ";
                        for (String f : failFarms) {
                            ms += "\n " + f;
                        }
                        Utilities.showDialog("Operacion finalizada", ms, context);
                    }
                } else {
                    Utilities.showDialog("Error", "Problemas en la conexion con GDrive. " + error.getMessage(), context);
                }
            }
            catch(Exception e){
                Log.d("SC",e.getMessage());
            }
        }


    }
    //---------------------------------------------------------------------------------------
    //Overriden methods for activity.
    //---------------------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gdrive_import_export);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        localListView = (ListView)findViewById(R.id.localListView);
        localListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        driveListView = (ListView)findViewById(R.id.driveListView);
        driveListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        importButton = (Button)findViewById(R.id.importButton);
        importButton.setClickable(false);
        importButton.setEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        ArrayList<String> localFarms = Utilities.GetFarms(this);
        localAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice,Utilities.GetFarms(this));
        localListView.setAdapter(localAdapter);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("SC", "VACAPP connected to GDrive.");

        InitializeTask iniTask = new InitializeTask(this);
        iniTask.execute();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public void onBackPressed(){
        BackToMain(null);
        this.finish();
    }
    //------------------------------------------------------------------------------------------
    //Dialog methods
    //-----------------------------------------------------------------------------------------
    protected void onPrepareDialog(int id, Dialog dialog){
        Log.d("SC","Calls on prepare");
        Log.d("SC","creating progress dialog");
        int maxProg = 100;
        if(id==EXPORT_ID) {
            progressDialog.setTitle("Exportando imagenes.");
            progressDialog.setMessage("Exportando imagenes.");
            //TODO: Change maxProg
        }
        else if(id == IMPORT_ID){
            progressDialog.setTitle("Importando imagenes.");
            progressDialog.setMessage("Importando imagenes.");
        }
        else if(id == CONNECT_ID){
            progressDialog.setTitle("Conectando con Google Drive.");
            progressDialog.setMessage("Conectando con Google Drive.");
        }
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setProgress(0);
        progressDialog.setMax(maxProg);
        progressDialog.show();

    }

    protected Dialog onCreateDialog(int id)
    {
        ProgressDialog progress= null;
        if(id == EXPORT_ID) {
            if (progressDialog == null) progressDialog = new ProgressDialog(GDriveImportExportActivity.this);
            return progressDialog;
        }
        else if(id == IMPORT_ID){
            if(progressDialog == null) progressDialog = new ProgressDialog(GDriveImportExportActivity.this);
            return progressDialog;
        }
        return progress;
    }

    //---------------------------------------------------------------------------
    //Buttons
    //---------------------------------------------------------------------------
    public void onExportClick(View v){
        SparseBooleanArray checked = localListView.getCheckedItemPositions();
        ArrayList<String> selectedFarms = new ArrayList<>();
        for(int i = 0; i < checked.size(); i ++){
            int pos = checked.keyAt(i);
            if(checked.valueAt(i)){
                Log.d("SC","Selected: "+localAdapter.getItem(pos));
                selectedFarms.add(localAdapter.getItem(pos));
            }
        }
        String[] params = new String[selectedFarms.size()];
        selectedFarms.toArray(params);
        ExportTask eTask = new ExportTask(this);
        eTask.execute(params);
    }

    public void onImportClick(View v){
        SparseBooleanArray checked = driveListView.getCheckedItemPositions();
        ArrayList<String> selectedFarms = new ArrayList<>();
        for(int i = 0; i < checked.size(); i ++){
            int pos = checked.keyAt(i);
            if(checked.valueAt(i)){
                Log.d("SC","Selected: "+driveAdapter.getItem(pos));
                selectedFarms.add(driveAdapter.getItem(pos));
            }
        }
        String[] params = new String[selectedFarms.size()];
        selectedFarms.toArray(params);
        ImportTask iTask = new ImportTask(this);
        iTask.execute(params);
    }
    //---------------------------------------------------------------------------------------------------------------------------------
    //FOLDER UTILS
    //----------------------------------------------------------------------------------------------------------------------------------

    /*Gets a directory with the specified name under the specified parent directory, or returns null if no
        such directory exists */
    public DriveFolder getFolder(String name, DriveFolder parent){
        Query query = new Query.Builder().addFilter(Filters.and(
                Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"),
                Filters.eq(SearchableField.TITLE, name),
                Filters.eq(SearchableField.TRASHED, false))).build();
        MetadataBuffer result = parent.queryChildren(mGoogleApiClient,query).await(10, TimeUnit.SECONDS).getMetadataBuffer();
        DriveFolder folder = null;
        if(result.getCount() == 0) return null;
        Metadata folderMetadata = null;
        for(Metadata meta:result){
            if(!meta.isFolder()) continue;

            if(folderMetadata == null) folderMetadata = meta;
            else if(meta.getCreatedDate().compareTo(folderMetadata.getCreatedDate()) > 0) folderMetadata = meta;
        }
        if(folderMetadata != null) {
            folder = Drive.DriveApi.getFolder(mGoogleApiClient,folderMetadata.getDriveId());
        }
        return folder;
    }

    /*Gets or creates a new directory with the specified name under the specified parent directory.*/
    public DriveFolder getOrCreateFolder(String name, DriveFolder parent){
        Query query = new Query.Builder().addFilter(Filters.and(
                Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"),
                Filters.eq(SearchableField.TITLE, name),
                Filters.eq(SearchableField.TRASHED, false))).build();
        MetadataBuffer result = parent.queryChildren(mGoogleApiClient,query).await(10, TimeUnit.SECONDS).getMetadataBuffer();
        DriveFolder folder = getFolder(name, parent);
        if(folder == null) folder = createFolder(name,parent);
        return folder;
    }

    /*Creates a new directory with the specified name under the specified parent directory.*/
    private DriveFolder createFolder(String name, DriveFolder parent){
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(name).build();
        return parent.createFolder(mGoogleApiClient,changeSet).await(10,TimeUnit.SECONDS).getDriveFolder();
    }

    /*Gets or creates the VACAPP directory*/
    public DriveFolder getOrCreateVacappFolder(){
        final DriveFolder rootFolder = Drive.DriveApi.getRootFolder(mGoogleApiClient);
        //Find VACAPP folder.
        DriveFolder vacappFolder = getOrCreateFolder("VACAPP", rootFolder);
        Log.d("SC","Encuentra la carpeta VACAPP en GDrive.");
        return vacappFolder;
    }

    /*Gets all directories in the VACAPP folder*/
    public ArrayList<String> getGoogleDriveFarms() throws Exception{
        ArrayList<String> list = new ArrayList<>();
        DriveFolder vacappFolder = getOrCreateVacappFolder();
        Query query = new Query.Builder().addFilter(
                Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder")).build();
        DriveApi.MetadataBufferResult bufferResult = vacappFolder.queryChildren(mGoogleApiClient, query).await(10, TimeUnit.SECONDS);
        if(!bufferResult.getStatus().isSuccess()) throw new Exception("Error adquiriendo datos de Google Drive.");
        MetadataBuffer buffer = bufferResult.getMetadataBuffer();
        for(Metadata meta:buffer){
            if(meta.isFolder() && !meta.isTrashed()) list.add(meta.getTitle());
        }
        return list;
    }

    //--------------------------------------------------------------------------------
    //File Utils
    //--------------------------------------------------------------------------------
    public DriveId getFile(String name, DriveFolder folder){
        Query query = new Query.Builder().addFilter(Filters.and(
                Filters.eq(SearchableField.TITLE, name),
                Filters.eq(SearchableField.TRASHED, false))).build();
        MetadataBuffer result = folder.queryChildren(mGoogleApiClient, query).await().getMetadataBuffer();
        if(result.getCount() == 0) return null;
        return result.get(0).getDriveId();
    }

    public DriveFile createOrUpdateFile(File file, DriveFolder folder) throws Exception{
        String name = file.getName();
        DriveId id = getFile(name, folder);
        Log.d("SC","Working on: "+file.toString());
        if(id != null){
            DriveFile existingFile = Drive.DriveApi.getFile(mGoogleApiClient, id);
            Log.d("SC","File: "+id+" existed.");
            existingFile.delete(mGoogleApiClient).await();
            Log.d("SC","File: "+id+" deleted.");
        }
        DriveContents contents = Drive.DriveApi.newDriveContents(mGoogleApiClient).await().getDriveContents();
        Utilities.copyFile(new FileInputStream(file),contents.getOutputStream());

        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                .setMimeType("image/jpeg").setTitle(file.getName()).build();
        DriveFolder.DriveFileResult dfresult = folder.createFile(mGoogleApiClient, metadataChangeSet, contents).await();
        DriveResource.MetadataResult data = dfresult.getDriveFile().getMetadata(mGoogleApiClient).await();
        Log.d("SC","Created file: "+data.getMetadata().getAlternateLink()+" "+data.getMetadata().toString()+" success: "+dfresult.getStatus());
        return dfresult.getDriveFile();
    }

    //------------------------------------------------------------------------------------
    //Import/Export methods
    //------------------------------------------------------------------------------------

    /*Exports the specified pictures to Google Drive, under the specified farm directory.*/
    public void exportFarm(String farm, ArrayList<File> files, DriveFolder vacappFolder) throws Exception{
        Log.d("SC","Exporting: "+farm);
        DriveFolder farmFolder = getOrCreateFolder(farm, vacappFolder);
        for(File file:files){
            createOrUpdateFile(file,farmFolder);
        }
    }
    /*Exports all pictures of cows to the VACAPP folder in Google Drive,
        following the same directory/file naming scheme used for VACAPP.*/
    /*public void exportAllFarms(){
        try {

            showDialog(EXPORT_ID);
            Thread t = new Thread(new Runnable()
            {
                public void run()
                {
                        DriveFolder vacappFolder = getOrCreateVacappFolder();
                        HashMap<String, ArrayList<File>> pics = Utilities.getAllVacappImages(GDriveImportExportActivity.this);
                        for (String finca : pics.keySet()) {
                            exportFarm(finca, pics.get(finca),getOrCreateVacappFolder());
                        }
                }
            });
            t.start();

        }
        catch(Exception e){
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    BackToMain(null);
                }
            };
            Utilities.showDialog("Error","VACAPP tiene problemas en la conexión con Google Drive. "+e.getMessage(),this,listener);
        }
    }*/

    /*Imports pictures from the selected farm in Google Drive to the local storage.
            Creates a farm with the specified name if it doesn't exist already and overwrites all
            existing files with repeated names.
         */
    public void importFarm(final String farmName) throws Exception {

        DriveFolder folder = getFolder(farmName, getOrCreateVacappFolder());
        if (folder == null) {
            throw new Exception("Couldn't find a farm in Google Drive with name " + farmName);
        }
        //progressDialog.setMessage(IMPORT_MSG + "Conectando con Google Drive y configurando archivos locales.");
        //Get all cows from folder
        Query query = new Query.Builder().addFilter(
                Filters.contains(SearchableField.MIME_TYPE, "image")
        ).build();
        Log.d("SC", "Looking at: " + folder.getMetadata(mGoogleApiClient).await().getMetadata().getTitle());
        MetadataBuffer buffer = folder.queryChildren(mGoogleApiClient, query).await().getMetadataBuffer();
        ArrayList<Metadata> cows = new ArrayList<>();
        for (Metadata metadata : buffer) {
            Log.d("SC", "Checking: " + metadata.getTitle());
            if (Utilities.isVacappFilename(metadata.getTitle())) {
                cows.add(metadata);
            }
        }
        Log.d("SC", "Cows to import: " + cows.size());

        File localFolder = Utilities.GetStorageDirectory(GDriveImportExportActivity.this);
        if (!localFolder.mkdir() && !localFolder.exists()) throw new Exception("No se pudo crear" +
                " el directorio local de VACAPP.");
        localFolder = new File(localFolder, farmName);
        if (!localFolder.mkdir() && !localFolder.exists()) throw new Exception("No se pudo crear" +
                " el directorio local de la finca.");
        String msg = IMPORT_MSG + "Importando: ";
        for (Metadata cowMeta : cows) {
            DriveContents content = cowMeta.getDriveId().asDriveFile().open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await().getDriveContents();
            File file = new File(localFolder, cowMeta.getTitle());
            if (file.exists()) file.delete();
            OutputStream out = new FileOutputStream(file);
            Utilities.copyFile(content.getInputStream(), out);
            Log.d("SC", "Created: " + file.toString());
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                Utilities.showDialog("Error","Hubo un error en la conexión con Google Drive. Por favor intente más tarde.",this);
                Log.d("SC", e.getMessage());
                BackToMain(null);
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
            BackToMain(null);
            Log.d("SC","Couldn't resolve.");
        }
    }

    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            onConnected(null);
                        }
                    };
                    Utilities.showDialog("Conexión exitosa","VACAPP ahora está conectado con su cuenta de Google Drive.",this,listener);
                }
                break;
        }
    }

    public DialogInterface.OnClickListener BackToMainDialogListener(){
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                BackToMain(null);
            }
        };
        return listener;
    }
    private void BackToMain(View v){
        Intent intent=new Intent(GDriveImportExportActivity.this,MainActivity.class);
        startActivity(intent);
        finish();
    }
}
