

import java.util.UUID;

public class WorkItem implements Comparable {
    private String url;
    private int minutes;
    private UUID id;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (!url.startsWith("https://www.youtube.com")) {
            url = "https://www.youtube.com" + url;
        }
        this.url = url;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public int compareTo(Object o) {
        WorkItem x = (WorkItem) o;
        return minutes - x.minutes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkItem)) return false;

        WorkItem link = (WorkItem) o;

        return (minutes != link.minutes || !url.equals(link.url));
    }

    @Override
    public int hashCode() {
        int result = url.hashCode();
        result = 31 * result + minutes;
        return result;
    }
}