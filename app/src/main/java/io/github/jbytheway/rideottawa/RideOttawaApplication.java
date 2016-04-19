package io.github.jbytheway.rideottawa;

import android.content.Intent;

import com.orm.SugarApp;

import net.danlew.android.joda.JodaTimeAndroid;

public class RideOttawaApplication extends SugarApp {
    public RideOttawaApplication() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
        Intent intentToSend = new Intent(this, AlarmService.class);
        intentToSend.putExtra(AlarmService.ACTION, AlarmService.ACTION_CHECK_ALARMS);
        startService(intentToSend);
    }

    public OcTranspoDataAccess getOcTranspo() {
        if (mOcTranspo == null) {
            mOcTranspo = new OcTranspoDataAccess(this);
        }
        return mOcTranspo;
    }

    private OcTranspoDataAccess mOcTranspo;
}
