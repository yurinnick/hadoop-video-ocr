import com.github.axet.vget.VGet;
import com.google.common.io.Files;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.opencv.core.Core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;

public class OCRMapReduce {
    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, Text, Text> {
        private Text word = new Text();
        public void map(LongWritable key, Text url, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {

            File videoDownloadDir = Files.createTempDir();
            VGet v = new VGet(new URL(url.toString()), videoDownloadDir);
            v.download();
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            File[] videoFiles = videoDownloadDir.listFiles();
            Arrays.sort(videoFiles);
            File[] videoFramesFiles = VideoProcessing.parseVideo(videoFiles[0], 60);
            File[] processedVideoFrames = VideoProcessing.cutImages(videoFramesFiles);

            Tesseract instance = Tesseract.getInstance();
            instance.setDatapath("/usr/share/tesseract-ocr");
            instance.setTessVariable("LC_NUMERIC", "C");

            for (File image: processedVideoFrames) {
                String result = null;
                try {
                    result = instance.doOCR(image);
                } catch (TesseractException e) {
                    e.printStackTrace();
                }
                if (!result.isEmpty()) {
                    word.set(result);
                    output.collect(url, word);
                }
            }
        }
    }

    public static class Reduce extends MapReduceBase implements Reducer<Text, Text, Text, Text> {
        @Override
        public void reduce(Text url, Iterator<Text> framesTexts, OutputCollector<Text, Text> output, Reporter reporter) throws IOException {
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
