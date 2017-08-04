package com.kehinde.amazons3;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.vansuita.pickimage.bean.PickResult;
import com.vansuita.pickimage.bundle.PickSetup;
import com.vansuita.pickimage.dialog.PickImageDialog;
import com.vansuita.pickimage.listeners.IPickResult;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 0;
    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;
    private File file;
    private String bucket = "emmatestbucket2";
    private ProgressDialog progressDialog;
    private String filename = "testfile.jpg";
    File downloadFromS3 = new File("/storage/emulated/0/testfile.jpg");
    private String picture_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        progressDialog=new ProgressDialog(this);
//        progressDialog.setIndeterminate(true);
//        progressDialog.setCancelable(false);

        s3credentialsProvider();
        setTransferUtility();
    }

    public void s3credentialsProvider(){

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "[Identity pool ID]", // Identity pool ID
                Regions.US_EAST_1 // Region
        );
        setAmazonS3Client(credentialsProvider);
    }

    public void setAmazonS3Client(CognitoCachingCredentialsProvider credentialsProvider){

        // Create an S3 client
        s3Client = new AmazonS3Client(credentialsProvider);

        // Set the region of your S3 bucket
        s3Client.setRegion(Region.getRegion(Regions.US_EAST_1));
    }

    public void setTransferUtility(){

        transferUtility = new TransferUtility(s3Client,
                getApplicationContext());
    }

    public void uploadFileToS3(View view) {
        showFileChooser();
    }

    private void showFileChooser() {

        PickSetup setup = new PickSetup()
                .setSystemDialog(true);

        PickImageDialog.build(setup)
                .setOnPickResult(new IPickResult() {
                    @Override
                    public void onPickResult(PickResult r) {
                        picture_url=r.getPath();
                        if (picture_url!=null){
                            File file=new File(picture_url);
                            uploadFile(file);
                        }
                    }
                }).show(this);
    }

    private void uploadFile(File file) {
        if (file!=null) {
            progressDialog=new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
            TransferObserver transferObserver = transferUtility.upload(
                    bucket,          /* The bucket to upload to */
                    filename,/* The key for the uploaded object */
                    file       /* The file where the data to upload exists */
            );

            transferObserverListener(transferObserver);
        }
    }

    public void downloadFileFromS3(View view) {
        progressDialog=new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
        TransferObserver transferObserver = transferUtility.download(
                bucket,     /* The bucket to download from */
                filename,    /* The key for the object to download */
                downloadFromS3        /* The file to download the object to */
        );
        transferObserverListener(transferObserver);
    }

    public void transferObserverListener(TransferObserver transferObserver){

        transferObserver.setTransferListener(new TransferListener(){

            @Override
            public void onStateChanged(int id, TransferState state) {
                Toast.makeText(getApplicationContext(), "State Change" + state,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                if (bytesTotal!=0) {
                    int percentage = (int) (bytesCurrent / bytesTotal * 100);
//                Toast.makeText(getApplicationContext(), "Progress in %" + percentage,
//                        Toast.LENGTH_SHORT).show();
                    progressDialog.setMessage(percentage + "% completed");
                    if (percentage == 100) {
                        progressDialog.hide();
                        Toast.makeText(MainActivity.this, "Successful", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(int id, Exception ex) {
                Log.e("error","error");
                progressDialog.hide();
            }

        });
    }
}
