package foundation.icon.btp.lib.exception.mta;

@SuppressWarnings("serial")
public class InvalidWitnessNewerException extends MTAException {
    public InvalidWitnessNewerException() {
        super("unknown");
    }

    public InvalidWitnessNewerException(String message) {
        super(message);
    }
}