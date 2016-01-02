package io.github.jbytheway.octranspoalarm;

import com.orm.SugarApp;

public class OcTranspoApplication extends SugarApp {
    public OcTranspoApplication() {
    }

    public OcTranspoDataAccess getOcTranspo() {
        if (mOcTranspo == null) {
            mOcTranspo = new OcTranspoDataAccess(this);
        }
        return mOcTranspo;
    }

    OcTranspoDataAccess mOcTranspo;
}
