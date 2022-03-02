package foundation.icon.btp.lib.exception;

@SuppressWarnings("serial")
public class RelayMessageRLPException extends Exception {
    private final String scope;
    private final String originalError;
    public RelayMessageRLPException() {
        super();
        this.scope = "unknown";
        this.originalError = "unknown";
    }

    public RelayMessageRLPException(String scope, String originalError) {
        this.scope = scope;
        this.originalError = originalError;
    }

    public String getScope() {
        return this.scope;
    }

    public String getOriginalError() {
        return this.originalError;
    }
}