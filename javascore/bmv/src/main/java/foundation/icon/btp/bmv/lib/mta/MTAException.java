package foundation.icon.btp.bmv.lib.mta;

@SuppressWarnings("serial")
public class MTAException extends Exception {
    private final String message;

    public MTAException() {
        super();
        this.message = "unknown";
    }

    public MTAException(String message) {
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