package io.github.jbytheway.rideottawa.utils;

import org.joda.time.Interval;
import org.joda.time.ReadableInstant;

public class TimeUtils {
    public static long minutesDifference(ReadableInstant start, ReadableInstant end) {
        // Using Duration.getStandardMinutes rounds towards zero, where we want to
        // round to nearest.  So we get the duration in seconds and do the rounding
        // ourselves.
        long secondsAway;
        if (start.isAfter(end)) {
            Interval intervalToArrival = new Interval(end, start);
            secondsAway = -intervalToArrival.toDuration().getStandardSeconds();
        } else {
            Interval intervalToArrival = new Interval(start, end);
            secondsAway = intervalToArrival.toDuration().getStandardSeconds();
        }
        return Math.round(((double) secondsAway)/60);
    }
}
