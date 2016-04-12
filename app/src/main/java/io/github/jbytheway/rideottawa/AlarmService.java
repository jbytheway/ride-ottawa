package io.github.jbytheway.rideottawa;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AlarmService extends IntentService {
    private static final String TAG = "AlarmService";
    public static final String TRIP_UID = "trip_uid";
    public static final String FAVOURITE_STOP_ID = "favourite_stop_id";
    public static final String MINUTES_WARNING = "minutes_warning";

    public AlarmService() {
        super("AlarmService");

        mExecutor = new ScheduledThreadPoolExecutor(1);
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
        DateTime timeForAlarm = alarm.getTime();
        DateTime now = mOcTranspo.getNow();
        Log.d(TAG, "processAlarm now="+now+", timeForAlarm="+timeForAlarm);
        if (now.isAfter(timeForAlarm)) {
            triggerAlarm(alarm);
            return;
        }
        Duration timeToWait = new Interval(now, timeForAlarm).toDuration();
        long secondsUntilAlarm = timeToWait.getStandardSeconds();
        // If under a minute away, wait until alarm should trigger
        // If more than 10 minutes away, wait until 10 minutes away
        // Otherwise, wait a minute
        long secondsUntilNextCheck = Math.min(secondsUntilAlarm, Math.max(secondsUntilAlarm - 600, 60));
        Log.d(TAG, "processAlarm: secondsUntilNextCheck = " + secondsUntilNextCheck);
        mExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                alarm.refreshTimeEstimate(AlarmService.this, mOcTranspo);
            }
        }, secondsUntilNextCheck, TimeUnit.SECONDS);
    }

    private void triggerAlarm(Alarm alarm) {
        Log.i(TAG, "ALARM ALARM");
    }

    private OcTranspoDataAccess mOcTranspo;
    private ScheduledThreadPoolExecutor mExecutor;
}
