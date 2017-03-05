package io.github.jbytheway.rideottawa;

import com.orm.SugarRecord;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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
            // this can happen if the FavouriteStop was deleted while the alarm was active
            return null;
        }

        DateTime midnight = mMidnightFormatter.parseDateTime(TripMidnight).withZoneRetainFields(mOttawaTimeZone);

        return new Alarm(favouriteStop, new TripUid(TripId, midnight), MinutesWarning, listener, this, ocTranspo);
    }

    public long getTimeToCheck() {
        return TimeToCheck;
    }

    public void setTimeToCheck(long timeToCheck) {
        TimeToCheck = timeToCheck;
    }

    private int TripId;
    private String TripMidnight;
    private long StopId;
    private int MinutesWarning;
    private long TimeToCheck;

    private static final DateTimeZone mOttawaTimeZone = DateTimeZone.forID("America/Toronto");
    private static final DateTimeFormatter mMidnightFormatter = DateTimeFormat.forPattern("yyyyMMdd");
}
