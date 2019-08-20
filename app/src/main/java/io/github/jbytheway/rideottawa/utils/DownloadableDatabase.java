package io.github.jbytheway.rideottawa.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.HeadersCallback;
import com.koushikdutta.ion.HeadersResponse;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

import io.github.jbytheway.rideottawa.R;

/**
 * Helper class for managing a readonly database taken from the internet
 */
public abstract class DownloadableDatabase extends SQLiteOpenHelper {
    private static final String TAG = "DownloadableDatabase";

    protected DownloadableDatabase(Context context, String name, String url, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);

        mContext = context;
        mUrl = url;
    }

    public void deleteDatabase() {
        File file = new File(getEtagPath());
        Log.i(TAG, "Deleting etag at " + file.getAbsolutePath());
        //noinspection ResultOfMethodCallIgnored
        file.delete();

        file = new File(getPath());
        Log.i(TAG, "Deleting database at " + file.getAbsolutePath());
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    @Override
    public final void onCreate(SQLiteDatabase db) {
        // Nothing to do; all tables etc. should be present already in downloaded version
    }

    @Override
    public final SQLiteDatabase getWritableDatabase() {
        throw new AssertionError("This class is only intended for use with readonly databases");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Nothing to do on upgrade; this is assumed to be a readonly database
    }

    public interface UpdateListener {
        void onSuccess();
        void onCancel();
        void onFail(Exception e, Integer code, String message, boolean wifiRelated, boolean fatal);
    }

    public @Nullable DateTime getLastUpdateCheck() {
        if (isDatabaseAvailable()) {
            long timestamp = new File(getEtagPath()).lastModified();
            return new DateTime(timestamp, DateTimeZone.UTC);
        } else {
            return null;
        }
    }

    private static class DecompressArgs {
        DecompressArgs(Context context, DownloadableDatabase database, File from, File to, String etag, ProgressDialog progressDialog, UpdateListener listener) {
            Context = context;
            mDatabase = database;
            From = from;
            To = to;
            Etag = etag;
            ProgressDialog = progressDialog;
            Listener = listener;
        }
        private DownloadableDatabase mDatabase;

        final Context Context;
        final File From;
        final File To;
        final String Etag;
        final ProgressDialog ProgressDialog;
        final UpdateListener Listener;

        void setEtag(String etag) {
            mDatabase.setEtag(etag);
        }

        void close() {
            mDatabase.close();
        }
    }

    private static class DecompressDatabaseTask extends AsyncTask<DecompressArgs, Void, Void> {
        @Override
        protected Void doInBackground(DecompressArgs... params) {
            mArgs = params[0];
            try {
                mArgs.ProgressDialog.setMessage(mArgs.Context.getString(R.string.decompressing));
                final String tmpFileName = mArgs.To.getName() + ".tmp";
                final File tmpFile = mArgs.Context.getFileStreamPath(tmpFileName);
                Log.d(TAG, "Decompressing " + mArgs.From.getPath() + " to " + tmpFile.getPath());
                FileInputStream fin = new FileInputStream(mArgs.From);
                GZIPInputStream zin = new GZIPInputStream(fin);

                FileOutputStream fout = new FileOutputStream(tmpFile);

                ByteStreams.copy(zin, fout);
                Log.d(TAG, "Decompression done");
                if (!tmpFile.renameTo(mArgs.To)) {
                    throw new IOException("Failed to rename "+tmpFile+" to "+mArgs.To.getPath());
                }
                // We want to force any future access to be from the new database, not the old one
                // so we force a close on this.  (TODO: test if this is really effective)
                mArgs.close();
                mArgs.setEtag(mArgs.Etag);
                mArgs.Listener.onSuccess();
            } catch (IOException e) {
                Log.e(TAG, "Decompressing failed", e);
                // Unset the ETag because we might have partially written the database
                mArgs.setEtag("");
                mArgs.Listener.onFail(e, null, mArgs.Context.getString(R.string.database_decompression_error), false, true);
            } finally {
                if (!mArgs.From.delete()) {
                    Log.e(TAG, "Deleting temporary file failed");
                }
            }
            return null;
        }

        DecompressArgs mArgs;
    }

    public void checkForUpdates(boolean wifiOnly, DateTime ifOlderThan, final ProgressDialog progressDialog, final UpdateListener listener) {
        try {
            final String existingEtag = getEtag();

            if (ifOlderThan != null) {
                // We have a DB already; if it's new enough we won't bother to check for updates
                DateTime lastModified = getLastUpdateCheck();
                Log.d(TAG, "lastModified = "+lastModified+", ifOlderThan="+ifOlderThan);
                if (lastModified != null && lastModified.isAfter(ifOlderThan)) {
                    // No need to update
                    listener.onSuccess();
                    return;
                }
            }
            final String gzFileName = getDatabaseName() + ".gz";
            final File temporaryFile = mContext.getFileStreamPath(gzFileName);

            ConnectivityManager connectivity = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            //noinspection deprecation
            boolean onWifi = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();

            if (!onWifi && wifiOnly) {
                downloadFailed(null, null, mContext.getString(R.string.not_downloading_without_wifi), true, listener);
                return;
            }

            Log.d(TAG, "Started download");
            mDownload = Ion
                    .with(mContext)
                    .load(mUrl)
                    .progressDialog(progressDialog)
                    .onHeaders(new HeadersCallback() {
                        @Override
                        public void onHeaders(HeadersResponse headers) {
                            String newEtag = headers.getHeaders().get("ETag");
                            Log.d(TAG, "Existing etag = '"+existingEtag+"', new etag = '"+newEtag+"'");
                            if (existingEtag.equals(newEtag)) {
                                mDownload.cancel(true);
                                touchEtag();
                                listener.onCancel();
                            } else {
                                progressDialog.setMessage(mContext.getString(R.string.downloading_new));
                            }
                        }
                    })
                    .write(temporaryFile)
                    .withResponse()
                    .setCallback(new FutureCallback<Response<File>>() {
                        @Override
                        public void onCompleted(Exception e, Response<File> result) {
                            Integer code = null;
                            if (result != null) {
                                code = result.getHeaders().code();
                            }
                            Log.d(TAG, "Completed download; e=" + e + "; code=" + code);
                            if (e != null || code == null || code != 200) {
                                if (e != null && e.getClass() == CancellationException.class) {
                                    // We are fine; cancelled in the onHeaders above; no update was necessary
                                    listener.onSuccess();
                                } else {
                                    downloadFailed(e, code, mContext.getString(R.string.download_failed_but_continuing), false, listener);
                                }
                                return;
                            }

                            new DecompressDatabaseTask().execute(new DecompressArgs(mContext, DownloadableDatabase.this, result.getResult(), new File(getPath()), result.getHeaders().getHeaders().get("ETag"), progressDialog, listener));
                        }
                    });
        } catch (IOException e) {
            // the only way this can happen is if we fail to get the existing eTag, which is really bad
            listener.onFail(e, null, mContext.getString(R.string.database_update_io_error), false, true);
        }
    }

    private void downloadFailed(Exception e, Integer code, String message, boolean wifiRelated, UpdateListener listener) {
        // The download failed.  Figure out whether we have a database
        // already (if not, then we cannot continue)
        boolean fatal = true;
        try {
            fatal = getEtag().isEmpty();
        } catch (IOException etagError) {
            Log.e(TAG, "Error determining ETag", etagError);
            // Apart from the log, we pretty much have to ignore that error
        }
        Log.d(TAG, "Download failed; e=" + e + "; code=" + code + "; fatal=" + fatal);
        listener.onFail(e, code, message, wifiRelated, fatal);
    }

    public boolean isDatabaseAvailable() {
        try {
            return !getEtag().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        File file = new File(getPath());
        if (!file.exists()) {
            throw new RuntimeException("Database file " + getPath() + " does not exist; did you call checkForUpdates() before trying to get the database?");
        }
        return super.getReadableDatabase();
    }

    private String getPathWith(String extension) {
        return mContext.getDatabasePath(super.getDatabaseName() + extension).getPath();
    }

    private String getPath() {
        return getPathWith("");
    }

    private String getEtagPath() {
        return getPathWith(".etag");
    }

    private String getEtag() throws IOException {
        File file = new File(getPath());
        File etagFile = new File(getEtagPath());
        if (!etagFile.exists() || !file.exists()) {
            return "";
        }
        InputStream is = new FileInputStream(etagFile);
        InputStreamReader isr = new InputStreamReader(is, "ASCII");
        String result = CharStreams.toString(isr);
        isr.close();
        return result;
    }

    private void setEtag(String etag) {
        try {
            FileOutputStream os = new FileOutputStream(getEtagPath());
            OutputStreamWriter osr = new OutputStreamWriter(os, "ASCII");
            osr.write(etag);
            osr.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to update etag", e);
            // Not really anything we can do to recover from this; just have to live with downloading the file again next time
        }
    }

    private void touchEtag() {
        // Update the last-modified date on the ETag so we know when we last checked for updates
        try {
            Files.touch(new File(getEtagPath()));
        } catch (IOException e) {
            Log.e(TAG, "Error touching etag", e);
            // Otherwise, ignore error
        }
    }

    private final Context mContext;
    private final String mUrl;
    private Future<Response<File>> mDownload;
}
