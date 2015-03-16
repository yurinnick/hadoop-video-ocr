import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;

import com.github.axet.vget.VGet;
import com.google.common.io.Files;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.opencv.core.Core;

public class OCRMapReduce {
    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
        public static String download_folder = "/tmp/video/";
        private PrintWriter logWriter;
        private PrintWriter logTess;
        private Text word = new Text();
        //Path download_folder;
        public void map(LongWritable key, Text url, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            File videoDownloadDir = Files.createTempDir();
            File videoFramesDir = Files.createTempDir();
            File processedVideoFramesDir = Files.createTempDir();

            FileOutputStream logStr = new FileOutputStream("/tmp/log_hadoop-123.txt", false);
            FileOutputStream logOutput = new FileOutputStream("/tmp/output.txt", false);
            logTess = new PrintWriter(logOutput);
            logWriter = new PrintWriter(logStr);


            VGet v = new VGet(new URL(url.toString()), videoDownloadDir);
            v.download();
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);


            File[] videoFiles = videoDownloadDir.listFiles();
            Arrays.sort(videoFiles);
            logWriter.println("start proccessing: " + videoFiles[0].getAbsolutePath());
            VideoParser vp = new VideoParser(
                    videoFiles[0],
                    videoFramesDir,
                    24);

            logWriter.println("videoparser complete");
            logWriter.println("start image cutter");

            File[] videoFramesFiles = videoFramesDir.listFiles();
            Arrays.sort(videoFramesFiles);
            for (File img: videoFramesFiles) {
                logWriter.println("cut image " + img.getAbsolutePath());
                ImageCutter ic = new ImageCutter(img, processedVideoFramesDir);
            }

            for (String name: processedVideoFramesDir.list()) {
                logWriter.println(name);
            }

            logWriter.println("image cutter complete");
            logWriter.println("start tesseract");

            Tesseract instance = Tesseract.getInstance();

            instance.setDatapath("/usr/share/tesseract-ocr");
            instance.setTessVariable("LC_NUMERIC", "C");
//            instance.setLanguage("rus");
            logWriter.println("tess conf complete");
            File[] processedVideoFramesFiles = processedVideoFramesDir.listFiles();
            Arrays.sort(processedVideoFramesFiles);
            for (File image: processedVideoFramesFiles) {
                try {
                    logWriter.println("OCR image" + image.getAbsolutePath());
                    String result = instance.doOCR(image);
                    if (!result.isEmpty()) {
                        // Remove empty lines from text
                        word.set(result.replaceAll("(?m)^[ \t]*\r?\n", ""));
                        output.collect(url, word);
                        logTess.println(result);
                    }
                } catch (TesseractException e) {
                    logWriter.println(e.getMessage());
                }
            }
            if (logWriter != null) {
                logWriter.flush();
                logWriter.close();
            }
            if (logTess != null) {
                logTess.flush();
                logTess.close();
            }
        }
    }

    public static class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
        // private static HashMap<UUID, List<Text>> text = new HashMap<>();

        @Override
        public void reduce(Text url, Iterator<Text> framesTexts, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            //List<Text> found = new ArrayList<>();//text.get(url);
            String result = new String();
            while (framesTexts.hasNext()) {
                result += framesTexts.next().toString() + '\n';
            }
            output.collect(url, new Text(result));
        }
    }

    public static void main(String[] args) throws Exception {
        JobConf conf = new JobConf(OCRMapReduce.class);
        conf.setJobName("downloader");

        conf.setOutputKeyClass(Text.class);
        conf.setOutputValueClass(Text.class);

        conf.setMapperClass(Map.class);
        conf.setCombinerClass(Reduce.class);
        conf.setReducerClass(Reduce.class);

        conf.setInputFormat(TextInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        System.out.println(args[0]);
        System.out.println(args[1]);
        FileInputFormat.setInputPaths(conf, new Path(args[0]));
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        JobClient.runJob(conf);
    }
}
