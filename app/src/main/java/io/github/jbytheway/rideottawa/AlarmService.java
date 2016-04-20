package io.github.jbytheway.rideottawa;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

import io.github.jbytheway.rideottawa.ui.ViewFavouriteActivity;
import io.github.jbytheway.rideottawa.utils.TimeUtils;

public class AlarmService extends IntentService {
    private static final String TAG = "AlarmService";
    public static final String ACTION = "action";
    public static final String TRIP_UID = "trip_uid";
    public static final String FAVOURITE_STOP_ID = "favourite_stop_id";
    public static final String MINUTES_WARNING = "minutes_warning";

    public static final int ACTION_NEW_ALARM = 1;
    public static final int ACTION_CHECK_ALARMS = 2;
    public static final int ACTION_CHECK_ALARMS_WAKEFULLY = 3;

    private static final int ALARM_NOTIFICATION_ID = 1;
    private static final long[] VIBRATION_PATTERN = new long[]{0, 300, 200, 300};

    private static final int SECONDS_IN_ADVANCE_TO_CHECK = 600;

    public AlarmService() {
        super("AlarmService");

        mTimeFormatter = DateTimeFormat.forPattern("HH:mm");
        mListener = new Alarm.OnRefreshedListener() {
            @Override
            public void onRefreshed(Alarm alarm) {
                processAlarm(alarm);
            }
        };
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent");
        if (mOcTranspo == null) {
            // Application object not accessible from here, so we must make our own OcTranspo
            mOcTranspo = new OcTranspoDataAccess(this);
        }
        int action = intent.getIntExtra(ACTION, -1);

        switch (action) {
            case ACTION_NEW_ALARM:
                TripUid tripUid = intent.getParcelableExtra(TRIP_UID);
                long stopId = intent.getLongExtra(FAVOURITE_STOP_ID, -1);
                int minutesWarning = intent.getIntExtra(MINUTES_WARNING, 0);
                FavouriteStop favouriteStop = FavouriteStop.findById(FavouriteStop.class, stopId);

                if (favouriteStop == null) {
                    throw new AssertionError("Invalid FavouriteStop id "+stopId);
                }

                new Alarm(
                        favouriteStop, tripUid, minutesWarning,
                        mListener,
                        mOcTranspo
                ).refreshTimeEstimate(this, mOcTranspo);
                break;
            case ACTION_CHECK_ALARMS:
                checkAlarms();
                break;
            case ACTION_CHECK_ALARMS_WAKEFULLY:
                checkAlarms();
                // In this case we are called from a WakefulBroadcastReceiver, so we must release
                // the wake lock
                AlarmReceiver.completeWakefulIntent(intent);
                break;
            default:
                throw new AssertionError("Unexpected ACTION "+action);
        }
        mOcTranspo.closeDatabase();
    }

    private PendingAlarmData getNextAlarmData() {
        List<PendingAlarmData> nextAlarmList = PendingAlarmData.find(PendingAlarmData.class, null, null, null, "time_to_check", "1");
        if (nextAlarmList.isEmpty()) {
            return null;
        }
        return nextAlarmList.get(0);
    }

    private void processAlarm(final Alarm alarm) {
        DateTime timeForAlarm = alarm.getTimeOfAlarm();
        DateTime now = mOcTranspo.getNow();
        Log.d(TAG, "processAlarm now=" + now + ", timeForAlarm=" + timeForAlarm);
        if (now.isAfter(timeForAlarm)) {
            triggerAlarm(alarm);
        } else {
            Duration timeToWait = new Interval(now, timeForAlarm).toDuration();
            long secondsUntilAlarm = timeToWait.getStandardSeconds();
            // Always wait at least 15 seconds
            secondsUntilAlarm = Math.max(secondsUntilAlarm, 15);
            // If under a minute away, wait until alarm should trigger
            // If more than 10 minutes away, wait until 10 minutes away
            // Otherwise, wait a minute
            long secondsUntilNextCheck = Math.min(secondsUntilAlarm, Math.max(secondsUntilAlarm - SECONDS_IN_ADVANCE_TO_CHECK, 60));
            Log.d(TAG, "processAlarm: secondsUntilNextCheck = " + secondsUntilNextCheck);
            DateTime timeToCheck = now.plusSeconds((int) secondsUntilNextCheck);
            long millisToCheck = timeToCheck.getMillis();

            // Save the pending Alarm data so that it can be loaded later when we get woken up
            alarm.makePendingAlarmData(millisToCheck).save();
        }

        // Now we need to find the next PendingAlarmData (which may or may not be the same one) and
        // set up the Alarm (in the android sense) to wake up the phone and check it.
        PendingAlarmData nextAlarm = getNextAlarmData();
        if (nextAlarm == null) {
            // This happens when we just triggered the last alarm
            return;
        }
        long nextTimeToCheck = nextAlarm.getTimeToCheck();

        // We need to trigger this event even in the case where the phone is asleep, so we must use an alarm for this
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, AlarmReceiver.REQUEST_CHECK_ALARMS, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTimeToCheck, pendingIntent);
    }

    private void checkAlarms() {
        // This is called from AlarmReceiver when the Intent set up through the AlarmManager above fires
        // We need to load all alarms that are within the check interval, update, and process them.

        DateTime now = mOcTranspo.getNow();
        DateTime checkThreshold = now.plusSeconds(SECONDS_IN_ADVANCE_TO_CHECK);
        long checkThresholdMillis = checkThreshold.getMillis();

        List<PendingAlarmData> nextAlarmsList = PendingAlarmData.find(PendingAlarmData.class, null, null, null, "time_to_check", "1");
        for (PendingAlarmData alarmData : nextAlarmsList) {
            if (alarmData.getTimeToCheck() > checkThresholdMillis) {
                // This alarm is far enough in the future we don't need to worry yet
                break;
            }
            Alarm nextAlarm = alarmData.makeAlarm(mListener, mOcTranspo);
            alarmData.delete();
            if (nextAlarm != null) {
                nextAlarm.refreshTimeEstimate(this, mOcTranspo);
            }
        }
    }

    private void triggerAlarm(Alarm alarm) {
        Log.i(TAG, "ALARM ALARM");

        DateTime timeOfBus = alarm.getTimeOfBus();
        DateTime now = mOcTranspo.getNow();
        String routeName = alarm.getRoute().getName();
        int minutesDifference = (int)TimeUtils.minutesDifference(now, timeOfBus);
        String busTimeFormatted = mTimeFormatter.print(timeOfBus);

        String minutesString = getResources().getQuantityString(R.plurals.minute_plural, minutesDifference, minutesDifference);
        String title = getString(R.string.alarm_notification_title, routeName, minutesString);
        String text = getString(R.string.alarm_notification_text, routeName, alarm.getStop().getName(this), busTimeFormatted);

        Intent resultIntent = new Intent(this, ViewFavouriteActivity.class);
        resultIntent.putExtra(ViewFavouriteActivity.FAVOURITE_ID, alarm.getFavourite().getId());
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ViewFavouriteActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder
                .setSmallIcon(R.drawable.alarm_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setCategory(Notification.CATEGORY_ALARM)
                .setWhen(alarm.getTimeOfBus().getMillis())
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setSound(sound)
                .setVibrate(VIBRATION_PATTERN)
                .setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(ALARM_NOTIFICATION_ID, builder.build());
    }

    private OcTranspoDataAccess mOcTranspo;
    private final DateTimeFormatter mTimeFormatter;
    private final Alarm.OnRefreshedListener mListener;
}
