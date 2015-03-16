/**
 * Created by tatyana on 05.12.14.
 */
//import org.omg.CORBA.Environment;
import org.apache.commons.io.FilenameUtils;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.security.Permission;
import java.util.ArrayList;

import static org.opencv.imgproc.Imgproc.*;

public class ImageCutter {

    public static final String LOG_PATH_IMAGECUTTER = "/tmp/imagecutter.txt";
    private PrintWriter logWriter;
    public static int MAX_SIZE = 300;
    public static int MIN_SIZE = 30;

    public ImageCutter(File imagePath, File outPath) throws FileNotFoundException {
        FileOutputStream logStr = new FileOutputStream(LOG_PATH_IMAGECUTTER, false);
        logWriter = new PrintWriter(logStr);
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        try {
            Mat image = Highgui.imread(imagePath.getAbsolutePath(), Highgui.CV_LOAD_IMAGE_GRAYSCALE);
            ArrayList<MatOfPoint> contours1 = findDark(image);
            if (contours1 != null) {
                logWriter.println("get dark contours1!");
            } else {
                logWriter.println("false");
            }
            String basename = FilenameUtils.getBaseName(imagePath.getName());
            cutContours(image, contours1, basename + "dark", outPath);

            ArrayList<MatOfPoint> contours2 = findLight(image);
            if (contours2 != null) {
                logWriter.println("get light contours1!");
            } else {
                logWriter.println("false");
            }
            cutContours(image, contours2, basename + "light", outPath);
        } catch(Exception e) {
            logWriter.println("can't load image");
        }
        if (logWriter != null) {
            logWriter.flush();
            logWriter.close();
        }
    }



    public ArrayList<MatOfPoint> findDark(Mat image){
        ArrayList<MatOfPoint> contours = null;
        try{
            Mat locImage = image.clone();
            Imgproc.threshold(locImage, locImage, 150, 250, Imgproc.THRESH_BINARY_INV);

            Mat kernel = getStructuringElement(MORPH_CROSS, new Size(3, 3));

            // dilate
            Mat dilated = locImage.clone();
            dilate(locImage,dilated,kernel,new Point(-1,-1),13);

            // finding the contours
            contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(dilated, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        } catch(Exception e){
            logWriter.println("findDark exception");
        }
        return contours;
    }

    public ArrayList<MatOfPoint> findLight(Mat image){
        ArrayList<MatOfPoint> contours = null;
        try {
            Mat locImage = image.clone();
            Imgproc.threshold(locImage, locImage, 200, 220, Imgproc.THRESH_BINARY);

            Mat kernel = getStructuringElement(MORPH_CROSS, new Size(3, 3));

            // dilate
            Mat dilated = locImage.clone();
            dilate(locImage,dilated,kernel,new Point(-1,-1),13);

            // finding the contours
            contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(dilated, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        } catch(Exception e) {
            logWriter.println("findLight exception");
        }
        return contours;
    }

    public void cutContours(Mat image, ArrayList<MatOfPoint> contours, String name, File outPath) {
        int i = 1;
        //for each contour found, draw a rectangle around it on original image
        for (MatOfPoint c : contours) {
            //get rectangle bounding contour
            Rect rect = boundingRect(c);

            if (((rect.height > MAX_SIZE) && (rect.width > MAX_SIZE)) ||
                ((rect.height < MIN_SIZE) || (rect.width < MIN_SIZE))) {
                continue;
            }

            //cut rectangles and save in separate images
            Mat part_image = new Mat(image, new Rect(rect.x, rect.y, rect.width, rect.height));

            String res = outPath.getAbsolutePath() + "/" + name + i + ".jpg";
            Highgui.imwrite(res, part_image);
            i++;
        }
    }
}
