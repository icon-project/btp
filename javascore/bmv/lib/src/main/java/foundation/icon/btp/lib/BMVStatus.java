package foundation.icon.btp.lib;

public class BMVStatus {
    public long height;
    public long offset;
    public long last_height;

    public BMVStatus(
        long height,
        long offset,
        long lastHeight
    ) {
        this.height = height;
        this.offset = offset;
        this.last_height = lastHeight;
    }
}
