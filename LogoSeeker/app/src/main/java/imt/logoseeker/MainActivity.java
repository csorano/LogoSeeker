package imt.logoseeker;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.bytedeco.javacpp.FlyCapture2;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_calib3d;
import org.bytedeco.javacpp.opencv_features2d;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_shape;
import org.bytedeco.javacpp.opencv_xfeatures2d;

import static org.bytedeco.javacpp.opencv_core.NORM_L2;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

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

        Button buttonAnalysis = (Button)findViewById(R.id.b_analysis);
        buttonAnalysis.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {startRecognition();}
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

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            mCurrentPhotoPath = picturePath;

            ImageView img = (ImageView) findViewById(R.id.v_picture);
            img.setImageBitmap(BitmapFactory.decodeFile(picturePath));

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

    private void dispatchViewLibrary(){

        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

/*
----------------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------------
 */

    // Reconnaissance javaCV
    private void startRecognition()
    {
        ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar);
        pb.setVisibility(View.VISIBLE);

        Button btn = (Button) findViewById(R.id.b_analysis);
        btn.setEnabled(false);

        Mat	imagePri =	imread(mCurrentPhotoPath);

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String path = storageDir.getAbsolutePath();
        File[] list = storageDir.listFiles();

        //Image Groupe 1
        String cocaPath = list[0].getAbsolutePath();
        Mat refCoca1 = imread(cocaPath + "/coca-1.jpg");
        Mat refCoca2 = imread(cocaPath + "/coca-2.jpg");
        Mat refCoca3 = imread(cocaPath + "/coca-3.jpg");
        Mat[] referencesCoca ={refCoca1 ,refCoca2,refCoca3};

        //Image Groupe 2
        String pepsiPath = list[1].getAbsolutePath();
        Mat refPepsi1 = imread(pepsiPath + "/pepsi-1.jpg");
        Mat refPepsi2 = imread(pepsiPath + "/pepsi-2.jpg");
        Mat refPepsi3 = imread(pepsiPath + "/pepsi-3.jpg");
        Mat[] referencesPepsi ={refPepsi1 ,refPepsi2,refPepsi3};

        //Image Groupe 3
        String spritePath = list[2].getAbsolutePath();
        Mat refSprite1 = imread(spritePath + "/sprite-1.jpg");
        Mat refSprite2 = imread(spritePath + "/sprite-2.jpg");
        Mat refSprite3 = imread(spritePath + "/sprite-3.jpg");
        Mat[] referencesSprite ={refSprite1 ,refSprite2,refSprite3};

        //Création des Descriptors
        Mat descriptorPri = new Mat();
        Mat[] descriptorsCoca = {null,null,null};
        Mat[] descriptorsPepsi = {null,null,null};
        Mat[] descriptorsSprite = {null,null,null};

        //Création des pointeurs
        KeyPointVector	keyPointsPri	=	new	KeyPointVector();
        KeyPointVector[] keyPointsCoca = {null,null,null};
        KeyPointVector[] keyPointsPepsi = {null,null,null};
        KeyPointVector[] keyPointsSprite = {null,null,null};


        //Paramètre du SIFT.create
        int	nFeatures	=	0;
        int	nOctaveLayers	=	3;
        double	contrastThreshold	=	0.03;
        int	edgeThreshold	=	10;
        double	sigma	=	1.6;
        Loader.load(opencv_calib3d.class);
        Loader.load(opencv_shape.class)	;
        opencv_xfeatures2d.SIFT sift;
        sift= opencv_xfeatures2d.SIFT.create(nFeatures,	nOctaveLayers,	contrastThreshold,	edgeThreshold,	sigma);


        //Classe SIFT permettant de calculer les points où il y a des changements de texture, contraste sur l'image
        //Detection de l'image primaire
        sift.detect(imagePri,	keyPointsPri);
        sift.compute(imagePri,keyPointsPri,descriptorPri);

        //Detection des images de la catégorie Coca
        for (int i=0;i<referencesCoca.length;i++){
            descriptorsCoca[i]=new Mat();
            keyPointsCoca[i]=new KeyPointVector();
            sift.detect(referencesCoca[i],	keyPointsCoca[i]);
            sift.compute(referencesCoca[i],keyPointsCoca[i],descriptorsCoca[i]);
        }

        //Detection des images de la catégorie Pepsi
        for (int i=0;i<referencesPepsi.length;i++){
            descriptorsPepsi[i]=new Mat();
            keyPointsPepsi[i]=new KeyPointVector();
            sift.detect(referencesPepsi[i],	keyPointsPepsi[i]);
            sift.compute(referencesPepsi[i],keyPointsPepsi[i],descriptorsPepsi[i]);
        }

        //Detection des images de la catégorie Sprite
        for (int i=0;i<referencesSprite.length;i++){
            descriptorsSprite[i]=new Mat();
            keyPointsSprite[i]=new KeyPointVector();
            sift.detect(referencesSprite[i],keyPointsSprite[i]);
            sift.compute(referencesSprite[i],keyPointsSprite[i],descriptorsSprite[i]);
        }


        opencv_features2d.BFMatcher matcher	=	new opencv_features2d.BFMatcher(NORM_L2,	false);
        DMatchVector	matches	=	new	DMatchVector();
        DMatchVector bestMatches = new DMatchVector();


        //Creer les matchs entre l'image Primaire et les autres images.Chaque match à une distance.On calculera la diastance moyenne.
        //On retourne la distance moyenne entre l'image primaire et chaque images de la classe Coca
        float[] DM1 = new float[3];
        for (int i=0;i<referencesCoca.length;i++){

            matcher.match(descriptorPri,	descriptorsCoca[i],	matches);
            bestMatches = selectBest (matches,25);
            //drawMatches(imagePri,	keyPointsPri,	referencesCoca[i], keyPointsCoca[i], bestMatches,	imageMatches);
            DM1[i]=distanceMoyenne(bestMatches);
        }

        //On retourne la distance moyenne entre l'image primaire et chaque images de la classe Pepsi
        float[] DM2 = new float[3];
        for (int i=0;i<referencesPepsi.length;i++){

            matcher.match(descriptorPri,	descriptorsPepsi[i],	matches);
            bestMatches = selectBest (matches,25);
            //drawMatches(imagePri,	keyPointsPri,	descriptorsPepsi[i], keyPointsPepsi[i], bestMatches,	imageMatches);
            DM2[i]=distanceMoyenne(bestMatches);
        }

        //On retourne la distance moyenne entre l'image primaire et chaque images de la classe Sprite
        float[] DM3 = new float[3];
        for (int i=0;i<referencesSprite.length;i++){

            matcher.match(descriptorPri,descriptorsSprite[i],	matches);
            bestMatches = selectBest (matches,25);
            //drawMatches(imagePri,	keyPointsPri,	descriptorsSprite[i], keyPointsSprite[i], bestMatches,	imageMatches);
            DM3[i]=distanceMoyenne(bestMatches);
        }

        //On calcul la distance moyenne total de chaque classe

        float DMCoca=(DM1[0]+DM1[1]+DM1[2])/3;
        float DMPepsi=(DM2[0]+DM2[1]+DM2[2])/3;
        float DMSprite=(DM3[0]+DM3[1]+DM3[2])/3	;

        //On affiche le nom de la marque ressamblant à l'image primaire
        if (DMCoca<DMPepsi && DMCoca<DMSprite){

            Toast.makeText(MainActivity.this, "Classe Coca", Toast.LENGTH_SHORT).show();
        }

        else if (DMPepsi<DMCoca && DMPepsi<DMSprite){

            Toast.makeText(MainActivity.this, "classe Pepsi", Toast.LENGTH_SHORT).show();
        }

        else if (DMSprite<DMCoca && DMSprite<DMPepsi){

            Toast.makeText(MainActivity.this, "classe Sprite", Toast.LENGTH_SHORT).show();
        }

        pb.setVisibility(View.INVISIBLE);
        btn.setEnabled(true);
    }

    static DMatchVector	selectBest(DMatchVector	matches,	int numberToSelect)	{
        DMatch[]	sorted =	toArray(matches);
        Arrays.sort(sorted,	(a,	b)	->	{
            return a.lessThan(b)	?	-1	:	1;
        });
        DMatch[]	best =	Arrays.copyOf(sorted,	numberToSelect);
        return new DMatchVector(best);
    }


    static DMatch[]	toArray(DMatchVector	matches)	{
        assert	matches.size()	<=	Integer.MAX_VALUE;
        int	n	=	(int)	matches.size();


        //	Convert	keyPoints	to	Scala	sequence
        DMatch[]	result	=	new	DMatch[n];
        for	(int	i	=	0;	i	<	n;	i++)	{
            result[i]	=	new	DMatch(matches.get(i));
        }
        return	result;
    }

    //Methode pour calculer la distance moyenne des "numberToSelect" matcher
    static float distanceMoyenne(DMatchVector bestMatches){

        float DM=0.0f;
        for(int i=0;i<bestMatches.size();i++){

            DM+=bestMatches.get(i).distance();
        }

        DM=DM/bestMatches.size();
        return DM;

    }



    /*static DMatchVector selectBest(DMatchVector matches, int numberToSelect) {
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
    }*/
}

