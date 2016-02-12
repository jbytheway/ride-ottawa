package io.github.jbytheway.octranspoalarm;

import com.orm.SugarApp;

import net.danlew.android.joda.JodaTimeAndroid;

public class OcTranspoApplication extends SugarApp {
    public OcTranspoApplication() {
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

    OcTranspoDataAccess mOcTranspo;
}
