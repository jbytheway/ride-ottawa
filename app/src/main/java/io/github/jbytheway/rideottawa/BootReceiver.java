package io.github.jbytheway.rideottawa;

import android.content.Context;
import android.content.Intent;
import androidx.legacy.content.WakefulBroadcastReceiver;

public class BootReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            return;
        }

        Intent intentToSend = new Intent(context, AlarmService.class);
        intentToSend.putExtra(AlarmService.ACTION, AlarmService.ACTION_CHECK_ALARMS_WAKEFULLY);
        startWakefulService(context, intentToSend);
    }
}