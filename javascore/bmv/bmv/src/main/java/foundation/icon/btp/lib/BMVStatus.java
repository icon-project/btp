package foundation.icon.btp.lib;

public class BMVStatus {
    public long height;
    public long offset;
    public long lastHeight;

    public BMVStatus(
        long height,
        long offset,
        long lastHeight
    ) {
        this.height = height;
        this.offset = offset;
        this.lastHeight = lastHeight;
    }
}
