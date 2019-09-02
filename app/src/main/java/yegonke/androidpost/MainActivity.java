package yegonke.androidpost;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private String cameraFilePath;
    private static final int CAMERA_REQUEST_CODE =123 ;
    private ImageView imageView;
    private Button button;
    private String SERVER_URL = "https://yegon.pythonanywhere.com/api/upload/";
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.capturedImage);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureFromCamera();
            }
        });
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        //This is the directory in which the file will be created. This is the default location of Camera photos
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for using again
        cameraFilePath = image.getAbsolutePath();
        return image;
    }

    private void captureFromCamera() {
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", createImageFile()));
            startActivityForResult(intent, CAMERA_REQUEST_CODE);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void onActivityResult(int requestCode,int resultCode,Intent data){
        // Result code is RESULT_OK only if the user captures an Image
        if (resultCode == Activity.RESULT_OK)
            switch (requestCode){
                case CAMERA_REQUEST_CODE:
                    imageView.setImageURI(Uri.parse(cameraFilePath));
                    button.setText(cameraFilePath);
                    Toast.makeText(getApplicationContext(),"Attempting upload...",Toast.LENGTH_SHORT).show();
                    new UploadImageTask().execute(cameraFilePath);
                    break;
            }
    }


    private class UploadImageTask extends AsyncTask<String, Void, String> {
        private final String U_TAG = UploadImageTask.class.getSimpleName();


        @Override
        protected String doInBackground(String... paths) {
            try {
                int resp = uploadFile(paths[0]);
                return "Server response " + resp;
            } catch (Exception e) {
                return "Unable to upload image";
            }
        }
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(),"Uploaded...",Toast.LENGTH_SHORT).show();
        }

        public int uploadFile(final String selectedFilePath) {


            int serverResponseCode = 0;

            HttpURLConnection connection;
            DataOutputStream dataOutputStream;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "921b1508a0b342f5bb06dfa40ae1f55d";


            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            File selectedFile = new File(selectedFilePath);


            String[] parts = selectedFilePath.split("/");
            final String fileName = parts[parts.length - 1];

            if (!selectedFile.isFile()) {

                Log.e(U_TAG,"Source File Doesn't Exist: " + selectedFilePath);
                return 0;
            } else {
                try {
                    FileInputStream fileInputStream = new FileInputStream(selectedFile);
                    URL url = new URL(SERVER_URL);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);//Allow Inputs
                    connection.setDoOutput(true);//Allow Outputs
                    connection.setUseCaches(false);//Don't use a cached Copy
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Connection", "Keep-Alive");
                    connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                    connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    connection.setRequestProperty("uploaded_file", selectedFilePath);

                    //creating new dataoutputstream
                    dataOutputStream = new DataOutputStream(connection.getOutputStream());

                    /**
                     * SENDING FILE FROM BODY USING POST AND OPENCONNECTION METHOD
                     */

//      ============================================START CONTENT WRAPPER===============================================


//      ============================================WRITE FILE===============================================

                    //writing bytes to data outputstream
                    dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                    dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\""
                            + fileName + "\"" + lineEnd);
                    dataOutputStream.writeBytes("Content-Type: */*" + lineEnd);

                    dataOutputStream.writeBytes(lineEnd);

                    //returns no. of bytes present in fileInputStream
                    bytesAvailable = fileInputStream.available();
                    //selecting the buffer size as minimum of available bytes or 1 MB
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    //setting the buffer as byte array of size of bufferSize
                    buffer = new byte[bufferSize];

                    //reads bytes from FileInputStream(from 0th index of buffer to buffersize)
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);


                    //loop repeats till bytesRead = -1, i.e., no bytes are left to read
                    while (bytesRead > 0) {

                        try {

                            //write the bytes read from inputstream
                            dataOutputStream.write(buffer, 0, bufferSize);
                        } catch (OutOfMemoryError e) {
                            Toast.makeText(MainActivity.this, "Insufficient Memory!", Toast.LENGTH_SHORT).show();
                        }
                        bytesAvailable = fileInputStream.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                    }

                    dataOutputStream.writeBytes(lineEnd);
//      ============================================END WRITE FILE===============================================
                    /**
                     * SENDING PLAIN TEXT FROM BODY USING POST AND OPENCONNECTION METHOD
                     */
                    //text upload
                    String CATEGORY = "TERROR ATTACK";
                    String TITLE    = "Dusit Attack";
                    String DESCRIPTION = "The 2019 DusitD2 complex attack was a terrorist attack that occurred from 15 to 16 January 2019 in the Westlands area of Nairobi, Kenya, which left more than 20 people dead.";
                    String LOCATION = "1.2725\\u00B0 S, 36.8398\\u00B0 E"; //escaped format of "1.2725° S, 36.8398° E"

//      ============================================WRITE CATEGORY===============================================
                    dataOutputStream.writeBytes(twoHyphens+ boundary + lineEnd);
                    dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"category\";" + lineEnd);
                    dataOutputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                    dataOutputStream.writeBytes(lineEnd + CATEGORY + lineEnd);
                    dataOutputStream.writeBytes(lineEnd);

//      ============================================WRITE TITLE===============================================
                    dataOutputStream.writeBytes(twoHyphens+ boundary + lineEnd);
                    dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"title\";" + lineEnd);
                    dataOutputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                    dataOutputStream.writeBytes(lineEnd + TITLE + lineEnd);
                    dataOutputStream.writeBytes(lineEnd);

//      ============================================WRITE DESCRIPTION===============================================
                    dataOutputStream.writeBytes(twoHyphens+ boundary + lineEnd);
                    dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"description\";" + lineEnd);
                    dataOutputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                    dataOutputStream.writeBytes(lineEnd + DESCRIPTION + lineEnd);
                    dataOutputStream.writeBytes(lineEnd);

//      ============================================WRITE LOCATION===============================================
                    dataOutputStream.writeBytes(twoHyphens+ boundary + lineEnd);
                    dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"location\";" + lineEnd);
                    dataOutputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                    dataOutputStream.writeBytes(lineEnd + LOCATION + lineEnd);
                    dataOutputStream.writeBytes(lineEnd);

//      ============================================END CONTENT WRAPPER===============================================
                    dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                    try {
                        serverResponseCode = connection.getResponseCode();
                    } catch (OutOfMemoryError e) {
                        Toast.makeText(MainActivity.this, "Memory Insufficient!", Toast.LENGTH_SHORT).show();
                    }
                    String serverResponseMessage = connection.getResponseMessage();

                    Log.i(TAG, "Server Response is: " + serverResponseMessage + ": " + serverResponseCode);

                    //response code of 200 indicates the server status OK
                    if (serverResponseCode == 200) {
                        Log.e(U_TAG,"File Upload completed.\n\n " + fileName);
                    }

                    //closing the input and output streams
                    fileInputStream.close();
                    dataOutputStream.flush();
                    dataOutputStream.close();


                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.e(U_TAG,"File Not Found");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    Log.e(U_TAG,"URL Error!");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(U_TAG,"Cannot Read/Write File");
                }
                //dialog.dismiss();
                return serverResponseCode;
            }

        }
    }

}



