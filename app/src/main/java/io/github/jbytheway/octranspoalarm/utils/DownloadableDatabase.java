package io.github.jbytheway.octranspoalarm.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
import java.util.zip.ZipInputStream;

import io.github.jbytheway.octranspoalarm.R;

/**
 * Helper class for managing a readonly database taken from the internet
 */
public abstract class DownloadableDatabase extends SQLiteOpenHelper {
    private static final String TAG = "DownloadableDatabase";

    public DownloadableDatabase(Context context, String name, String url, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);

        mContext = context;
        mUrl = url;
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
        void onFail(Exception e, boolean fatal);
    }

    public void checkForUpdates(final ProgressDialog progressDialog, final UpdateListener listener) throws IOException {
        //final Future<File> downloading = null;

        final String existingEtag = getEtag();
        final String temporaryFileName = getDatabaseName() + ".tmp";
        final File temporaryFile = mContext.getFileStreamPath(temporaryFileName);

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
                        if (e != null || code != 200) {
                            if (e != null && e.getClass() == CancellationException.class) {
                                // We are fine; no update was necessary
                                listener.onSuccess();
                            } else {
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
                                listener.onFail(e, fatal);
                            }
                            return;
                        }

                        try {
                            uncompressNewDatabase(result.getResult(), progressDialog, listener);
                            setEtag(result.getHeaders().getHeaders().get("ETag"));
                        } finally {
                            temporaryFile.delete();
                        }

                        listener.onSuccess();
                    }
                });
    }

    public void uncompressNewDatabase(File from, ProgressDialog progressDialog, UpdateListener listener) {
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
            // This has to be a fatal error because we might have partially written the database
            setEtag("");
            listener.onFail(e, true);
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

    String getPathWith(String extension) {
        return mContext.getDatabasePath(super.getDatabaseName() + extension).getPath();
    }

    String getPath() {
        return getPathWith("");
    }

    String getEtagPath() {
        return getPathWith(".etag");
    }

    String getEtag() throws IOException {
        File etagFile = new File(getEtagPath());
        if (!etagFile.exists()) {
            return "";
        }
        InputStream is = new FileInputStream(etagFile);
        return IOUtils.toString(is);
    }

    void setEtag(String etag) {
        try {
            FileOutputStream os = new FileOutputStream(getEtagPath());
            IOUtils.write(etag, os);
        } catch (IOException e) {
            Log.e(TAG, "Failed to update etag", e);
            // Not really anything we can do to recover from this; just have to live with downloading the file again next time
        }
    }

    Context mContext;
    String mUrl;
    Future<Response<File>> mDownload;
}
