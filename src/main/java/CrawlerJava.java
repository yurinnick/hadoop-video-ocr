

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class CrawlerJava implements AutoCloseable {

    private int videosLeft;
    private WorkItem root;
    private PrintWriter urlWriter;
    private PrintWriter logWriter;
    private boolean disposed;
    // временно, чисто для тестов
    // private List<WorkItem> itemsForSerialization;

    public CrawlerJava(String rootUrl) {
        this.root = new WorkItem();
        this.root.setUrl(rootUrl);
        this.root.setMinutes(1);
        //root.setId(UUID.randomUUID());
        videosLeft = 20;
        disposed = false;
        // itemsForSerialization = new ArrayList<>(videosLeft);
    }

    public void start(String logPath, String outputFile) throws IOException, InterruptedException {
        HashSet<WorkItem> _visited = new HashSet<WorkItem>();
        SortedSet<WorkItem> _toVisit = new TreeSet<WorkItem>();
        _toVisit.add(root);

        FileOutputStream outStr = new FileOutputStream(outputFile, false);
        FileOutputStream logStr = new FileOutputStream(logPath, false);
        urlWriter = new PrintWriter(outStr);
        logWriter = new PrintWriter(logStr);
        while (_toVisit.size() > 0 && videosLeft > 0) {
            try {
                videosLeft--;
                WorkItem nextLink = _toVisit.first();
                _toVisit.remove(nextLink);
                _visited.add(nextLink);
                Connection conn = Jsoup.connect(nextLink.getUrl());

                Connection.Response response = conn.execute();
                Document doc = response.parse();
                List<WorkItem> links = SelectLinksToSimilar(doc);
                for (WorkItem link : links) {
                    if (link.getUrl() == null || link.getUrl().equals("")) {
                        continue;
                    }
                    if (ShouldVisit(link) && !_visited.contains(link)
                            && !_toVisit.contains(link)) {
                        _toVisit.add(link);
                    }
                }
                if (nextLink.getMinutes() < 10) {
                    nextLink.setId(UUID.randomUUID());
                    SendToFile(nextLink);
                }
            } catch (Exception ex) {
                logWriter.println(ex.getMessage() + " " + "в start()");
            }
            Thread.sleep(3000);
        }
    }

    private boolean ShouldVisit(WorkItem link) {
        return link.getMinutes() < 20 && link.getUrl().contains("watch");
    }

    private List<WorkItem> SelectLinksToSimilar(Document doc) {
        Elements recommendedVideos = doc.select("#watch7-sidebar-contents .video-list-item.related-list-item");
        List<WorkItem> links = new ArrayList<WorkItem>(recommendedVideos.size());
        for (Element block : recommendedVideos) {
            Elements minutesElement = block.select(".video-time");
            String addr = block.select("a").first().attr("href");
            WorkItem link = new WorkItem();
            link.setUrl(addr);
            link.setMinutes(GetMinutes(minutesElement));
            links.add(link);
        }
        return links;
    }

    private int GetMinutes(Elements minutesElem) {
        String time = minutesElem.text();
        try {

            if (time.indexOf(':') != time.lastIndexOf(':')) {
                return 100;//не будем это качать
            }
            //есть минуты и секунды
            int lastSemiColon = time.lastIndexOf(':');
            if (lastSemiColon == -1) {
                return 100;
            }
            String minutes = time.substring(0, lastSemiColon);
            return Integer.parseInt(minutes);
        } catch (Exception ex) {
            logWriter.println(ex.getMessage() + " " + "в GetMinutes(). Текст элемента= " + time);
            return 100;
        }
    }

    private void SendToFile(WorkItem nextLink) {
        urlWriter.println(nextLink.getUrl());
        //urlWriter.println(nextLink.getUrl() + " " + nextLink.getMinutes() + " " + nextLink.getId());
        // itemsForSerialization.add(nextLink);
    }

    @Override
    public void close() throws Exception {
        if (!disposed) {
            disposed = true;
            if (urlWriter != null) {
                urlWriter.flush();
                urlWriter.close();
            }
            if (logWriter != null) {
                logWriter.flush();
                logWriter.close();
            }
           /* try (PrintWriter xmlWriter = new PrintWriter("outXml.xml")) {
                XStream xmlStream = new XStream();
                xmlStream.toXML(itemsForSerialization, xmlWriter);
            }*/
        }
    }

}
