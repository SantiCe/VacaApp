package com.example.keinsfield.vacapp.Activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.example.keinsfield.vacapp.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.util.concurrent.TimeUnit;

public class GDriveImportExportActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    protected GoogleApiClient mGoogleApiClient;
    private ProgressDialog exportProgress;
    private static final int EXPORT_ID = 1;

    protected static final int RESOLVE_CONNECTION_REQUEST_CODE = 24;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gdrive_import_export);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.d("SC", "VACAPP connected to GDrive.");
        exportAllFarms();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    protected void onPrepareDialog(int id, Dialog dialog){
        Log.d("SC","Calls on prepare");
        Log.d("SC","creating progress dialog");
        ProgressDialog progress = null;
        int maxProg = 100;
        if(id==EXPORT_ID) {
            progress = exportProgress;
            progress.setMessage("Exportando imagenes.");
            //TODO: Change maxProg
        }
        progress.setCancelable(false);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setProgress(0);
        progress.setMax(maxProg);
        progress.show();

    }

    protected Dialog onCreateDialog(int id)
    {
        Log.d("SC","Calls on create.");
        ProgressDialog progress= null;
        if(id == EXPORT_ID) {
            if (exportProgress == null) exportProgress = new ProgressDialog(this);
        }
        return progress;
    }

    //Creates a folder called "VACAPP" in the root folder of Google Drive. Should only be called if one is sure that there is no folder called VACAPP already.
    private DriveFolder createVacappFolder(){
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("VACAPP").build();
        return Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(mGoogleApiClient,changeSet).await(10,TimeUnit.SECONDS).getDriveFolder();
    }

    //Looks for the VACAPP folder in Google Drive, and creates it in the root folder if it doesn't exist, and returns the VACAPP folder.
    public DriveFolder getVacappFolder(){
        DriveFolder root = Drive.DriveApi.getRootFolder(mGoogleApiClient);
        Query query = new Query.Builder().addFilter(Filters.and(
                Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"),
                Filters.eq(SearchableField.TITLE, "VACAPP"),
                Filters.eq(SearchableField.TRASHED,false))).build();
        MetadataBuffer result = root.queryChildren(mGoogleApiClient,query).await(10, TimeUnit.SECONDS).getMetadataBuffer();
        DriveFolder vacappFolder = null;
        if(result.getCount() == 0){
            //Should create directory
            Log.d("SC", "Should create!");
            vacappFolder = createVacappFolder();
        }
        else{
            Log.d("SC","Should not create!");
            Metadata vacappFolderMetadata = null;
            for(Metadata meta:result){
                if(!meta.isFolder()) continue;
                if(vacappFolderMetadata == null) vacappFolderMetadata = meta;
                else if(meta.getCreatedDate().compareTo(vacappFolderMetadata.getCreatedDate()) > 0) vacappFolderMetadata = meta;
            }
            if(vacappFolderMetadata != null) {
                Log.d("SC","VACAPP FOLDER: "+vacappFolderMetadata.getAlternateLink()+" ");
                vacappFolder = Drive.DriveApi.getFolder(mGoogleApiClient,vacappFolderMetadata.getDriveId());
            }
            else Log.d("SC","Turns out it did have to!");

        }
        if(vacappFolder == null) vacappFolder = createVacappFolder();
        return vacappFolder;
    }

    //Exports all pictures of cows to the VACAPP folder in Google Drive, following the same directory/file naming scheme used for VACAPP.
    public void exportAllFarms(){
        try {
            final DriveFolder rootFolder = Drive.DriveApi.getRootFolder(mGoogleApiClient);
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle("VACAPP").build();
            showDialog(EXPORT_ID);
            Thread t = new Thread(new Runnable()
            {
                public void run()
                {
                    //Find VACAPP folder.
                    DriveFolder vacappFolder = getVacappFolder();
                    exportProgress.incrementProgressBy(1);
                    Log.d("SC", "Gets here succesfully.");
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

    private void BackToMain(View v){
        Intent intent=new Intent(getApplicationContext(),MainActivity.class);
        startActivity(intent);
        finish();
    }
}
