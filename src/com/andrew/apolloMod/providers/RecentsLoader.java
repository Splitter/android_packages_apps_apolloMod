
package com.andrew.apolloMod.providers;

import android.support.v4.content.AsyncTaskLoader;
import android.content.ContentQueryMap;
import android.content.Context;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import com.andrew.apolloMod.providers.HistoryStore.RecentColumns;

public class RecentsLoader extends AsyncTaskLoader<Cursor> {
    final ForceLoadContentObserver mObserver;

    Uri mUri;
    String[] mProjection;
    String mSelection;
    String[] mSelectionArgs;
    String mSortOrder;

    Cursor mCursor;
    Context mContext;

    /* Runs on a worker thread */
    @Override
    public Cursor loadInBackground() {

    	//Get Recently Added Songs
    	String[] sProjection = new String[] {
                BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ALBUM, AudioColumns.ARTIST
        };

        StringBuilder where = new StringBuilder();
        where.append(MediaColumns.TITLE + " != ''");
        where.append(" AND " + AudioColumns.IS_MUSIC + "=1");
        
        Cursor mediaCursor = getContext().getContentResolver().query( Audio.Media.EXTERNAL_CONTENT_URI,
        			sProjection, where.toString(), null, MediaColumns.DATE_ADDED + " DESC LIMIT 4");
        
        //Merge with Album data
        String [] projection =  new String[] {
                BaseColumns._ID, AlbumColumns.ALBUM
        };        
        Uri uri = Audio.Albums.EXTERNAL_CONTENT_URI;
        String sortOrder = Audio.Albums.DEFAULT_SORT_ORDER;
        Cursor albumCursor = getContext().getContentResolver().query(uri, projection, null, null, sortOrder);
        
        //Matrix cursor to hold final data to be returned to calling context
        MatrixCursor cursor = new MatrixCursor( new String[]
        		{ BaseColumns._ID, MediaColumns.TITLE, AudioColumns.ARTIST, AudioColumns.ALBUM, AudioColumns.ALBUM_ID});
        //Map data from Audio Id cursor to the ALbumName Colum
        ContentQueryMap mQueryMap = new ContentQueryMap(albumCursor, AlbumColumns.ALBUM, false, null);

		Map<String, ContentValues> data = mQueryMap.getRows();
        if (mediaCursor != null) {
            while(mediaCursor.moveToNext()) {
				String id = mediaCursor.getString(mediaCursor.getColumnIndexOrThrow(BaseColumns._ID));
				String title = mediaCursor.getString(mediaCursor.getColumnIndexOrThrow(MediaColumns.TITLE));
				String artist = mediaCursor.getString(mediaCursor.getColumnIndexOrThrow(AudioColumns.ARTIST));
				String album = mediaCursor.getString(mediaCursor.getColumnIndexOrThrow(AudioColumns.ALBUM));
					
				ContentValues tData = data.get(album);
				String albumid = (String) tData.get(BaseColumns._ID);
				cursor.addRow(new String[] {id, title, artist, album, albumid});
            }
            mediaCursor.close();
        }
        
        //Add empty row to symbolize start of recently played
		cursor.addRow(new String[] {"-1", "", "", "", ""});
        
        //Add recently played to cursor
        sProjection = new String[] {
    			"DISTINCT "+RecentColumns._ID, RecentColumns.TIME_PLAYED,
                RecentColumns.TITLE, RecentColumns.ARTIST,
                RecentColumns.ALBUM, RecentColumns.ALBUM_ID       
        };                
        Cursor historyCursor = getContext().getContentResolver().query( HistoryStore.CONTENT_URI,
    			sProjection, null, null, RecentColumns.TIME_PLAYED + " DESC LIMIT 4");
        if (historyCursor != null) {
            while(historyCursor.moveToNext()) {
				String id = historyCursor.getString(historyCursor.getColumnIndexOrThrow(BaseColumns._ID));
				String title = historyCursor.getString(historyCursor.getColumnIndexOrThrow(MediaColumns.TITLE));
				String artist = historyCursor.getString(historyCursor.getColumnIndexOrThrow(AudioColumns.ARTIST));
				String album = historyCursor.getString(historyCursor.getColumnIndexOrThrow(AudioColumns.ALBUM));
				String albumid = historyCursor.getString(historyCursor.getColumnIndexOrThrow(AudioColumns.ALBUM_ID));
				cursor.addRow(new String[] {id, title, artist, album, albumid});
            }
            historyCursor.close();
        }
        
        if (cursor != null) {
            // Ensure the cursor window is filled
            registerContentObserver(cursor, mObserver);
            cursor.setNotificationUri(mContext.getContentResolver(), HistoryStore.CONTENT_URI);
        }
        return cursor;
    }

    /**
     * Registers an observer to get notifications from the content provider
     * when the cursor needs to be refreshed.
     */
    void registerContentObserver(Cursor cursor, ContentObserver observer) {
        cursor.registerContentObserver(mObserver);
    }

    /* Runs on the UI thread */
    @Override
    public void deliverResult(Cursor cursor) {
        if (isReset()) {
            // An async query came in while the loader is stopped
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        Cursor oldCursor = mCursor;
        mCursor = cursor;

        if (isStarted()) {
            super.deliverResult(cursor);
        }

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    /**
     * Creates an empty unspecified CursorLoader.  You must follow this with
     * calls to {@link #setUri(Uri)}, {@link #setSelection(String)}, etc
     * to specify the query to perform.
     */
    public RecentsLoader(Context context) {
        super(context);
        mContext = context;
        mObserver = new ForceLoadContentObserver();
    }

    /**
     * Starts an asynchronous load of the contacts list data. When the result is ready the callbacks
     * will be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     *
     * Must be called from the UI thread
     */
    @Override
    protected void onStartLoading() {
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }

    /**
     * Must be called from the UI thread
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        
        // Ensure the loader is stopped
        onStopLoading();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = null;
    }

    public Uri getUri() {
        return mUri;
    }

    public void setUri(Uri uri) {
        mUri = uri;
    }

    public String[] getProjection() {
        return mProjection;
    }

    public void setProjection(String[] projection) {
        mProjection = projection;
    }

    public String getSelection() {
        return mSelection;
    }

    public void setSelection(String selection) {
        mSelection = selection;
    }

    public String[] getSelectionArgs() {
        return mSelectionArgs;
    }

    public void setSelectionArgs(String[] selectionArgs) {
        mSelectionArgs = selectionArgs;
    }

    public String getSortOrder() {
        return mSortOrder;
    }

    public void setSortOrder(String sortOrder) {
        mSortOrder = sortOrder;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
        writer.print(prefix); writer.print("mUri="); writer.println(mUri);
        writer.print(prefix); writer.print("mProjection=");
                writer.println(Arrays.toString(mProjection));
        writer.print(prefix); writer.print("mSelection="); writer.println(mSelection);
        writer.print(prefix); writer.print("mSelectionArgs=");
                writer.println(Arrays.toString(mSelectionArgs));
        writer.print(prefix); writer.print("mSortOrder="); writer.println(mSortOrder);
        writer.print(prefix); writer.print("mCursor="); writer.println(mCursor);
    }
}
