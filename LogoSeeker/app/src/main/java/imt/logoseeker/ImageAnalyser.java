package imt.logoseeker;

import android.app.Activity;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.indexer.FloatRawIndexer;
import org.bytedeco.javacpp.opencv_calib3d;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacpp.opencv_core.KeyPointVector;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_features2d;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_shape;
import org.bytedeco.javacpp.opencv_xfeatures2d;

import java.util.ArrayList;

import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_ml.SVM;

class ImageAnalyser {

    private String rootPath;
    private String pathToVocabulary;

    ImageAnalyser(Activity act)
    {
        rootPath = act.getFilesDir().getAbsolutePath();
        pathToVocabulary = rootPath + "/vocabulary.yml" ;
    }

    Brand launchAnalysis(String photoPath, int nbBrands, ArrayList<Brand> brands)
    {
        // Chargement du vocabulary
        Loader.load(opencv_core.class);
        opencv_core.CvFileStorage storage = opencv_core.cvOpenFileStorage(pathToVocabulary, null, opencv_core.CV_STORAGE_READ);
        Pointer p = opencv_core.cvReadByName(storage, null, "vocabulary", opencv_core.cvAttrList());
        @SuppressWarnings("deprecation") CvMat cvMat = new CvMat(p);
        Mat vocabulary = new Mat(cvMat);
        opencv_core.cvReleaseFileStorage(storage);

        //Paramètre du SIFT.create
        int	nFeatures	=	0;
        int	nOctaveLayers	=	3;
        double	contrastThreshold	=	0.03;
        int	edgeThreshold	=	10;
        double	sigma	=	1.6;
        Loader.load(opencv_calib3d.class);
        Loader.load(opencv_shape.class)	;
        opencv_xfeatures2d.SIFT detector = opencv_xfeatures2d.SIFT.create(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma);

        //create a matcher with FlannBased Euclidien distance (possible also with BruteForce-Hamming)
        opencv_features2d.FlannBasedMatcher matcher = new opencv_features2d.FlannBasedMatcher();

        //create BoF (or BoW) descriptor extractor
        opencv_features2d.BOWImgDescriptorExtractor bowide = new opencv_features2d.BOWImgDescriptorExtractor(detector, matcher);

        //Set the dictionary with the vocabulary we created in the first step
        bowide.setVocabulary(vocabulary);

        String[] class_names;
        class_names = new String[nbBrands];

        for(int i =0; i < nbBrands; ++i)
        {
            class_names[i] = brands.get(i).getClassifierName();
        }

        final SVM[] classifiers;
        classifiers = new SVM[nbBrands];
        for (int i = 0; i < nbBrands; i++) {
            classifiers[i] = SVM.create();
            String clPath = rootPath + "/" + class_names[i];
            classifiers[i] = SVM.load(clPath);
        }

        Mat response_hist = new Mat();
        response_hist.convertTo(response_hist,opencv_core.CV_32F);
        KeyPointVector keypoints = new KeyPointVector();

        Mat imageTest = imread(photoPath);
        opencv_imgproc.resize(imageTest, imageTest, new opencv_core.Size(500, 700));
        detector.detect(imageTest,keypoints);
        bowide.compute(imageTest, keypoints, response_hist, new opencv_core.IntVectorVector(), new Mat());

        // Finding best match
        float minf = Float.MAX_VALUE;
        Brand bestMatch = null;

        // loop for all classes
        try {
            for (int i = 0; i < nbBrands; i++) {
                Mat resMat = new Mat();
                // classifier prediction based on reconstructed histogram
                response_hist.convertTo(response_hist,opencv_core.CV_32F);
                float res = classifiers[i].predict(response_hist, resMat, 1);
                //System.out.println(class_names[i] + " is " + res);
                FloatRawIndexer indexer = resMat.createIndexer();
                if (resMat.cols() > 0 && resMat.rows() > 0) {
                    res = indexer.get(0, 0);
                }
                if (res < minf) {
                    minf = res;
                    bestMatch = brands.get(i);
                }
            }
            return bestMatch;
        }
        catch(Exception e)
        {
            return null;
        }
    }

}
