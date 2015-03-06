import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

public class VideoParser {
    private VideoCapture video = null;
    public static final String LOG_PATH_VIDEOPARSER="/tmp/videoparser.txt";
    //frames frequency
    private int frameFreqNumber = 1;
    private PrintWriter logWriter;
    public VideoParser(String videoPath, String outPath, int frameFreqNumber) throws FileNotFoundException {
        FileOutputStream logStr = new FileOutputStream(LOG_PATH_VIDEOPARSER, false);
        logWriter = new PrintWriter(logStr);
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        if(frameFreqNumber >1) {
            this.frameFreqNumber = frameFreqNumber;
        }
        this.video = new VideoCapture();
        try {
            video.open(videoPath);
        }catch (Exception e){
            logWriter.println(e);
        }
        if(!video.isOpened()){
            logWriter.println("can't open video");
            throw new FileNotFoundException(videoPath);
        } else {
            logWriter.println("video opened");
            getFrames(outPath);
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
        String p = outPath;
        String e = ".jpg";
        int i=1;
        String name = "";
        Mat frame = new Mat();
        int k=1;
        while(video.read(frame)){
            if(k==frameFreqNumber){
                logWriter.println("get " + i + " frame");
                String res = p + name + i + e;
                Highgui.imwrite(res, frame);
                i++;
                k=0;
            }
            k++;
        }
    }
}
