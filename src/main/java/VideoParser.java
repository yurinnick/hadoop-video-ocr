import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class VideoParser {

    private VideoCapture video = null;
    public static final String LOG_PATH_VIDEOPARSER="/tmp/videoparser.txt";
    private int frameFreqNumber = 1;
    private PrintWriter logWriter;

    public VideoParser(File videoFile, File outPath, int frameFreqNumber) throws FileNotFoundException {
        FileOutputStream logStr = new FileOutputStream(LOG_PATH_VIDEOPARSER, false);
        logWriter = new PrintWriter(logStr);
        String videoFilePath = videoFile.getAbsolutePath();
        logWriter.println("open file " + videoFilePath);

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        if(frameFreqNumber > 1) {
            this.frameFreqNumber = frameFreqNumber;
        }
        this.video = new VideoCapture();
        try {
            video.open(videoFilePath);
        }catch (Exception e){
            logWriter.println(e);
        }
        if(!video.isOpened()){
            logWriter.println("can't open video");
            throw new FileNotFoundException(videoFilePath);
        } else {
            logWriter.println("video opened");
            getFrames(outPath.getAbsolutePath());
            video.release();
        }
       // video.release();
        if(video.isOpened()){
            logWriter.println("can't release video");
        } else {
            logWriter.println("video released");
        }
        if (logWriter != null) {
            logWriter.flush();
            logWriter.close();
        }
    }

    public void getFrames(String outPath) {
        int i = 1;
        Mat frame = new Mat();
        int k = 1;
        while(video.read(frame)){
            if(k == frameFreqNumber) {
                logWriter.println("get " + String.format("%08d", i) + " frame");
                String res = outPath + "/" + String.format("%08d", i) + ".jpg";
                Highgui.imwrite(res, frame);
                i++;
                k=0;
            }
            k++;
        }
    }
}
