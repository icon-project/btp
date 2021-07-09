package foundation.icon.btp.bmv.lib.mpt;

@SuppressWarnings("serial")
public class MPTException extends Exception {
    private final String message;

    public MPTException() {
        super();
        this.message = "unknown";
    }

    public MPTException(String message) {
        super();
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

    public String toString() {
        return message;
    }
}