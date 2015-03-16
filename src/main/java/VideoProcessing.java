import com.google.common.io.Files;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.opencv.core.*;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.opencv.imgproc.Imgproc.*;

public final class VideoProcessing {
    public static int MAX_SIZE = 300;
    public static int MIN_SIZE = 30;

    private enum ImageToneType {
        Dark, Light
    }

    public static File[] parseVideo(File inputFile, int frameFrequency)
            throws FileNotFoundException {
        String videoFilePath = inputFile.getAbsolutePath();
        VideoCapture video = new VideoCapture();

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        if(frameFrequency < 1) {
            throw new IllegalArgumentException("frameFreqNumber couldn't be less then 1");
        }
        video.open(videoFilePath);
        File[] frames = getFrames(video, frameFrequency);
        video.release();
        return frames;

    }

    private static File[] getFrames(VideoCapture video, int frameFrequency) {
        int frameNumber = 1;
        Mat frame = new Mat();
        File videoFramesDir = Files.createTempDir();
        while(video.read(frame)){
            if(frameNumber % frameFrequency == 0) {
                String res = videoFramesDir.getAbsolutePath() + "/" +
                        String.format("%08d", frameNumber / frameFrequency) + ".jpg";
                Highgui.imwrite(res, frame);
            }
            frameNumber ++;
        }
        File [] frames = videoFramesDir.listFiles();
        Arrays.sort(frames);
        return frames;
    }

    public static File[] cutImages(File[] imageDirectory)
            throws FileNotFoundException {
        File[] cutImages = null;
        for (File image: imageDirectory) {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            String basename = FilenameUtils.getBaseName(image.getName());
            Mat mat = Highgui.imread(
                    image.getAbsolutePath(),
                    Highgui.CV_LOAD_IMAGE_GRAYSCALE);
            ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            contours.addAll(find(mat, ImageToneType.Dark));
            contours.addAll(find(mat, ImageToneType.Light));
            cutImages = (File[]) ArrayUtils.addAll(
                    cutImages,
                    cutContours(mat, contours, basename));
        }
        Arrays.sort(cutImages);
        return cutImages;
    }

    private static ArrayList<MatOfPoint> find(Mat image, ImageToneType type){
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat locImage = image.clone();
        if (type == ImageToneType.Dark) {
            Imgproc.threshold(locImage, locImage, 150, 250, Imgproc.THRESH_BINARY_INV);
        } else if (type == ImageToneType.Light) {
            Imgproc.threshold(locImage, locImage, 200, 220, Imgproc.THRESH_BINARY);
        }
        Mat kernel = getStructuringElement(MORPH_CROSS, new Size(3, 3));
        Mat dilated = locImage.clone();
        dilate(locImage,dilated,kernel,new Point(-1,-1),13);
        Mat hierarchy = new Mat();
        Imgproc.findContours(dilated, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        return contours;
    }

    private static File[] cutContours(Mat image, ArrayList<MatOfPoint> contours, String name) {
        File cutContoursDir = Files.createTempDir();
        int index = 1;
        for (MatOfPoint c : contours) {
            Rect rect = boundingRect(c);
            if (((rect.height > MAX_SIZE) && (rect.width > MAX_SIZE)) ||
                    ((rect.height < MIN_SIZE) || (rect.width < MIN_SIZE))) {
                continue;
            }
            Mat part_image = new Mat(image, new Rect(rect.x, rect.y, rect.width, rect.height));
            String res = cutContoursDir.getAbsolutePath() + "/" + name + (index++) + ".jpg";
            Highgui.imwrite(res, part_image);
        }
        return cutContoursDir.listFiles();
    }
}
