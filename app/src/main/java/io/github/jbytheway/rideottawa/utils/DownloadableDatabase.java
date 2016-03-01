package io.github.jbytheway.rideottawa.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.util.Log;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.HeadersCallback;
import com.koushikdutta.ion.HeadersResponse;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        file.delete();

        file = new File(getPath());
        Log.i(TAG, "Deleting database at " + file.getAbsolutePath());
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
        void onFail(Exception e, String message, boolean wifiRelated, boolean fatal);
    }

    public void checkForUpdates(boolean wifiOnly, final ProgressDialog progressDialog, final UpdateListener listener) {
        try {
            final String existingEtag = getEtag();
            final String temporaryFileName = getDatabaseName() + ".tmp";
            final File temporaryFile = mContext.getFileStreamPath(temporaryFileName);

            ConnectivityManager connectivity = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean onWifi = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();

            if (!onWifi && wifiOnly) {
                downloadFailed(null, mContext.getString(R.string.not_downloading_without_wifi), true, listener);
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
                            if (existingEtag.equals(newEtag)) {
                                mDownload.cancel(true);
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
                                    downloadFailed(e, mContext.getString(R.string.download_failed_but_continuing), false, listener);
                                }
                                return;
                            }

                            try {
                                uncompressNewDatabase(result.getResult(), progressDialog, listener);
                                setEtag(result.getHeaders().getHeaders().get("ETag"));
                            } finally {
                                //noinspection ResultOfMethodCallIgnored
                                temporaryFile.delete();
                            }

                            listener.onSuccess();
                        }
                    });
        } catch (IOException e) {
            // the only way this can happen is if we fail to get the existing eTag, which is really bad
            listener.onFail(e, mContext.getString(R.string.database_update_io_error), false, true);
        }
    }

    private void downloadFailed(Exception e, String message, boolean wifiRelated, UpdateListener listener) {
        // The download failed.  Figure out whether we have a database
        // already (if not, then we cannot continue)
        boolean fatal = true;
        try {
            fatal = getEtag().isEmpty();
        } catch (IOException etagError) {
            Log.e(TAG, "Error determining ETag", etagError);
            // Apart from the log, we pretty much have to ignore that error
        }
        Log.d(TAG, "Download failed; e=" + e + "; fatal=" + fatal);
        listener.onFail(e, message, wifiRelated, fatal);
    }

    private void uncompressNewDatabase(File from, ProgressDialog progressDialog, UpdateListener listener) {
        try {
            progressDialog.setMessage(mContext.getString(R.string.decompressing));
            Log.d(TAG, "Decompressing " + from.getPath() + " to " + getPath());
            FileInputStream fin = new FileInputStream(from);
            GZIPInputStream zin = new GZIPInputStream(fin);

            FileOutputStream fout = new FileOutputStream(getPath());

            IOUtils.copy(zin, fout);
            Log.d(TAG, "Decompression done");
        } catch (IOException e) {
            Log.e(TAG, "Decompressing failed", e);
            // Unset the ETag because we might have partially written the database
            setEtag("");
            listener.onFail(e, mContext.getString(R.string.database_decompression_error), false, true);
        }
    }

    @Override
    public SQLiteDatabase getReadableDatabase() {
        File file = new File (getPath());
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
        File etagFile = new File(getEtagPath());
        if (!etagFile.exists()) {
            return "";
        }
        InputStream is = new FileInputStream(etagFile);
        return IOUtils.toString(is);
    }

    private void setEtag(String etag) {
        try {
            FileOutputStream os = new FileOutputStream(getEtagPath());
            IOUtils.write(etag, os);
        } catch (IOException e) {
            Log.e(TAG, "Failed to update etag", e);
            // Not really anything we can do to recover from this; just have to live with downloading the file again next time
        }
    }

    private final Context mContext;
    private final String mUrl;
    private Future<Response<File>> mDownload;
}
