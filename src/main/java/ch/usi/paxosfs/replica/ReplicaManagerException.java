package ch.usi.paxosfs.replica;

/**
 * Created by pacheco on 26.04.15.
 */
public class ReplicaManagerException extends Exception {
    public ReplicaManagerException() {
        super();
    }

    public ReplicaManagerException(String message) {
        super(message);
    }

    public ReplicaManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReplicaManagerException(Throwable cause) {
        super(cause);
    }
}
