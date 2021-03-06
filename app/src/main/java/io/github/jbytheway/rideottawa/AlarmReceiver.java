package io.github.jbytheway.rideottawa;

import android.content.Context;
import android.content.Intent;
import androidx.legacy.content.WakefulBroadcastReceiver;

public class AlarmReceiver extends WakefulBroadcastReceiver {
    public static final int REQUEST_CHECK_ALARMS = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intentToSend = new Intent(context, AlarmService.class);
        intentToSend.putExtra(AlarmService.ACTION, AlarmService.ACTION_CHECK_ALARMS_WAKEFULLY);
        startWakefulService(context, intentToSend);
    }
}
