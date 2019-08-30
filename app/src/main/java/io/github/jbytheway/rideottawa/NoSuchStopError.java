package io.github.jbytheway.rideottawa;

public class NoSuchStopError extends Exception {
    public NoSuchStopError(String message) {
        super(message);
    }
}
