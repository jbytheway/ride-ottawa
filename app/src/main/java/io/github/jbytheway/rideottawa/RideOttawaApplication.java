package io.github.jbytheway.rideottawa;

import com.orm.SugarApp;

import net.danlew.android.joda.JodaTimeAndroid;

public class RideOttawaApplication extends SugarApp {
    public RideOttawaApplication() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
    }

    public OcTranspoDataAccess getOcTranspo() {
        if (mOcTranspo == null) {
            mOcTranspo = new OcTranspoDataAccess(this);
        }
        return mOcTranspo;
    }

    private OcTranspoDataAccess mOcTranspo;
}
