/**
 * Created by tatyana on 05.12.14.
 */
//import org.omg.CORBA.Environment;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.security.Permission;
import java.util.ArrayList;

import static org.opencv.imgproc.Imgproc.*;

public class ImageCutter {
    private Mat image;
    public static final String LOG_PATH_IMAGECUTTER="/tmp/imagecutter.txt";
    private PrintWriter logWriter;
    public ImageCutter(String imagePath, String outPath) throws FileNotFoundException {
        FileOutputStream logStr = new FileOutputStream(LOG_PATH_IMAGECUTTER, false);
        logWriter = new PrintWriter(logStr);
        System.loadLibrary( Core.NATIVE_LIBRARY_NAME );
        //System.out.println("mat = " + image.dump());
        try {
            this.image = Highgui.imread(imagePath, Highgui.CV_LOAD_IMAGE_GRAYSCALE);
            //System.out.println("mat = " + image.dump());

            ArrayList<MatOfPoint> contours1 = findDark();
            if (contours1 != null) {
                logWriter.println("get dark contours1!");
            } else {
                logWriter.println("false");
            }
            cutContours(contours1, "dark", outPath);

            ArrayList<MatOfPoint> contours2 = findLight();
            if (contours2 != null) {
                logWriter.println("get light contours1!");
            } else {
                logWriter.println("false");
            }
            cutContours(contours2, "light", outPath);
        } catch(Exception e) {
            logWriter.println("can't load image");
        }
        if (logWriter != null) {
            logWriter.flush();
            logWriter.close();
        }
    }

    public ArrayList<MatOfPoint> findDark(){
        ArrayList<MatOfPoint> contours = null;
        try{
            Mat locImage = image.clone();
            //System.out.println("mat = " + locImage.dump());
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
            logWriter.println("exception 1");
        }
        return contours;
    }

    public ArrayList<MatOfPoint> findLight(){
        ArrayList<MatOfPoint> contours = null;
        try{
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

        } catch(Exception e){
            logWriter.println("exception 2");
        }
        return contours;
    }

    public void cutContours(ArrayList<MatOfPoint> contours, String name, String outPath) {
        int i = 1;
        //String p="/home/tatyana/IdeaProjects/TestProject/";
        String p = outPath;
        String e = ".jpg";
        //for each contour found, draw a rectangle around it on original image
        for (MatOfPoint c : contours) {
            //get rectangle bounding contour
            Rect rect = boundingRect(c);

            //discard areas that are too large
            if ((rect.height > 300) && (rect.width > 300)) {
                continue;
            }

            //discard areas that are too small
            if ((rect.height < 30) || (rect.width < 30)) {
                continue;
            }

            //cut rectangles and save in separate images
            Mat part_image = new Mat(image, new Rect(rect.x, rect.y, rect.width, rect.height));
                    //(); [y:y + h, x:x + w]

            String res = p + name + i + e;
            Highgui.imwrite(res, part_image);
            i++;

            //draw rectangle around contour on original image
            //Core.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x+rect.width, rect.y+rect.height), new Scalar(255, 0, 255), 1, 8, 0);
            //cv2.rectangle(image,(x,y),(x+w,y+h),(255,0,255),2)
        }
        /*
        String p="/home/tatyana/IdeaProjects/TestProject/";
        String e = ".jpg";
        String res = p + name + e;
        if(Highgui.imwrite(res, image)==true){
            System.out.println("write countured image");
        } else{
            System.out.println("false");
        }*/
    }
}
