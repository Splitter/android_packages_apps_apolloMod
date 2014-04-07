
package com.andrew.apolloMod.providers;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.MediaStore.Audio.AudioColumns;

public class HistoryStore extends ContentProvider {

	static final String PROVIDER = "com.andrew.apolloMod.recents";
	
	static final String URL = "content://" + PROVIDER ;
	
	public static final Uri CONTENT_URI = Uri.parse(URL);
	
	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
											+ "/vnd.com.andrew.apolloMod.recents";
	
	static final int RECENTS = 1;
	
	static final UriMatcher uriMatcher;
		static{
			uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
			uriMatcher.addURI(PROVIDER, "recents", RECENTS);
			}  
		
	RecentDB recentDBHelper;	
	private SQLiteDatabase database;
	   
	public HistoryStore() { }
	
	public static interface RecentColumns {
		
		public static final String _ID 			= 	BaseColumns._ID;
		
		public static final String TITLE 		= 	MediaColumns.TITLE;
		
		public static final String ALBUM 		= 	AudioColumns.ALBUM;

		public static final String ALBUM_ID 	= 	AudioColumns.ALBUM_ID;
		
		public static final String ARTIST 		= 	AudioColumns.ARTIST;
		
        public static final String TIME_PLAYED 	= 	"timeplayed";
		
	}

	public class RecentDB extends SQLiteOpenHelper {
		
		private static final int VERSION = 2;

	    public static final String DATABASENAME = "recenthistory.db";
	    
	    public static final String DATABASETABLE = "recenthistory";
	    
	    public RecentDB(Context context) {
	    	super(context, DATABASENAME, null, VERSION);
	    }
	    @Override
	    public void onCreate(final SQLiteDatabase db) {
	        db.execSQL("CREATE TABLE IF NOT EXISTS " + DATABASETABLE + " ("
	                + RecentColumns._ID + " LONG NOT NULL," + RecentColumns.ALBUM
	                + " TEXT NOT NULL," + RecentColumns.ARTIST + " TEXT NOT NULL,"
	                + RecentColumns.TITLE + " TEXT NOT NULL,"+ RecentColumns.ALBUM_ID 
	                + " TEXT NOT NULL," + RecentColumns.TIME_PLAYED + " LONG NOT NULL);");
	    }	    
	    @Override
	    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
	        db.execSQL("DROP TABLE IF EXISTS " + DATABASETABLE);
	        onCreate(db);
	    }
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int count = database.delete(RecentDB.DATABASETABLE, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)){
			case RECENTS:
				return CONTENT_TYPE;
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {		
		long row = database.insert(RecentDB.DATABASETABLE, "", values);		
		if(row > 0) {
			Uri newUri = ContentUris.withAppendedId(CONTENT_URI, row);
			getContext().getContentResolver().notifyChange(newUri, null);
			return newUri;
		}
		throw new SQLException("Fail to add a new record into " + uri);
	}

	@Override
	public boolean onCreate() {
		recentDBHelper = new RecentDB(getContext());
		database = recentDBHelper.getWritableDatabase();
		if(database == null)
			return false;
		else
			return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(RecentDB.DATABASETABLE);
		//queryBuilder.setDistinct(true);
		if (sortOrder == null || sortOrder == "")
			sortOrder = RecentColumns.TIME_PLAYED;
		
		Cursor cursor = queryBuilder.query(database, projection, selection,
											selectionArgs, null, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int count = database.update(RecentDB.DATABASETABLE, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
}
