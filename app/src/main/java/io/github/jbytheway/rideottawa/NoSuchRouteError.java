package io.github.jbytheway.rideottawa;

public class NoSuchRouteError extends Exception {
    public NoSuchRouteError(String message) {
        super(message);
    }
}
