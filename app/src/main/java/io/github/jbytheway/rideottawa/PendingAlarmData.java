package io.github.jbytheway.rideottawa;

import com.orm.SugarRecord;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

// This is supposed to encapsulate an Alarm together with the next time we need to check that
// Alarm, but in a form which is suitable for storage in the database.
public class PendingAlarmData extends SugarRecord {
    @SuppressWarnings("unused")
    public PendingAlarmData() {
        // Required for Sugar
    }

    public PendingAlarmData(TripUid tripUid, long stopId, int minutesWarning, long timeToCheck) {
        TripId = tripUid.getTripId();
        TripMidnight = mMidnightFormatter.print(tripUid.getMidnight());
        StopId = stopId;
        MinutesWarning = minutesWarning;
        TimeToCheck = timeToCheck;
    }

    public Alarm makeAlarm(Alarm.OnRefreshedListener listener, OcTranspoDataAccess ocTranspo) {
        FavouriteStop favouriteStop = FavouriteStop.findById(FavouriteStop.class, StopId);

        if (favouriteStop == null) {
            throw new AssertionError("Invalid FavouriteStop id "+StopId);
        }

        DateTime midnight = mMidnightFormatter.parseDateTime(TripMidnight);

        return new Alarm(favouriteStop, new TripUid(TripId, midnight), MinutesWarning, listener, ocTranspo);
    }

    public long getTimeToCheck() {
        return TimeToCheck;
    }

    private int TripId;
    private String TripMidnight;
    private long StopId;
    private int MinutesWarning;
    private long TimeToCheck;

    private static DateTimeFormatter mMidnightFormatter = DateTimeFormat.forPattern("yyyyMMdd");
}
