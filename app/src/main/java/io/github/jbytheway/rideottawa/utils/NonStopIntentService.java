package io.github.jbytheway.rideottawa.utils;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.widget.Toast;

import androidx.annotation.Nullable;

// Implementation based on example at https://developer.android.com/guide/components/services

public abstract class NonStopIntentService extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final String mName;

    protected NonStopIntentService(String name) {
        mName = name;
    }

    // Handler that receives messages for the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            if (msg.obj == null || onHandleIntent((Intent) msg.obj)) {
                // Stop the service using the startId, so that we don't stop
                // the service in the middle of handling another job
                stopSelf(msg.arg1);
            }
        }
    }

    // Return true if the service should stop
    protected abstract boolean onHandleIntent(Intent intent);

    @Override
    public void onCreate() {
        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread(mName, Process.THREAD_PRIORITY_BACKGROUND);
        super.onCreate();
        // Get the HandlerThread's Looper and use it for our Handler
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        mServiceLooper.quitSafely();
    }
}
