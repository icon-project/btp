package foundation.icon.btp.lib.exception.mtaException;

@SuppressWarnings("serial")
public class InvalidWitnessOldException extends MTAException {
    public InvalidWitnessOldException() {
        super("unknown");
    }

    public InvalidWitnessOldException(String message) {
        super(message);
    }
}