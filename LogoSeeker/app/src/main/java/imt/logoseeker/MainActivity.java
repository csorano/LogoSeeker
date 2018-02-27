package imt.logoseeker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    final int REQUEST_TAKE_PHOTO = 1;
    final int RESULT_LOAD_IMAGE=2;

    private String mCurrentPhotoPath;
    private VolleyInterface volley;
    private ImageAnalyser ia;

    //ON CREATE
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creating tools
        String url = "http://www-rech.telecom-lille.fr/freeorb/";
        volley = new VolleyInterface(this,this, url);
        ia = new ImageAnalyser(this);

        Button buttonCap = (Button) findViewById(R.id.b_capture);
        buttonCap.setOnClickListener(v -> {
            TextView textResult = (TextView) findViewById(R.id.t_Result);
            textResult.setText("");
            dispatchTakePictureIntent();
        });

        Button buttonLib = (Button) findViewById(R.id.b_Library);
        buttonLib.setOnClickListener(v -> {
            TextView textResult = (TextView) findViewById(R.id.t_Result);
            textResult.setText("");
            dispatchViewLibrary();
        });

        Button buttonAnalysis = (Button) findViewById(R.id.b_analysis);
        buttonAnalysis.setOnClickListener(v -> {
            updateInterface(R.integer.UI_DISABLED);
            new AnalysisTask().execute(mCurrentPhotoPath);
        });
    }

    // Mise à jour de l'interface
    private void updateInterface(int status)
    {
        ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar);
        Button btnAna = (Button) findViewById(R.id.b_analysis);
        Button btnCap = (Button) findViewById(R.id.b_capture);
        Button btnLib = (Button) findViewById(R.id.b_Library);
        TextView txtResult = (TextView) findViewById(R.id.t_Result);

        if(status == R.integer.UI_DISABLED)
        {
            pb.setVisibility(View.VISIBLE);
            pb.postInvalidate();
            btnAna.setEnabled(false);
            btnAna.setText(R.string.disabled_message);
            btnCap.setEnabled(false);
            btnLib.setEnabled(false);
            txtResult.setText("");
        }
        else if(status == R.integer.UI_ENABLED)
        {
            pb.setVisibility(View.GONE);
            btnAna.setEnabled(true);
            btnCap.setEnabled(true);
            btnLib.setEnabled(true);
            btnAna.setText(R.string.enabled_message);
        }
    }

    //Lancement de l'appareil photo
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.i("createImageFile",ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                try {
                    Uri photoURI = FileProvider.getUriForFile(this, "imt.logoseeker.fileprovider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }
                catch(Exception ex)
                {
                    Log.i("getUriForFile",ex.getMessage());
                }
            }
        }
    }

    //Nommage de la photo
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.FRANCE).format(new Date());
        String imageFileName = "LOGO_" + timeStamp + "_";
        final String appDirectoryName = "LogoSeeker";
        final File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appDirectoryName);
        //noinspection ResultOfMethodCallIgnored
        storageDir.mkdirs();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }


    //Result Activité
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //Activity Appareil Photo
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            ImageView img = (ImageView) findViewById(R.id.v_picture);

            Bitmap imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath,new BitmapFactory.Options());
            img.setImageBitmap(imageBitmap);

            Toast.makeText(MainActivity.this, "Photo Captured", Toast.LENGTH_SHORT).show();

            galleryAddPic();
        }

        //Activity Selection image galery
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK){

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = null;
            if (selectedImage != null) {
                cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
            }
            if (cursor != null) {
                cursor.moveToFirst();

                int columnIndex;
                columnIndex = cursor.getColumnIndex(filePathColumn[0]);

                String picturePath = cursor.getString(columnIndex);
                cursor.close();

                mCurrentPhotoPath = picturePath;

                ImageView img = (ImageView) findViewById(R.id.v_picture);
                img.setImageBitmap(BitmapFactory.decodeFile(picturePath));
            }
        }
    }

    //Ajout de la photo à la galery
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void dispatchViewLibrary() {

        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

/*--------------------------------------------------------------------------------------------*/

    private Brand startRecognitionFromServer(String photoPath)
    {
        return ia.launchAnalysis(photoPath,volley.getNbBrands(),volley.getBrandsFromJson());
    }

    private void goToResultPage(Brand result)
    {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("result", result);
        startActivity(intent);
    }

    @SuppressLint("StaticFieldLeak")
    private class AnalysisTask extends AsyncTask<String, Void, Brand> {
        protected Brand doInBackground(String[] paths){
            return startRecognitionFromServer(paths[0]);
        }

        protected void onPostExecute(Brand result) {
            updateInterface(R.integer.UI_ENABLED);

            if(result == null) {
                TextView textResult = (TextView) findViewById(R.id.t_Result);
                textResult.setText(R.string.error_message);
            }
            else
            {
                // Go to result page
                goToResultPage(result);
            }
        }
    }
}

