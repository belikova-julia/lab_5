package lab5;

public class StoreMessage {
    private final String url;
    private final float avgTime;

    public StoreMessage(String url, float avgTime) {
        this.url = url;
        this.avgTime = avgTime;
    }

    public float getAvgTime() {
        return avgTime;
    }

    public String getUrl() {
        return url;
    }
}
