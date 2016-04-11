package io.github.jbytheway.rideottawa;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

public class AlarmService extends IntentService {
    private static final String TAG = "AlarmService";
    public static final String TRIP_UID = "trip_uid";
    public static final String FAVOURITE_STOP_ID = "favourite_stop_id";
    public static final String MINUTES_WARNING = "minutes_warning";

    public AlarmService() {
        super("AlarmService");

        mHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (mOcTranspo == null) {
            // Application object not accessible from here, so we must make our own OcTranspo
            mOcTranspo = new OcTranspoDataAccess(this);
        }
        TripUid tripUid = intent.getParcelableExtra(TRIP_UID);
        long stopId = intent.getLongExtra(FAVOURITE_STOP_ID, -1);
        int minutesWarning = intent.getIntExtra(MINUTES_WARNING, 0);
        FavouriteStop favouriteStop = FavouriteStop.findById(FavouriteStop.class, stopId);

        if (favouriteStop == null) {
            throw new AssertionError("Invalid FavouriteStop id "+stopId);
        }

        new Alarm(
                favouriteStop, tripUid, minutesWarning,
                new Alarm.OnRefreshedListener() {
                    @Override
                    public void onRefreshed(Alarm alarm) {
                        processAlarm(alarm);
                    }
                },
                mOcTranspo
        ).refreshTimeEstimate(this, mOcTranspo);
    }

    private void processAlarm(final Alarm alarm) {
        Log.d(TAG, "processAlarm");
        DateTime timeForAlarm = alarm.getTime();
        DateTime now = mOcTranspo.getNow();
        if (now.isAfter(timeForAlarm)) {
            triggerAlarm(alarm);
            return;
        }
        Duration timeToWait = new Interval(now, timeForAlarm).toDuration();
        long minutesUntilAlarm = timeToWait.getStandardMinutes();
        long minutesUntilNextCheck = Math.max(minutesUntilAlarm - 10, 1);
        Log.d(TAG, "processAlarm: minutesUntilNextCheck = "+minutesUntilNextCheck);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                alarm.refreshTimeEstimate(AlarmService.this, mOcTranspo);
            }
        }, minutesUntilNextCheck * 60 * 1000);
    }

    private void triggerAlarm(Alarm alarm) {
        Log.i(TAG, "ALARM ALARM");
    }

    private OcTranspoDataAccess mOcTranspo;
    private Handler mHandler;
}
