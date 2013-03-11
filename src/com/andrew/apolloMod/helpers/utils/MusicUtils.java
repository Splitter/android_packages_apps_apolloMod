
package com.andrew.apolloMod.helpers.utils;

import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Genres;
import android.provider.MediaStore.Audio.GenresColumns;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Audio.PlaylistsColumns;
import android.provider.MediaStore.MediaColumns;
import android.provider.Settings;
import android.widget.ImageButton;
import android.widget.Toast;

import com.andrew.apolloMod.IApolloService;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.service.ServiceBinder;
import com.andrew.apolloMod.service.ServiceToken;

import static com.andrew.apolloMod.Constants.EXTERNAL;
import static com.andrew.apolloMod.Constants.GENRES_DB;
import static com.andrew.apolloMod.Constants.PLAYLIST_NAME_FAVORITES;
import static com.andrew.apolloMod.Constants.PLAYLIST_NEW;
import static com.andrew.apolloMod.Constants.PLAYLIST_QUEUE;

/**
 * Various methods used to help with specific music statements
 */
public class MusicUtils {

    // Used to make number of albums/songs/time strings
    private final static StringBuilder sFormatBuilder = new StringBuilder();

    private final static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());

    public static IApolloService mService = null;

    private static HashMap<Context, ServiceBinder> sConnectionMap = new HashMap<Context, ServiceBinder>();

    private final static long[] sEmptyList = new long[0];

    private static final Object[] sTimeArgs = new Object[5];

    private static ContentValues[] sContentValuesCache = null;

    /**
     * @param context
     * @return
     */
    public static ServiceToken bindToService(Activity context) {
        return bindToService(context, null);
    }

    /**
     * @param context
     * @param callback
     * @return
     */
    public static ServiceToken bindToService(Context context, ServiceConnection callback) {
        Activity realActivity = ((Activity)context).getParent();
        if (realActivity == null) {
            realActivity = (Activity)context;
        }
        ContextWrapper cw = new ContextWrapper(realActivity);
        cw.startService(new Intent(cw, ApolloService.class));
        ServiceBinder sb = new ServiceBinder(callback);
        if (cw.bindService((new Intent()).setClass(cw, ApolloService.class), sb, 0)) {
            sConnectionMap.put(cw, sb);
            return new ServiceToken(cw);
        }
        return null;
    }

    /**
     * @param token
     */
    public static void unbindFromService(ServiceToken token) {
        if (token == null) {
            return;
        }
        ContextWrapper cw = token.mWrappedContext;
        ServiceBinder sb = sConnectionMap.remove(cw);
        if (sb == null) {
            return;
        }
        cw.unbindService(sb);
        if (sConnectionMap.isEmpty()) {
            mService = null;
        }
    }

    /**
     * @param context
     * @param numalbums
     * @param numsongs
     * @param isUnknown
     * @return a string based on the number of albums for an artist or songs for
     *         an album
     */
    public static String makeAlbumsLabel(Context mContext, int numalbums, int numsongs,
            boolean isUnknown) {

        StringBuilder songs_albums = new StringBuilder();

        Resources r = mContext.getResources();
        if (isUnknown) {
            String f = r.getQuantityText(R.plurals.Nsongs, numsongs).toString();
            sFormatBuilder.setLength(0);
            sFormatter.format(f, Integer.valueOf(numsongs));
            songs_albums.append(sFormatBuilder);
        } else {
            String f = r.getQuantityText(R.plurals.Nalbums, numalbums).toString();
            sFormatBuilder.setLength(0);
            sFormatter.format(f, Integer.valueOf(numalbums));
            songs_albums.append(sFormatBuilder);
            songs_albums.append("\n");
        }
        return songs_albums.toString();
    }

    /**
     * @param mContext
     * @return
     */
    public static int getCardId(Context mContext) {

        ContentResolver res = mContext.getContentResolver();
        Cursor c = res.query(Uri.parse("content://media/external/fs_id"), null, null, null, null);
        int id = -1;
        if (c != null) {
            c.moveToFirst();
            id = c.getInt(0);
            c.close();
        }
        return id;
    }

    /**
     * @param context
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @param limit
     * @return
     */
    public static Cursor query(Context context, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder, int limit) {
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return null;
            }
            if (limit > 0) {
                uri = uri.buildUpon().appendQueryParameter("limit", "" + limit).build();
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (UnsupportedOperationException ex) {
            return null;
        }
    }

    /**
     * @param context
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return
     */
    public static Cursor query(Context context, Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return query(context, uri, projection, selection, selectionArgs, sortOrder, 0);
    }

    /**
     * @param context
     * @param cursor
     */
    public static void shuffleAll(Context context, Cursor cursor) {
        playAll(context, cursor, 0, true);
    }

    /**
     * @param context
     * @param cursor
     */
    public static void playAll(Context context, Cursor cursor) {
        playAll(context, cursor, 0, false);
    }

    /**
     * @param context
     * @param cursor
     * @param position
     */
    public static void playAll(Context context, Cursor cursor, int position) {
        playAll(context, cursor, position, false);
    }

    /**
     * @param context
     * @param list
     * @param position
     */
    public static void playAll(Context context, long[] list, int position) {
        playAll(context, list, position, false);
    }

    /**
     * @param context
     * @param cursor
     * @param position
     * @param force_shuffle
     */
    private static void playAll(Context context, Cursor cursor, int position, boolean force_shuffle) {

        long[] list = getSongListForCursor(cursor);
        playAll(context, list, position, force_shuffle);
    }

    /**
     * @param cursor
     * @return
     */
    public static long[] getSongListForCursor(Cursor cursor) {
        if (cursor == null) {
            return sEmptyList;
        }
        int len = cursor.getCount();
        long[] list = new long[len];
        cursor.moveToFirst();
        int colidx = -1;
        try {
            colidx = cursor.getColumnIndexOrThrow(Audio.Playlists.Members.AUDIO_ID);
        } catch (IllegalArgumentException ex) {
            colidx = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        }
        for (int i = 0; i < len; i++) {
            list[i] = cursor.getLong(colidx);
            cursor.moveToNext();
        }
        return list;
    }

    /**
     * @param context
     * @param list
     * @param position
     * @param force_shuffle
     */
    private static void playAll(Context context, long[] list, int position, boolean force_shuffle) {
        if (list.length == 0 || mService == null) {
            return;
        }
        try {
            if (force_shuffle) {
                mService.setShuffleMode(ApolloService.SHUFFLE_NORMAL);
            }
            long curid = mService.getAudioId();
            int curpos = mService.getQueuePosition();
            if (position != -1 && curpos == position && curid == list[position]) {
                // The selected file is the file that's currently playing;
                // figure out if we need to restart with a new playlist,
                // or just launch the playback activity.
                long[] playlist = mService.getQueue();
                if (Arrays.equals(list, playlist)) {
                    // we don't need to set a new list, but we should resume
                    // playback if needed
                    mService.play();
                    return;
                }
            }
            if (position < 0) {
                position = 0;
            }
            mService.open(list, force_shuffle ? -1 : position);
            mService.play();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @return
     */
    public static long[] getQueue() {

        if (mService == null)
            return sEmptyList;

        try {
            return mService.getQueue();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return sEmptyList;
    }

    /**
     * @param context
     * @param name
     * @param def
     * @return number of weeks used to create the Recent tab
     */
    public static int getIntPref(Context context, String name, int def) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(),
                Context.MODE_PRIVATE);
        return prefs.getInt(name, def);
    }

    /**
     * @param context
     * @param id
     * @return
     */
    public static long[] getSongListForArtist(Context context, long id) {
        final String[] projection = new String[] {
            BaseColumns._ID
        };
        String selection = AudioColumns.ARTIST_ID + "=" + id + " AND " + AudioColumns.IS_MUSIC
                + "=1";
        String sortOrder = AudioColumns.ALBUM_KEY + "," + AudioColumns.TRACK;
        Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = query(context, uri, projection, selection, null, sortOrder);
        if (cursor != null) {
            long[] list = getSongListForCursor(cursor);
            cursor.close();
            return list;
        }
        return sEmptyList;
    }

    /**
     * @param context
     * @param id
     * @return
     */
    public static long[] getSongListForAlbum(Context context, long id) {
        final String[] projection = new String[] {
            BaseColumns._ID
        };
        String selection = AudioColumns.ALBUM_ID + "=" + id + " AND " + AudioColumns.IS_MUSIC
                + "=1";
        String sortOrder = AudioColumns.TRACK;
        Uri uri = Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = query(context, uri, projection, selection, null, sortOrder);
        if (cursor != null) {
            long[] list = getSongListForCursor(cursor);
            cursor.close();
            return list;
        }
        return sEmptyList;
    }

    /**
     * @param context
     * @param id
     * @return
     */
    public static long[] getSongListForGenre(Context context, long id) {
        String[] projection = new String[] {
            BaseColumns._ID
        };
        StringBuilder selection = new StringBuilder();
        selection.append(AudioColumns.IS_MUSIC + "=1");
        selection.append(" AND " + MediaColumns.TITLE + "!=''");
        Uri uri = Genres.Members.getContentUri(EXTERNAL, id);
        Cursor cursor = context.getContentResolver().query(uri, projection, selection.toString(),
                null, null);
        if (cursor != null) {
            long[] list = getSongListForCursor(cursor);
            cursor.close();
            return list;
        }
        return sEmptyList;
    }

    /**
     * @param context
     * @param id
     * @return
     */
    public static long[] getSongListForPlaylist(Context context, long id) {
        final String[] projection = new String[] {
            Audio.Playlists.Members.AUDIO_ID
        };
        String sortOrder = Playlists.Members.DEFAULT_SORT_ORDER;
        Uri uri = Playlists.Members.getContentUri(EXTERNAL, id);
        Cursor cursor = query(context, uri, projection, null, null, sortOrder);
        if (cursor != null) {
            long[] list = getSongListForCursor(cursor);
            cursor.close();
            return list;
        }
        return sEmptyList;
    }

    /**
     * @param context
     * @param name
     * @return
     */
    public static long createPlaylist(Context context, String name) {

        if (name != null && name.length() > 0) {
            ContentResolver resolver = context.getContentResolver();
            String[] cols = new String[] {
                PlaylistsColumns.NAME
            };
            String whereclause = PlaylistsColumns.NAME + " = '" + name + "'";
            Cursor cur = resolver.query(Audio.Playlists.EXTERNAL_CONTENT_URI, cols, whereclause,
                    null, null);
            if (cur.getCount() <= 0) {
                ContentValues values = new ContentValues(1);
                values.put(PlaylistsColumns.NAME, name);
                Uri uri = resolver.insert(Audio.Playlists.EXTERNAL_CONTENT_URI, values);
                return Long.parseLong(uri.getLastPathSegment());
            }
            return -1;
        }
        return -1;
    }

    /**
     * @param context
     * @return
     */
    public static long getFavoritesId(Context context) {
        long favorites_id = -1;
        String favorites_where = PlaylistsColumns.NAME + "='" + "Favorites" + "'";
        String[] favorites_cols = new String[] {
            BaseColumns._ID
        };
        Uri favorites_uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor cursor = query(context, favorites_uri, favorites_cols, favorites_where, null, null);
        if (cursor.getCount() <= 0) {
            favorites_id = createPlaylist(context, "Favorites");
        } else {
            cursor.moveToFirst();
            favorites_id = cursor.getLong(0);
            cursor.close();
        }
        return favorites_id;
    }

    /**
     * @param context
     * @param id
     */
    public static void setRingtone(Context context, long id) {
        ContentResolver resolver = context.getContentResolver();
        // Set the flag in the database to mark this as a ringtone
        Uri ringUri = ContentUris.withAppendedId(Audio.Media.EXTERNAL_CONTENT_URI, id);
        try {
            ContentValues values = new ContentValues(2);
            values.put(AudioColumns.IS_RINGTONE, "1");
            values.put(AudioColumns.IS_ALARM, "1");
            resolver.update(ringUri, values, null, null);
        } catch (UnsupportedOperationException ex) {
            // most likely the card just got unmounted
            return;
        }

        String[] cols = new String[] {
                BaseColumns._ID, MediaColumns.DATA, MediaColumns.TITLE
        };

        String where = BaseColumns._ID + "=" + id;
        Cursor cursor = query(context, Audio.Media.EXTERNAL_CONTENT_URI, cols, where, null, null);
        try {
            if (cursor != null && cursor.getCount() == 1) {
                // Set the system setting to make this the current ringtone
                cursor.moveToFirst();
                Settings.System.putString(resolver, Settings.System.RINGTONE, ringUri.toString());
                String message = context.getString(R.string.set_as_ringtone, cursor.getString(2));
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * @param context
     * @param plid
     */
    public static void clearPlaylist(Context context, int plid) {
        Uri uri = Audio.Playlists.Members.getContentUri(EXTERNAL, plid);
        context.getContentResolver().delete(uri, null, null);
        return;
    }

    /**
     * @param context
     * @param ids
     * @param playlistid
     */
    public static void addToPlaylist(Context context, long[] ids, long playlistid) {

        if (ids == null) {
        } else {
            int size = ids.length;
            ContentResolver resolver = context.getContentResolver();
            // need to determine the number of items currently in the playlist,
            // so the play_order field can be maintained.
            String[] cols = new String[] {
                "count(*)"
            };
            Uri uri = Audio.Playlists.Members.getContentUri(EXTERNAL, playlistid);
            Cursor cur = resolver.query(uri, cols, null, null, null);
            cur.moveToFirst();
            int base = cur.getInt(0);
            cur.close();
            int numinserted = 0;
            for (int i = 0; i < size; i += 1000) {
                makeInsertItems(ids, i, 1000, base);
                numinserted += resolver.bulkInsert(uri, sContentValuesCache);
            }
            String message = context.getResources().getQuantityString(
                    R.plurals.NNNtrackstoplaylist, numinserted, numinserted);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @param ids
     * @param offset
     * @param len
     * @param base
     */
    private static void makeInsertItems(long[] ids, int offset, int len, int base) {

        // adjust 'len' if would extend beyond the end of the source array
        if (offset + len > ids.length) {
            len = ids.length - offset;
        }
        // allocate the ContentValues array, or reallocate if it is the wrong
        // size
        if (sContentValuesCache == null || sContentValuesCache.length != len) {
            sContentValuesCache = new ContentValues[len];
        }
        // fill in the ContentValues array with the right values for this pass
        for (int i = 0; i < len; i++) {
            if (sContentValuesCache[i] == null) {
                sContentValuesCache[i] = new ContentValues();
            }

            sContentValuesCache[i].put(Playlists.Members.PLAY_ORDER, base + offset + i);
            sContentValuesCache[i].put(Playlists.Members.AUDIO_ID, ids[offset + i]);
        }
    }

    /**
     * Toggle favorites
     */
    public static void toggleFavorite() {

        if (mService == null)
            return;
        try {
            mService.toggleFavorite();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param context
     * @param id
     */
    public static void addToFavorites(Context context, long id) {

        long favorites_id;

        if (id < 0) {

        } else {
            ContentResolver resolver = context.getContentResolver();

            String favorites_where = PlaylistsColumns.NAME + "='" + PLAYLIST_NAME_FAVORITES + "'";
            String[] favorites_cols = new String[] {
                BaseColumns._ID
            };
            Uri favorites_uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
            Cursor cursor = resolver.query(favorites_uri, favorites_cols, favorites_where, null,
                    null);
            if (cursor.getCount() <= 0) {
                favorites_id = createPlaylist(context, PLAYLIST_NAME_FAVORITES);
            } else {
                cursor.moveToFirst();
                favorites_id = cursor.getLong(0);
                cursor.close();
            }

            String[] cols = new String[] {
                Playlists.Members.AUDIO_ID
            };
            Uri uri = Playlists.Members.getContentUri(EXTERNAL, favorites_id);
            Cursor cur = resolver.query(uri, cols, null, null, null);

            int base = cur.getCount();
            cur.moveToFirst();
            while (!cur.isAfterLast()) {
                if (cur.getLong(0) == id)
                    return;
                cur.moveToNext();
            }
            cur.close();

            ContentValues values = new ContentValues();
            values.put(Playlists.Members.AUDIO_ID, id);
            values.put(Playlists.Members.PLAY_ORDER, base + 1);
            resolver.insert(uri, values);
        }
    }

    /**
     * @param context
     * @param id
     * @return
     */
    public static boolean isFavorite(Context context, long id) {

        long favorites_id;

        if (id < 0) {

        } else {
            ContentResolver resolver = context.getContentResolver();

            String favorites_where = PlaylistsColumns.NAME + "='" + PLAYLIST_NAME_FAVORITES + "'";
            String[] favorites_cols = new String[] {
                BaseColumns._ID
            };
            Uri favorites_uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
            Cursor cursor = resolver.query(favorites_uri, favorites_cols, favorites_where, null,
                    null);
            if (cursor.getCount() <= 0) {
                favorites_id = createPlaylist(context, PLAYLIST_NAME_FAVORITES);
            } else {
                cursor.moveToFirst();
                favorites_id = cursor.getLong(0);
                cursor.close();
            }

            String[] cols = new String[] {
                Playlists.Members.AUDIO_ID
            };
            Uri uri = Playlists.Members.getContentUri(EXTERNAL, favorites_id);
            Cursor cur = resolver.query(uri, cols, null, null, null);

            cur.moveToFirst();
            while (!cur.isAfterLast()) {
                if (cur.getLong(0) == id) {
                    cur.close();
                    return true;
                }
                cur.moveToNext();
            }
            cur.close();
            return false;
        }
        return false;
    }

    /**
     * @param context
     * @param id
     */
    public static void removeFromFavorites(Context context, long id) {
        long favorites_id;
        if (id < 0) {
        } else {
            ContentResolver resolver = context.getContentResolver();
            String favorites_where = PlaylistsColumns.NAME + "='" + PLAYLIST_NAME_FAVORITES + "'";
            String[] favorites_cols = new String[] {
                BaseColumns._ID
            };
            Uri favorites_uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
            Cursor cursor = resolver.query(favorites_uri, favorites_cols, favorites_where, null,
                    null);
            if (cursor.getCount() <= 0) {
                favorites_id = createPlaylist(context, PLAYLIST_NAME_FAVORITES);
            } else {
                cursor.moveToFirst();
                favorites_id = cursor.getLong(0);
                cursor.close();
            }
            Uri uri = Playlists.Members.getContentUri(EXTERNAL, favorites_id);
            resolver.delete(uri, Playlists.Members.AUDIO_ID + "=" + id, null);
        }
    }

    /**
     * @param mService
     * @param mImageButton
     * @param id
     */
    public static void setFavoriteImage(ImageButton mImageButton) {
        try {
            if (MusicUtils.mService.isFavorite(MusicUtils.mService.getAudioId())) {
                mImageButton.setImageResource(R.drawable.apollo_holo_light_favorite_selected);
            } else {
                mImageButton.setImageResource(R.drawable.apollo_holo_light_favorite_normal);
                // Theme chooser
                ThemeUtils.setImageButton(mImageButton.getContext(), mImageButton,
                        "apollo_favorite_normal");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param mContext
     * @param id
     * @param name
     */
    public static void renamePlaylist(Context mContext, long id, String name) {

        if (name != null && name.length() > 0) {
            ContentResolver resolver = mContext.getContentResolver();
            ContentValues values = new ContentValues(1);
            values.put(PlaylistsColumns.NAME, name);
            resolver.update(Audio.Playlists.EXTERNAL_CONTENT_URI, values, BaseColumns._ID + "=?",
                    new String[] {
                        String.valueOf(id)
                    });
            Toast.makeText(mContext, "Playlist renamed", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @param mContext
     * @param list
     */
    public static void addToCurrentPlaylist(Context mContext, long[] list) {

        if (mService == null)
            return;
        try {
            mService.enqueue(list, ApolloService.LAST);
            String message = mContext.getResources().getQuantityString(
                    R.plurals.NNNtrackstoplaylist, list.length, Integer.valueOf(list.length));
            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
        } catch (RemoteException ex) {
        }
    }

    /**
     * @param context
     * @param secs
     * @return time String
     */
    public static String makeTimeString(Context context, long secs) {

        String durationformat = context.getString(secs < 3600 ? R.string.durationformatshort
                : R.string.durationformatlong);

        /*
         * Provide multiple arguments so the format can be changed easily by
         * modifying the xml.
         */
        sFormatBuilder.setLength(0);

        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = secs / 60 % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;

        return sFormatter.format(durationformat, timeArgs).toString();
    }

    /**
     * @return current album ID
     */
    public static long getCurrentAlbumId() {

        if (mService != null) {
            try {
                return mService.getAlbumId();
            } catch (RemoteException ex) {
            }
        }
        return -1;
    }

    /**
     * @return current artist ID
     */
    public static long getCurrentArtistId() {

        if (MusicUtils.mService != null) {
            try {
                return mService.getArtistId();
            } catch (RemoteException ex) {
            }
        }
        return -1;
    }

    /**
     * @return current track ID
     */
    public static long getCurrentAudioId() {

        if (MusicUtils.mService != null) {
            try {
                return mService.getAudioId();
            } catch (RemoteException ex) {
            }
        }
        return -1;
    }

    /**
     * @return current artist name
     */
    public static String getArtistName() {

        if (mService != null) {
            try {
                return mService.getArtistName();
            } catch (RemoteException ex) {
            }
        }
        return null;
    }

    /**
     * @return current album name
     */
    public static String getAlbumName() {

        if (mService != null) {
            try {
                return mService.getAlbumName();
            } catch (RemoteException ex) {
            }
        }
        return null;
    }

    /**
     * @return current track name
     */
    public static String getTrackName() {

        if (mService != null) {
            try {
                return mService.getTrackName();
            } catch (RemoteException ex) {
            }
        }
        return null;
    }

    /**
     * @return duration of a track
     */
    public static long getDuration() {
        if (mService != null) {
            try {
                return mService.duration();
            } catch (RemoteException e) {
            }
        }
        return 0;
    }

    /**
     * Create a Search Chooser
     */
    public static void doSearch(Context mContext, Cursor mCursor, int index) {
        CharSequence title = null;
        Intent i = new Intent();
        i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String query = mCursor.getString(index);
        title = "";
        i.putExtra("", query);
        title = title + " " + query;
        title = "Search " + title;
        i.putExtra(SearchManager.QUERY, query);
        mContext.startActivity(Intent.createChooser(i, title));
    }

    /**
     * Method that removes all tracks from the current queue
     */
    public static void removeAllTracks() {
        try {
            if (mService == null) {
                long[] current = MusicUtils.getQueue();
                if (current != null) {
                    mService.removeTracks(0, current.length-1);
                }
            }
        } catch (RemoteException e) {
        }
    }

    /**
     * @param id
     * @return removes track from a playlist
     */
    public static int removeTrack(long id) {
        if (mService == null)
            return 0;

        try {
            return mService.removeTrack(id);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * @param index
     */
    public static void setQueuePosition(int index) {
        if (mService == null)
            return;
        try {
            mService.setQueuePosition(index);
        } catch (RemoteException e) {
        }
    }

    public static String getArtistName(Context mContext, long artist_id, boolean default_name) {
        String where = BaseColumns._ID + "=" + artist_id;
        String[] cols = new String[] {
            ArtistColumns.ARTIST
        };
        Uri uri = Audio.Artists.EXTERNAL_CONTENT_URI;
        Cursor cursor = mContext.getContentResolver().query(uri, cols, where, null, null);
        if (cursor == null){
            return MediaStore.UNKNOWN_STRING;
        }
        if (cursor.getCount() <= 0) {
            if (default_name)
                return mContext.getString(R.string.unknown);
            else
                return MediaStore.UNKNOWN_STRING;
        } else {
            cursor.moveToFirst();
            String name = cursor.getString(0);
            cursor.close();
            if (name == null || MediaStore.UNKNOWN_STRING.equals(name)) {
                if (default_name)
                    return mContext.getString(R.string.unknown);
                else
                    return MediaStore.UNKNOWN_STRING;
            }
            return name;
        }
    }

    /**
     * @param mContext
     * @param album_id
     * @param default_name
     * @return album name
     */
    public static String getAlbumName(Context mContext, long album_id, boolean default_name) {
        String where = BaseColumns._ID + "=" + album_id;
        String[] cols = new String[] {
            AlbumColumns.ALBUM
        };
        Uri uri = Audio.Albums.EXTERNAL_CONTENT_URI;
        Cursor cursor = mContext.getContentResolver().query(uri, cols, where, null, null);
        if (cursor == null){
            return MediaStore.UNKNOWN_STRING;
        }
        if (cursor.getCount() <= 0) {
            if (default_name)
                return mContext.getString(R.string.unknown);
            else
                return MediaStore.UNKNOWN_STRING;
        } else {
            cursor.moveToFirst();
            String name = cursor.getString(0);
            cursor.close();
            if (name == null || MediaStore.UNKNOWN_STRING.equals(name)) {
                if (default_name)
                    return mContext.getString(R.string.unknown);
                else
                    return MediaStore.UNKNOWN_STRING;
            }
            return name;
        }
    }

    /**
     * @param playlist_id
     * @return playlist name
     */
    public static String getPlaylistName(Context mContext, long playlist_id) {
        String where = BaseColumns._ID + "=" + playlist_id;
        String[] cols = new String[] {
            PlaylistsColumns.NAME
        };
        Uri uri = Audio.Playlists.EXTERNAL_CONTENT_URI;
        Cursor cursor = mContext.getContentResolver().query(uri, cols, where, null, null);
        if (cursor == null){
            return "";
        }
        if (cursor.getCount() <= 0)
            return "";
        cursor.moveToFirst();
        String name = cursor.getString(0);
        cursor.close();
        return name;
    }

    /**
     * @param mContext
     * @param genre_id
     * @param default_name
     * @return genre name
     */
    public static String getGenreName(Context mContext, long genre_id, boolean default_name) {
        String where = BaseColumns._ID + "=" + genre_id;
        String[] cols = new String[] {
            GenresColumns.NAME
        };
        Uri uri = Audio.Genres.EXTERNAL_CONTENT_URI;
        Cursor cursor = mContext.getContentResolver().query(uri, cols, where, null, null);
        if (cursor == null){
            return MediaStore.UNKNOWN_STRING;
        }
        if (cursor.getCount() <= 0) {
            if (default_name)
                return mContext.getString(R.string.unknown);
            else
                return MediaStore.UNKNOWN_STRING;
        } else {
            cursor.moveToFirst();
            String name = cursor.getString(0);
            cursor.close();
            if (name == null || MediaStore.UNKNOWN_STRING.equals(name)) {
                if (default_name)
                    return mContext.getString(R.string.unknown);
                else
                    return MediaStore.UNKNOWN_STRING;
            }
            return name;
        }
    }

    /**
     * @param genre
     * @return parsed genre name
     */
    public static String parseGenreName(Context mContext, String genre) {
        int genre_id = -1;

        if (genre == null || genre.trim().length() <= 0)
            return mContext.getResources().getString(R.string.unknown);

        try {
            genre_id = Integer.parseInt(genre);
        } catch (NumberFormatException e) {
            return genre;
        }
        if (genre_id >= 0 && genre_id < GENRES_DB.length)
            return GENRES_DB[genre_id];
        else
            return mContext.getResources().getString(R.string.unknown);
    }

    /**
     * @return if music is playing
     */
    public static boolean isPlaying() {
        if (mService == null)
            return false;

        try {
            return mService.isPlaying();
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * @return current track's queue position
     */
    public static int getQueuePosition() {
        if (mService == null)
            return 0;
        try {
            return mService.getQueuePosition();
        } catch (RemoteException e) {
        }
        return 0;
    }

    /**
     * @param mContext
     * @param create_shortcut
     * @param list
     */
    public static void makePlaylistList(Context mContext, boolean create_shortcut,
            List<Map<String, String>> list) {

        Map<String, String> map;

        String[] cols = new String[] {
                Audio.Playlists._ID, Audio.Playlists.NAME
        };
        StringBuilder where = new StringBuilder();

        ContentResolver resolver = mContext.getContentResolver();
        if (resolver == null) {
            System.out.println("resolver = null");
        } else {
            where.append(Audio.Playlists.NAME + " != ''");
            where.append(" AND " + Audio.Playlists.NAME + " != '" + PLAYLIST_NAME_FAVORITES + "'");
            Cursor cur = resolver.query(Audio.Playlists.EXTERNAL_CONTENT_URI, cols,
                    where.toString(), null, Audio.Playlists.NAME);
            list.clear();

            // map = new HashMap<String, String>();
            // map.put("id", String.valueOf(PLAYLIST_FAVORITES));
            // map.put("name", mContext.getString(R.string.favorite));
            // list.add(map);

            map = new HashMap<String, String>();
            map.put("id", String.valueOf(PLAYLIST_QUEUE));
            map.put("name", mContext.getString(R.string.queue));
            list.add(map);

            map = new HashMap<String, String>();
            map.put("id", String.valueOf(PLAYLIST_NEW));
            map.put("name", mContext.getString(R.string.new_playlist));
            list.add(map);

            if (cur != null && cur.getCount() > 0) {
                cur.moveToFirst();
                while (!cur.isAfterLast()) {
                    map = new HashMap<String, String>();
                    map.put("id", String.valueOf(cur.getLong(0)));
                    map.put("name", cur.getString(1));
                    list.add(map);
                    cur.moveToNext();
                }
            }
            if (cur != null) {
                cur.close();
            }
        }
    }
    
    public static void notifyWidgets(String what){ 
        try {
        	mService.notifyChange(what);
        } catch (Exception e) {
        }
    }

}
