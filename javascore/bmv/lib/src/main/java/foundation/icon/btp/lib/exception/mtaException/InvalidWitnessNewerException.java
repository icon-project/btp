package foundation.icon.btp.lib.exception.mtaException;

@SuppressWarnings("serial")
public class InvalidWitnessNewerException extends MTAException {
    public InvalidWitnessNewerException() {
        super("unknown");
    }

    public InvalidWitnessNewerException(String message) {
        super(message);
    }
}