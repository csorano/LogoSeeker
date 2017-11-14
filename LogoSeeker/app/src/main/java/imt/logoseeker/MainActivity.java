package imt.logoseeker;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.bytedeco.javacpp.opencv_features2d;
import org.bytedeco.javacpp.opencv_core.*;

public class MainActivity extends AppCompatActivity {

    final int REQUEST_TAKE_PHOTO = 1;
    final int RESULT_LOAD_IMAGE=2;
    String mCurrentPhotoPath;

    //Action du bouton capture
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonCap = (Button)findViewById(R.id.b_capture);
        buttonCap.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        Button buttonLib = (Button)findViewById(R.id.b_Library);
        buttonLib.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dispatchViewLibrary();
            }
        });
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
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "LOGO_" + timeStamp + "_";
        final String appDirectoryName = "LogoSeeker";
        final File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), appDirectoryName);
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


    //Visionneur de photo
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            ImageView img = (ImageView) findViewById(R.id.v_picture);

            Bitmap imageBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath,new BitmapFactory.Options());
            img.setImageBitmap(imageBitmap);

            Toast.makeText(MainActivity.this, "Photo Captured", Toast.LENGTH_SHORT).show();

           galleryAddPic();
        }

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK){

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView img = (ImageView) findViewById(R.id.v_picture);
            img.setImageBitmap(BitmapFactory.decodeFile(picturePath));

        }
    }



    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void dispatchViewLibrary(){

        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }


    // Reconnaissance javaCV
    private void startRecognition()
    {
        // Getting image back from Image View
        ImageView img = (ImageView) findViewById(R.id.v_picture);
        Bitmap image = ((BitmapDrawable)img.getDrawable()).getBitmap();

        // Getting paths to reference Images
        ArrayList<Uri> imagesPaths = new ArrayList<Uri>();
        imagesPaths.add(Uri.parse("drawable://" + R.drawable.image1));
        imagesPaths.add(Uri.parse("drawable://" + R.drawable.image2));
        imagesPaths.add(Uri.parse("drawable://" + R.drawable.image3));


    }

    static DMatchVector selectBest(DMatchVector matches, int numberToSelect) {
        DMatch[] sorted = toArray(matches);
        Arrays.sort(sorted, (a, b) -> {
            return a.lessThan(b) ? -1 : 1;
        });
        DMatch[] best = Arrays.copyOf(sorted, numberToSelect);
        return new DMatchVector(best);
    }

    static long getDistanceMoyenne(DMatchVector bestMatches)
    {
        int distanceMoyenne = 0;
        for(int y = 0; y < 25;++y)
        {
            distanceMoyenne += bestMatches.get(y).distance();
        }
        distanceMoyenne = distanceMoyenne / 25;
        return distanceMoyenne;
    }

    static DMatch[] toArray(DMatchVector matches) {
        assert matches.size() <= Integer.MAX_VALUE;
        int n = (int) matches.size();
        // Convert keyPoints to Scala sequence
        DMatch[] result = new DMatch[n];
        for (int i = 0; i < n; i++) {
            result[i] = new DMatch(matches.get(i));
        }
        return result;
    }
}

