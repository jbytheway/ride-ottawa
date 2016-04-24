package io.github.jbytheway.rideottawa.db;

public class StopTime {
    public StopTime(int time, int stopSequence) {
        Time = time;
        StopSequence = stopSequence;
    }

    public final int Time;
    public final int StopSequence;
}
