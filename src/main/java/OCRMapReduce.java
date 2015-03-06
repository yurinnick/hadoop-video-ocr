import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;

import com.github.axet.vget.VGet;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapred.*;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class OCRMapReduce {
    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
        public static String download_folder = "/tmp/video/";
        private PrintWriter logWriter;
        private PrintWriter logTess;
        private Text word = new Text();
        //Path download_folder;
        public void map(LongWritable key, Text url, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
            FileOutputStream logStr = new FileOutputStream("/tmp/log_hadoop-123.txt", false);
            logWriter = new PrintWriter(logStr);
            FileOutputStream logOutput = new FileOutputStream("/tmp/output.txt", false);
            logTess = new PrintWriter(logOutput);

            logWriter.println("start");
            LocalFileSystem localFileSystem = FileSystem.getLocal(new Configuration());
            File destFolder = localFileSystem.pathToFile(new Path(download_folder));
            VGet v = new VGet(new URL(url.toString()), destFolder);
            v.download();
            System.out.println("download complete");
            System.out.println("start videoparser");
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

            String[] vid = getFileNameFromFolder(download_folder);
            String path_in_vid = download_folder + vid[0];
            //fullPath + directoryName
            //directory must exist!!
            String path_out_vid = "/tmp/videoFrames/";
            VideoParser vp = new VideoParser(path_in_vid, path_out_vid, 24);
            String[] file = getFileNameFromFolder("/tmp/videoFrames/");
            logWriter.println("videoparser complete");
            logWriter.println("start image cutter");
            //cutting image
            //fullPath + imageName
            String path_in_image;
            for (int i = 0; i < file.length; i++) {
                path_in_image = "/tmp/videoFrames/" + file[i];
                //System.out.println(i);
                //System.out.println(path_in_image);
                //fullPath + directoryName
                //directory must exist!!
                String path_out_image = "/tmp/image/" + i;
                ImageCutter ic = new ImageCutter(path_in_image, path_out_image);
            }
            logWriter.println("image cutter complete");
            logWriter.println("start tesseract");

            Tesseract instance = Tesseract.getInstance();

            instance.setDatapath("/usr/share/tesseract-ocr");
            instance.setTessVariable("LC_NUMERIC", "C");
//            instance.setLanguage("rus");
            logWriter.println("tess conf complete");
            String[] images = getFileNameFromFolder("/tmp/image/");
            File imageFile;
            for (String image: images) {
                imageFile = new File("/tmp/image/" + image);
                try {
                    String result = instance.doOCR(imageFile);
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

            // разбить на кадры
            //цикл по всем картинкам видео
            // сделать распознавание
            // допустим, распозналось абс => word.set("абс");
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

        /*try (CrawlerJava c = new CrawlerJava(ROOT)) {
            c.start(LOG_PATH, PATH_TO_URL_LIST);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }*/
        //Map.download_folder = args[2];
        System.out.println(System.getProperty("java.library.path"));
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
    public static String[] getFileNameFromFolder(String folderName) {
        File folder = new File(folderName);

        File[] listOfFiles = folder.listFiles();
        String[] sb = new String[listOfFiles.length];
        //System.out.println(listOfFiles.length);
        for(int i=0; i<listOfFiles.length; i++) {
            //System.out.println(listOfFiles[i].getName());
            sb[i]=(listOfFiles[i].getName());
        }
        try{
            Arrays.sort(sb, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    //System.out.println(o1);
                    String[] o1_ = o1.split("\\.");
                    //System.out.println(o1_.length);
                    String[] o2_ = o2.split("\\.");
                    if (Integer.parseInt(o1_[0])> Integer.parseInt(o2_[0])){
                        return 1;
                    }else if(Integer.parseInt(o1_[0])< Integer.parseInt(o2_[0])){
                        return -1;
                    }else {
                        return 0;
                    }

                }
            });
        }catch (Exception e){

        }
        return sb;
        //return  listOfFiles;
    }
}
