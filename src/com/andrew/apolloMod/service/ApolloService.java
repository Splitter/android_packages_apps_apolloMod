/* Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrew.apolloMod.service;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Random;
import java.util.Vector;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.MediaColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RemoteViews;

import com.andrew.apolloMod.IApolloService;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.app.widgets.AppWidget11;
import com.andrew.apolloMod.app.widgets.AppWidget41;
import com.andrew.apolloMod.app.widgets.AppWidget42;
import com.andrew.apolloMod.cache.ImageInfo;
import com.andrew.apolloMod.helpers.GetBitmapTask;
import com.andrew.apolloMod.helpers.GetBitmapTask.OnBitmapReadyListener;
import com.andrew.apolloMod.helpers.utils.ImageUtils;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.helpers.utils.VisualizerUtils;
import com.andrew.apolloMod.preferences.SharedPreferencesCompat;

import static com.andrew.apolloMod.Constants.APOLLO_PREFERENCES;
import static com.andrew.apolloMod.Constants.DATA_SCHEME;
import static com.andrew.apolloMod.Constants.SIZE_THUMB;
import static com.andrew.apolloMod.Constants.SRC_FIRST_AVAILABLE;
import static com.andrew.apolloMod.Constants.TYPE_ALBUM;

public class ApolloService extends Service implements GetBitmapTask.OnBitmapReadyListener {
    /**
     * used to specify whether enqueue() should start playing the new list of
     * files right away, next or once all the currently queued files have been
     * played
     */
    public static final int NOW = 1;

    public static final int NEXT = 2;

    public static final int LAST = 3;

    public static final int PLAYBACKSERVICE_STATUS = 1;

    public static final int SHUFFLE_NONE = 0;

    public static final int SHUFFLE_NORMAL = 1;

    public static final int SHUFFLE_AUTO = 2;

    public static final int REPEAT_NONE = 0;

    public static final int REPEAT_CURRENT = 1;

    public static final int REPEAT_ALL = 2;

    public static final String APOLLO_PACKAGE_NAME = "com.andrew.apolloMod";

    public static final String MUSIC_PACKAGE_NAME = "com.android.music";

    public static final String PLAYSTATE_CHANGED = "com.andrew.apolloMod.playstatechanged";

    public static final String META_CHANGED = "com.andrew.apolloMod.metachanged";

    public static final String FAVORITE_CHANGED = "com.andrew.apolloMod.favoritechanged";

    public static final String QUEUE_CHANGED = "com.andrew.apolloMod.queuechanged";

    public static final String REPEATMODE_CHANGED = "com.andrew.apolloMod.repeatmodechanged";

    public static final String SHUFFLEMODE_CHANGED = "com.andrew.apolloMod.shufflemodechanged";

    public static final String PROGRESSBAR_CHANGED = "com.andrew.apolloMod.progressbarchnaged";

    public static final String REFRESH_PROGRESSBAR = "com.andrew.apolloMod.refreshprogessbar";

    public static final String CYCLEREPEAT_ACTION = "com.andrew.apolloMod.musicservicecommand.cyclerepeat";

    public static final String TOGGLESHUFFLE_ACTION = "com.andrew.apolloMod.musicservicecommand.toggleshuffle";

    public static final String SERVICECMD = "com.andrew.apolloMod.musicservicecommand";

    public static final String CMDNAME = "command";

    public static final String CMDTOGGLEPAUSE = "togglepause";

    public static final String CMDSTOP = "stop";

    public static final String CMDPAUSE = "pause";

    public static final String CMDPLAY = "play";

    public static final String CMDPREVIOUS = "previous";

    public static final String CMDNEXT = "next";

    public static final String CMDNOTIF = "buttonId";

    public static final String CMDTOGGLEFAVORITE = "togglefavorite";

    public static final String CMDCYCLEREPEAT = "cyclerepeat";

    public static final String CMDTOGGLESHUFFLE = "toggleshuffle";

    public static final String TOGGLEPAUSE_ACTION = "com.andrew.apolloMod.musicservicecommand.togglepause";

    public static final String PAUSE_ACTION = "com.andrew.apolloMod.musicservicecommand.pause";

    public static final String PREVIOUS_ACTION = "com.andrew.apolloMod.musicservicecommand.previous";

    public static final String NEXT_ACTION = "com.andrew.apolloMod.musicservicecommand.next";

    private static final int TRACK_ENDED = 1;

    private static final int RELEASE_WAKELOCK = 2;

    private static final int SERVER_DIED = 3;

    private static final int FOCUSCHANGE = 4;

    private static final int FADEDOWN = 5;

    private static final int FADEUP = 6;

    private static final int TRACK_WENT_TO_NEXT = 7;

    private static final int MAX_HISTORY_SIZE = 100;

    private Notification status;

    private MultiPlayer mPlayer;

    private String mFileToPlay;

    private int mShuffleMode = SHUFFLE_NONE;

    private int mRepeatMode = REPEAT_NONE;

    private int mMediaMountedCount = 0;

    private long[] mAutoShuffleList = null;

    private long[] mPlayList = null;

    private int mPlayListLen = 0;

    private final Vector<Integer> mHistory = new Vector<Integer>(MAX_HISTORY_SIZE);

    private Cursor mCursor;

    private int mPlayPos = -1;

    private int mNextPlayPos = -1;

    private static final String LOGTAG = "MediaPlaybackService";

    private final Shuffler mRand = new Shuffler();

    private int mOpenFailedCounter = 0;

    String[] mCursorCols = new String[] {
            "audio._id AS _id", MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.IS_PODCAST,
            MediaStore.Audio.Media.BOOKMARK
    };

    private final static int IDCOLIDX = 0;

    private final static int PODCASTCOLIDX = 8;

    private final static int BOOKMARKCOLIDX = 9;

    private BroadcastReceiver mUnmountReceiver = null;

    private WakeLock mWakeLock;

    private int mServiceStartId = -1;

    private boolean mServiceInUse = false;

    private boolean mIsSupposedToBePlaying = false;

    @SuppressWarnings("unused")
	private boolean mQuietMode = false;

    private AudioManager mAudioManager;

    private boolean mQueueIsSaveable = true;

    // used to track what type of audio focus loss caused the playback to pause
    private boolean mPausedByTransientLossOfFocus = false;

    private SharedPreferences mPreferences;

    // We use this to distinguish between different cards when saving/restoring
    // playlists.
    // This will have to change if we want to support multiple simultaneous
    // cards.
    private int mCardId;

    private final AppWidget11 mAppWidgetProvider1x1 = AppWidget11.getInstance();

    private final AppWidget42 mAppWidgetProvider4x2 = AppWidget42.getInstance();

    private final AppWidget41 mAppWidgetProvider4x1 = AppWidget41.getInstance();

    private String mAlbumBitmapTag;

    private Bitmap mAlbumBitmap;
    
    private GetBitmapTask mAlbumBitmapTask;

    // interval after which we stop the service when idle
    private static final int IDLE_DELAY = 60000;

    private RemoteControlClient mRemoteControlClient;

    private final Handler mMediaplayerHandler = new Handler() {
        float mCurrentVolume = 1.0f;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FADEDOWN:
                    mCurrentVolume -= .05f;
                    if (mCurrentVolume > .2f) {
                        mMediaplayerHandler.sendEmptyMessageDelayed(FADEDOWN, 10);
                    } else {
                        mCurrentVolume = .2f;
                    }
                    mPlayer.setVolume(mCurrentVolume);
                    break;
                case FADEUP:
                    mCurrentVolume += .01f;
                    if (mCurrentVolume < 1.0f) {
                        mMediaplayerHandler.sendEmptyMessageDelayed(FADEUP, 10);
                    } else {
                        mCurrentVolume = 1.0f;
                    }
                    mPlayer.setVolume(mCurrentVolume);
                    break;
                case SERVER_DIED:
                    if (mIsSupposedToBePlaying) {
                        gotoNext(true);
                    } else {
                        // the server died when we were idle, so just
                        // reopen the same song (it will start again
                        // from the beginning though when the user
                        // restarts)
                        openCurrentAndNext();
                    }
                    break;
                case TRACK_WENT_TO_NEXT:
                    if (mNextPlayPos >= 0 && mPlayList != null) {
                        mPlayPos = mNextPlayPos;
                        if (mCursor != null) {
                            mCursor.close();
                            mCursor = null;
                        }
                        mCursor = getCursorForId(mPlayList[mPlayPos]);
                        updateAlbumBitmap();
                        notifyChange(META_CHANGED);
                        updateNotification();
                        setNextTrack();
                    }
                    break;
                case TRACK_ENDED:
                    if (mRepeatMode == REPEAT_CURRENT) {
                        seek(0);
                        play();
                    } else {
                        gotoNext(false);
                    }
                    break;
                case RELEASE_WAKELOCK:
                    mWakeLock.release();
                    break;

                case FOCUSCHANGE:
                    // This code is here so we can better synchronize it with
                    // the code that
                    // handles fade-in
                    switch (msg.arg1) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS");
                            if (isPlaying()) {
                                mPausedByTransientLossOfFocus = false;
                            }
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            mMediaplayerHandler.removeMessages(FADEUP);
                            mMediaplayerHandler.sendEmptyMessage(FADEDOWN);
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
                            if (isPlaying()) {
                                mPausedByTransientLossOfFocus = true;
                            }
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_GAIN");
                            if (!isPlaying() && mPausedByTransientLossOfFocus) {
                                mPausedByTransientLossOfFocus = false;
                                mCurrentVolume = 0f;
                                mPlayer.setVolume(mCurrentVolume);
                                play(); // also queues a fade-in
                            } else {
                                mMediaplayerHandler.removeMessages(FADEDOWN);
                                mMediaplayerHandler.sendEmptyMessage(FADEUP);
                            }
                            break;
                        default:
                            Log.e(LOGTAG, "Unknown audio focus change code");
                    }
                    break;

                default:
                    break;
            }
        }
    };

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                gotoNext(true);
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                prev();
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (isPlaying()) {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else if (CMDPLAY.equals(cmd)) {
                play();
            } else if (CMDSTOP.equals(cmd)) {
                pause();
                mPausedByTransientLossOfFocus = false;
                seek(0);
            } else if (CMDTOGGLEFAVORITE.equals(cmd)) {
                if (!isFavorite()) {
                    addToFavorites();
                } else {
                    removeFromFavorites();
                }
            } else if (CMDCYCLEREPEAT.equals(cmd) || CYCLEREPEAT_ACTION.equals(action)) {
                cycleRepeat();
            } else if (CMDTOGGLESHUFFLE.equals(cmd) || TOGGLESHUFFLE_ACTION.equals(action)) {
                toggleShuffle();
            } else if (AppWidget42.CMDAPPWIDGETUPDATE.equals(cmd)) {
                int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider4x2.performUpdate(ApolloService.this, appWidgetIds);
            } else if (AppWidget41.CMDAPPWIDGETUPDATE.equals(cmd)) {
                int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider4x1.performUpdate(ApolloService.this, appWidgetIds);
            } else if (AppWidget11.CMDAPPWIDGETUPDATE.equals(cmd)) {
                int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                mAppWidgetProvider1x1.performUpdate(ApolloService.this, appWidgetIds);
            }
        }
    };

    private final OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            mMediaplayerHandler.obtainMessage(FOCUSCHANGE, focusChange, 0).sendToTarget();
        }
    };

    public ApolloService() {
    }

    @SuppressLint({ "WorldWriteableFiles", "WorldReadableFiles" })
	@Override
    public void onCreate() {
        super.onCreate();

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        ComponentName rec = new ComponentName(getPackageName(),
                MediaButtonIntentReceiver.class.getName());
        mAudioManager.registerMediaButtonEventReceiver(rec);
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(rec);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0,
                mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteControlClient = new RemoteControlClient(mediaPendingIntent);
        mAudioManager.registerRemoteControlClient(mRemoteControlClient);

        int flags = RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
                | RemoteControlClient.FLAG_KEY_MEDIA_NEXT | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
                | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_STOP;
        mRemoteControlClient.setTransportControlFlags(flags);

        mPreferences = getSharedPreferences(APOLLO_PREFERENCES, MODE_WORLD_READABLE
                | MODE_WORLD_WRITEABLE);
        mCardId = MusicUtils.getCardId(this);

        registerExternalStorageListener();

        // Needs to be done in this thread, since otherwise
        // ApplicationContext.getPowerManager() crashes.
        mPlayer = new MultiPlayer();
        mPlayer.setHandler(mMediaplayerHandler);

        reloadQueue();
        notifyChange(QUEUE_CHANGED);
        notifyChange(META_CHANGED);

        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(SERVICECMD);
        commandFilter.addAction(TOGGLEPAUSE_ACTION);
        commandFilter.addAction(PAUSE_ACTION);
        commandFilter.addAction(NEXT_ACTION);
        commandFilter.addAction(PREVIOUS_ACTION);
        commandFilter.addAction(CYCLEREPEAT_ACTION);
        commandFilter.addAction(TOGGLESHUFFLE_ACTION);
        registerReceiver(mIntentReceiver, commandFilter);

        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        mWakeLock.setReferenceCounted(false);

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that
        // case.
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
    }

    @Override
    public void onDestroy() {
        // Check that we're not being destroyed while something is still
        // playing.
        if (mIsSupposedToBePlaying) {
            Log.e(LOGTAG, "Service being destroyed while still playing.");
        }
        // release all MediaPlayer resources, including the native player and
        // wakelocks
        Intent i = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
        i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(i);
        mPlayer.release();
        mPlayer = null;

        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);

        // make sure there aren't any other messages coming
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mMediaplayerHandler.removeCallbacksAndMessages(null);

        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
        updateAlbumBitmap();

        unregisterReceiver(mIntentReceiver);
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
            mUnmountReceiver = null;
        }
        mWakeLock.release();
        super.onDestroy();
    }

    private final char hexdigits[] = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    private void saveQueue(boolean full) {
        if (!mQueueIsSaveable) {
            return;
        }

        Editor ed = mPreferences.edit();
        // long start = System.currentTimeMillis();
        if (full) {
            StringBuilder q = new StringBuilder();

            // The current playlist is saved as a list of "reverse hexadecimal"
            // numbers, which we can generate faster than normal decimal or
            // hexadecimal numbers, which in turn allows us to save the playlist
            // more often without worrying too much about performance.
            // (saving the full state takes about 40 ms under no-load conditions
            // on the phone)
            int len = mPlayListLen;
            for (int i = 0; i < len; i++) {
                long n = mPlayList[i];
                if (n < 0) {
                    continue;
                } else if (n == 0) {
                    q.append("0;");
                } else {
                    while (n != 0) {
                        int digit = (int)(n & 0xf);
                        n >>>= 4;
                        q.append(hexdigits[digit]);
                    }
                    q.append(";");
                }
            }
            // Log.i("@@@@ service", "created queue string in " +
            // (System.currentTimeMillis() - start) + " ms");
            ed.putString("queue", q.toString());
            ed.putInt("cardid", mCardId);
            if (mShuffleMode != SHUFFLE_NONE) {
                // In shuffle mode we need to save the history too
                len = mHistory.size();
                q.setLength(0);
                for (int i = 0; i < len; i++) {
                    int n = mHistory.get(i);
                    if (n == 0) {
                        q.append("0;");
                    } else {
                        while (n != 0) {
                            int digit = (n & 0xf);
                            n >>>= 4;
                            q.append(hexdigits[digit]);
                        }
                        q.append(";");
                    }
                }
                ed.putString("history", q.toString());
            }
        }
        ed.putInt("curpos", mPlayPos);
        if (mPlayer.isInitialized()) {
            ed.putLong("seekpos", mPlayer.position());
        }
        ed.putInt("repeatmode", mRepeatMode);
        ed.putInt("shufflemode", mShuffleMode);
        SharedPreferencesCompat.apply(ed);

        // Log.i("@@@@ service", "saved state in " + (System.currentTimeMillis()
        // - start) + " ms");
    }

    private void reloadQueue() {
        String q = null;

        int id = mCardId;
        if (mPreferences.contains("cardid")) {
            id = mPreferences.getInt("cardid", ~mCardId);
        }
        if (id == mCardId) {
            // Only restore the saved playlist if the card is still
            // the same one as when the playlist was saved
            q = mPreferences.getString("queue", "");
        }
        int qlen = q != null ? q.length() : 0;
        if (qlen > 1) {
            // Log.i("@@@@ service", "loaded queue: " + q);
            int plen = 0;
            int n = 0;
            int shift = 0;
            for (int i = 0; i < qlen; i++) {
                char c = q.charAt(i);
                if (c == ';') {
                    ensurePlayListCapacity(plen + 1);
                    mPlayList[plen] = n;
                    plen++;
                    n = 0;
                    shift = 0;
                } else {
                    if (c >= '0' && c <= '9') {
                        n += ((c - '0') << shift);
                    } else if (c >= 'a' && c <= 'f') {
                        n += ((10 + c - 'a') << shift);
                    } else {
                        // bogus playlist data
                        plen = 0;
                        break;
                    }
                    shift += 4;
                }
            }
            mPlayListLen = plen;

            int pos = mPreferences.getInt("curpos", 0);
            if (pos < 0 || pos >= mPlayListLen) {
                // The saved playlist is bogus, discard it
                mPlayListLen = 0;
                return;
            }
            mPlayPos = pos;

            // When reloadQueue is called in response to a card-insertion,
            // we might not be able to query the media provider right away.
            // To deal with this, try querying for the current file, and if
            // that fails, wait a while and try again. If that too fails,
            // assume there is a problem and don't restore the state.
            Cursor crsr = MusicUtils.query(this, Audio.Media.EXTERNAL_CONTENT_URI, new String[] {
                "_id"
            }, "_id=" + mPlayList[mPlayPos], null, null);
            if (crsr == null || crsr.getCount() == 0) {
                // wait a bit and try again
                SystemClock.sleep(3000);
                crsr = getContentResolver().query(Audio.Media.EXTERNAL_CONTENT_URI, mCursorCols,
                        "_id=" + mPlayList[mPlayPos], null, null);
            }
            if (crsr != null) {
                crsr.close();
            }

            // Make sure we don't auto-skip to the next song, since that
            // also starts playback. What could happen in that case is:
            // - music is paused
            // - go to UMS and delete some files, including the currently
            // playing one
            // - come back from UMS
            // (time passes)
            // - music app is killed for some reason (out of memory)
            // - music service is restarted, service restores state, doesn't
            // find
            // the "current" file, goes to the next and: playback starts on its
            // own, potentially at some random inconvenient time.
            mOpenFailedCounter = 20;
            mQuietMode = true;
            openCurrentAndNext();
            mQuietMode = false;
            if (!mPlayer.isInitialized()) {
                // couldn't restore the saved state
                mPlayListLen = 0;
                return;
            }

            long seekpos = mPreferences.getLong("seekpos", 0);
            seek(seekpos >= 0 && seekpos < duration() ? seekpos : 0);
            Log.d(LOGTAG, "restored queue, currently at position " + position() + "/" + duration()
                    + " (requested " + seekpos + ")");

            int repmode = mPreferences.getInt("repeatmode", REPEAT_NONE);
            if (repmode != REPEAT_ALL && repmode != REPEAT_CURRENT) {
                repmode = REPEAT_NONE;
            }
            mRepeatMode = repmode;

            int shufmode = mPreferences.getInt("shufflemode", SHUFFLE_NONE);
            if (shufmode != SHUFFLE_AUTO && shufmode != SHUFFLE_NORMAL) {
                shufmode = SHUFFLE_NONE;
            }
            if (shufmode != SHUFFLE_NONE) {
                // in shuffle mode we need to restore the history too
                q = mPreferences.getString("history", "");
                qlen = q != null ? q.length() : 0;
                if (qlen > 1) {
                    plen = 0;
                    n = 0;
                    shift = 0;
                    mHistory.clear();
                    for (int i = 0; i < qlen; i++) {
                        char c = q.charAt(i);
                        if (c == ';') {
                            if (n >= mPlayListLen) {
                                // bogus history data
                                mHistory.clear();
                                break;
                            }
                            mHistory.add(n);
                            n = 0;
                            shift = 0;
                        } else {
                            if (c >= '0' && c <= '9') {
                                n += ((c - '0') << shift);
                            } else if (c >= 'a' && c <= 'f') {
                                n += ((10 + c - 'a') << shift);
                            } else {
                                // bogus history data
                                mHistory.clear();
                                break;
                            }
                            shift += 4;
                        }
                    }
                }
            }
            if (shufmode == SHUFFLE_AUTO) {
                if (!makeAutoShuffleList()) {
                    shufmode = SHUFFLE_NONE;
                }
            }
            mShuffleMode = shufmode;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mServiceInUse = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        mDelayedStopHandler.removeCallbacksAndMessages(null);

        if (intent != null) {
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");

            if (CMDNEXT.equals(cmd) || NEXT_ACTION.equals(action)) {
                gotoNext(true);
            } else if (CMDPREVIOUS.equals(cmd) || PREVIOUS_ACTION.equals(action)) {
                if (position() < 2000) {
                    prev();
                } else {
                    seek(0);
                    play();
                }
            } else if (CMDTOGGLEPAUSE.equals(cmd) || TOGGLEPAUSE_ACTION.equals(action)) {
                if (mIsSupposedToBePlaying) {
                    pause();
                    mPausedByTransientLossOfFocus = false;
                } else {
                    play();
                }
            } else if (CMDPAUSE.equals(cmd) || PAUSE_ACTION.equals(action)) {
                pause();
                mPausedByTransientLossOfFocus = false;
            } else if (CMDPLAY.equals(cmd)) {
                play();
            } else if (CMDSTOP.equals(cmd)) {
                pause();
                if (intent.getIntExtra(CMDNOTIF, 0) == 3) {
                    stopForeground(true);
                }
                mPausedByTransientLossOfFocus = false;
                seek(0);
            } else if (CMDTOGGLEFAVORITE.equals(cmd)) {
                if (!isFavorite()) {
                    addToFavorites();
                } else {
                    removeFromFavorites();
                }
            } else if (CMDCYCLEREPEAT.equals(cmd) || CYCLEREPEAT_ACTION.equals(action)) {
                cycleRepeat();
            } else if (CMDTOGGLESHUFFLE.equals(cmd) || TOGGLESHUFFLE_ACTION.equals(action)) {
                toggleShuffle();
            }
        }

        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mServiceInUse = false;

        // Take a snapshot of the current playlist
        saveQueue(true);

        if (mIsSupposedToBePlaying || mPausedByTransientLossOfFocus) {
            // something is currently playing, or will be playing once
            // an in-progress action requesting audio focus ends, so don't stop
            // the service now.
            return true;
        }

        // If there is a playlist but playback is paused, then wait a while
        // before stopping the service, so that pause/resume isn't slow.
        // Also delay stopping the service if we're transitioning between
        // tracks.
        if (mPlayListLen > 0 || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
            Message msg = mDelayedStopHandler.obtainMessage();
            mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
            return true;
        }

        // No active playlist, OK to stop the service right now
        stopSelf(mServiceStartId);
        return true;
    }

    private final Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // Check again to make sure nothing is playing right now
            if (isPlaying() || mPausedByTransientLossOfFocus || mServiceInUse
                    || mMediaplayerHandler.hasMessages(TRACK_ENDED)) {
                return;
            }
            // save the queue again, because it might have changed
            // since the user exited the music app (because of
            // party-shuffle or because the play-position changed)
            saveQueue(true);
            stopSelf(mServiceStartId);
        }
    };

    /**
     * Called when we receive a ACTION_MEDIA_EJECT notification.
     * 
     * @param storagePath path to mount point for the removed media
     */
    public void closeExternalStorageFiles(String storagePath) {
        // stop playback and clean up if the SD card is going to be unmounted.
        stop(true);
        notifyChange(QUEUE_CHANGED);
        notifyChange(META_CHANGED);
    }

    /**
     * Registers an intent to listen for ACTION_MEDIA_EJECT notifications. The
     * intent will call closeExternalStorageFiles() if the external media is
     * going to be ejected, so applications can clean up any files they have
     * open.
     */
    public void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                        saveQueue(true);
                        mQueueIsSaveable = false;
                        closeExternalStorageFiles(intent.getData().getPath());
                    } else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                        mMediaMountedCount++;
                        mCardId = MusicUtils.getCardId(ApolloService.this);
                        reloadQueue();
                        mQueueIsSaveable = true;
                        notifyChange(QUEUE_CHANGED);
                        notifyChange(META_CHANGED);
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            iFilter.addDataScheme(DATA_SCHEME);
            registerReceiver(mUnmountReceiver, iFilter);
        }
    }

    /**
     * Notify the change-receivers that something has changed. The intent that
     * is sent contains the following data for the currently playing track: "id"
     * - Integer: the database row ID "artist" - String: the name of the artist
     * "album" - String: the name of the album "track" - String: the name of the
     * track The intent has an action that is one of
     * "com.andrew.apolloMod.metachanged" "com.andrew.apolloMod.queuechanged",
     * "com.andrew.apolloMod.playbackcomplete" "com.andrew.apolloMod.playstatechanged"
     * respectively indicating that a new track has started playing, that the
     * playback queue has changed, that playback has stopped because the last
     * file in the list has been played, or that the play-state changed
     * (paused/resumed).
     */
    public void notifyChange(String what) {

        Intent i = new Intent(what);
        i.putExtra("id", Long.valueOf(getAudioId()));
        i.putExtra("artist", getArtistName());
        i.putExtra("album", getAlbumName());
        i.putExtra("track", getTrackName());
        i.putExtra("playing", mIsSupposedToBePlaying);
        i.putExtra("isfavorite", isFavorite());
        sendStickyBroadcast(i);

        i = new Intent(i);
        i.setAction(what.replace(APOLLO_PACKAGE_NAME, MUSIC_PACKAGE_NAME));
        sendStickyBroadcast(i);

        if (what.equals(PLAYSTATE_CHANGED)) {
            mRemoteControlClient
                    .setPlaybackState(mIsSupposedToBePlaying ? RemoteControlClient.PLAYSTATE_PLAYING
                            : RemoteControlClient.PLAYSTATE_PAUSED);
        } else if (what.equals(META_CHANGED)) {
            RemoteControlClient.MetadataEditor ed = mRemoteControlClient.editMetadata(true);
            ed.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, getTrackName());
            ed.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, getAlbumName());
            ed.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, getArtistName());
            ed.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, duration());
            Bitmap b = getAlbumBitmap();
            if (b != null) {
                ed.putBitmap(MetadataEditor.BITMAP_KEY_ARTWORK, b);
            }
            ed.apply();
        }

        if (what.equals(QUEUE_CHANGED)) {
            saveQueue(true);
        } else {
            saveQueue(false);
        }
        mAppWidgetProvider1x1.notifyChange(this, what);
        mAppWidgetProvider4x1.notifyChange(this, what);
        mAppWidgetProvider4x2.notifyChange(this, what);

    }

    private void ensurePlayListCapacity(int size) {
        if (mPlayList == null || size > mPlayList.length) {
            // reallocate at 2x requested size so we don't
            // need to grow and copy the array for every
            // insert
            long[] newlist = new long[size * 2];
            int len = mPlayList != null ? mPlayList.length : mPlayListLen;
            for (int i = 0; i < len; i++) {
                newlist[i] = mPlayList[i];
            }
            mPlayList = newlist;
        }
        // FIXME: shrink the array when the needed size is much smaller
        // than the allocated size
    }

    // insert the list of songs at the specified position in the playlist
    private void addToPlayList(long[] list, int position) {
        int addlen = list.length;
        if (position < 0) { // overwrite
            mPlayListLen = 0;
            position = 0;
        }
        ensurePlayListCapacity(mPlayListLen + addlen);
        if (position > mPlayListLen) {
            position = mPlayListLen;
        }

        // move part of list after insertion point
        int tailsize = mPlayListLen - position;
        for (int i = tailsize; i > 0; i--) {
            mPlayList[position + i] = mPlayList[position + i - addlen];
        }

        // copy list into playlist
        for (int i = 0; i < addlen; i++) {
            mPlayList[position + i] = list[i];
        }
        mPlayListLen += addlen;
        if (mPlayListLen == 0) {
            mCursor.close();
            mCursor = null;
            updateAlbumBitmap();
            notifyChange(META_CHANGED);
        }
    }

    /**
     * Appends a list of tracks to the current playlist. If nothing is playing
     * currently, playback will be started at the first track. If the action is
     * NOW, playback will switch to the first of the new tracks immediately.
     * 
     * @param list The list of tracks to append.
     * @param action NOW, NEXT or LAST
     */
    public void enqueue(long[] list, int action) {
        synchronized (this) {
            if (action == NEXT && mPlayPos + 1 < mPlayListLen) {
                addToPlayList(list, mPlayPos + 1);
                notifyChange(QUEUE_CHANGED);
            } else {
                // action == LAST || action == NOW || mPlayPos + 1 ==
                // mPlayListLen
                addToPlayList(list, Integer.MAX_VALUE);
                notifyChange(QUEUE_CHANGED);
                if (action == NOW) {
                    mPlayPos = mPlayListLen - list.length;
                    openCurrentAndNext();
                    play();
                    notifyChange(META_CHANGED);
                    return;
                }
            }
            if (mPlayPos < 0) {
                mPlayPos = 0;
                openCurrentAndNext();
                play();
                notifyChange(META_CHANGED);
            }
        }
    }

    /**
     * Replaces the current playlist with a new list, and prepares for starting
     * playback at the specified position in the list, or a random position if
     * the specified position is 0.
     * 
     * @param list The new list of tracks.
     */
    public void open(long[] list, int position) {
        synchronized (this) {
            if (mShuffleMode == SHUFFLE_AUTO) {
                mShuffleMode = SHUFFLE_NORMAL;
            }
            long oldId = getAudioId();
            int listlength = list.length;
            boolean newlist = true;
            if (mPlayListLen == listlength) {
                // possible fast path: list might be the same
                newlist = false;
                for (int i = 0; i < listlength; i++) {
                    if (list[i] != mPlayList[i]) {
                        newlist = true;
                        break;
                    }
                }
            }
            if (newlist) {
                addToPlayList(list, -1);
                notifyChange(QUEUE_CHANGED);
            }
            if (position >= 0) {
                mPlayPos = position;
            } else {
                mPlayPos = mRand.nextInt(mPlayListLen);
            }
            mHistory.clear();

            saveBookmarkIfNeeded();
            openCurrentAndNext();
            if (oldId != getAudioId()) {
                notifyChange(META_CHANGED);
            }
        }
    }

    /**
     * Returns the current play list
     * 
     * @return An array of integers containing the IDs of the tracks in the play
     *         list
     */
    public long[] getQueue() {
        synchronized (this) {
            int len = mPlayListLen;
            long[] list = new long[len];
            for (int i = 0; i < len; i++) {
                list[i] = mPlayList[i];
            }
            return list;
        }
    }

    private Cursor getCursorForId(long lid) {
        String id = String.valueOf(lid);

        Cursor c = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                mCursorCols, "_id=" + id , null, null);
        if (c != null) {
            c.moveToFirst();
        }
        return c;
    }

    private void openCurrentAndNext() {
        synchronized (this) {
            if (mCursor != null) {
                mCursor.close();
                mCursor = null;
            }

            if (mPlayListLen == 0) {
                return;
            }
            stop(false);
            mCursor = getCursorForId(mPlayList[mPlayPos]);
            if (mCursor == null ) {
                return;
            }
            while(!open(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + mCursor.getLong(IDCOLIDX))) {
                if (mOpenFailedCounter++ < 10 &&  mPlayListLen > 1) {
                    int pos = getNextPosition(false);
                    if (pos < 0) {
                        gotoIdleState();
                        if (mIsSupposedToBePlaying) {
                            mIsSupposedToBePlaying = false;
                            notifyChange(PLAYSTATE_CHANGED);
                        }
                        return;
                    }
                    mPlayPos = pos;
                    stop(false);
                    mPlayPos = pos;
                    mCursor = getCursorForId(mPlayList[mPlayPos]);
                } else {
                    mOpenFailedCounter = 0;
                    Log.d(LOGTAG, "Failed to open file for playback");
                    return;
                }
            }

            updateAlbumBitmap();

            // go to bookmark if needed
            if (isPodcast()) {
                long bookmark = getBookmark();
                // Start playing a little bit before the bookmark,
                // so it's easier to get back in to the narrative.
                seek(bookmark - 5000);
            }
            setNextTrack();

        }
    }

    private void setNextTrack() {
        mNextPlayPos = getNextPosition(false);
        if (mNextPlayPos >= 0 && mPlayList != null) {
            long id = mPlayList[mNextPlayPos];
            mPlayer.setNextDataSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + id);
        }
    }

    /**
     * Opens the specified file and readies it for playback.
     * 
     * @param path The full path of the file to be opened.
     */
    public boolean open(String path) {
        synchronized (this) {
            if (path == null) {
                return false;
            }

            // if mCursor is null, try to associate path with a database cursor
            if (mCursor == null) {

                ContentResolver resolver = getContentResolver();
                Uri uri;
                String where;
                String selectionArgs[];
                if (path.startsWith("content://media/")) {
                    uri = Uri.parse(path);
                    where = null;
                    selectionArgs = null;
                } else {
                    // Remove schema for search in the database
                    // Otherwise the file will not found
                    String data = path;
                    if( data.startsWith("file://") ){
                        data = data.substring(7);
                    }
                    uri = MediaStore.Audio.Media.getContentUriForPath(path);
                    where = MediaColumns.DATA + "=?";
                    selectionArgs = new String[] {
                        data
                    };
                }

                try {
                    mCursor = resolver.query(uri, mCursorCols, where, selectionArgs, null);
                    if (mCursor != null) {
                        if (mCursor.getCount() == 0) {
                            mCursor.close();
                            mCursor = null;
                        } else {
                            mCursor.moveToNext();
                            ensurePlayListCapacity(1);
                            mPlayListLen = 1;
                            mPlayList[0] = mCursor.getLong(IDCOLIDX);
                            mPlayPos = 0;
                        }
                    }
                } catch (UnsupportedOperationException ex) {
                }

                updateAlbumBitmap();
            }
            mFileToPlay = path;
            mPlayer.setDataSource(mFileToPlay);
            if (mPlayer.isInitialized()) {
                mOpenFailedCounter = 0;
                return true;
            }
            stop(true);
            return false;
        }
    }

    /**
     * Method that query the media database for search a path an translate
     * to the internal media id
     *
     * @param path The path to search
     * @return long The id of the resource, or -1 if not found
     */
    public long getIdFromPath(String path) {
        try {
            // Remove schema for search in the database
            // Otherwise the file will not found
            String data = path;
            if( data.startsWith("file://") ){
                data = data.substring(7);
            }
            ContentResolver resolver = getContentResolver();
            String where = MediaColumns.DATA + "=?";
            String selectionArgs[] = new String[] {
                data
            };
            Cursor cursor =
                    resolver.query(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            mCursorCols, where, selectionArgs, null);
            try {
                if (cursor == null || cursor.getCount() == 0) {
                    return -1;
                }
                cursor.moveToNext();
                return cursor.getLong(IDCOLIDX);
            } finally {
                try {
                    if( cursor != null )
                        cursor.close();
                } catch (Exception ex) {
                }
            }
        } catch (UnsupportedOperationException ex) {
        }
        return -1;
    }

    /**
     * Starts playback of a previously opened file.
     */
    public void play() {
        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        mAudioManager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(),
                MediaButtonIntentReceiver.class.getName()));

        if (mPlayer.isInitialized()) {
            // if we are at the end of the song, go to the next song first

            mPlayer.start();
            // make sure we fade in, in case a previous fadein was stopped
            // because
            // of another focus loss
            mMediaplayerHandler.removeMessages(FADEDOWN);
            mMediaplayerHandler.sendEmptyMessage(FADEUP);

            updateNotification();
            if (!mIsSupposedToBePlaying) {
                mIsSupposedToBePlaying = true;
                notifyChange(PLAYSTATE_CHANGED);
            }
        } else if (mPlayListLen <= 0) {
            // This is mostly so that if you press 'play' on a bluetooth headset
            // without every having played anything before, it will still play
            // something.
            setShuffleMode(SHUFFLE_AUTO);
        }
    }

    private void updateNotification() {
        Bitmap b = getAlbumBitmap();
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.status_bar);
        RemoteViews bigViews = new RemoteViews(getPackageName(), R.layout.status_bar_expanded);
        
        if (b != null) {
            views.setViewVisibility(R.id.status_bar_icon, View.GONE);
            views.setViewVisibility(R.id.status_bar_album_art, View.VISIBLE);
            views.setImageViewBitmap(R.id.status_bar_album_art, b);
            bigViews.setImageViewBitmap(R.id.status_bar_album_art, b);
        } else {
            views.setViewVisibility(R.id.status_bar_icon, View.VISIBLE);
            views.setViewVisibility(R.id.status_bar_album_art, View.GONE);
        }
        
        ComponentName rec = new ComponentName(getPackageName(),
                MediaButtonIntentReceiver.class.getName());
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.putExtra(CMDNOTIF, 1);
        mediaButtonIntent.setComponent(rec);
        KeyEvent mediaKey = new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        mediaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                1, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        views.setOnClickPendingIntent(R.id.status_bar_play, mediaPendingIntent);
        bigViews.setOnClickPendingIntent(R.id.status_bar_play, mediaPendingIntent);
        
        mediaButtonIntent.putExtra(CMDNOTIF, 2);
        mediaKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
        mediaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey);
        mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 2,
                mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        views.setOnClickPendingIntent(R.id.status_bar_next, mediaPendingIntent);
        bigViews.setOnClickPendingIntent(R.id.status_bar_next, mediaPendingIntent);

        mediaButtonIntent.putExtra(CMDNOTIF, 4);
        mediaKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        mediaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey);
        mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 4,
                mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        bigViews.setOnClickPendingIntent(R.id.status_bar_prev, mediaPendingIntent);
        
        mediaButtonIntent.putExtra(CMDNOTIF, 3);
        mediaKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP);
        mediaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey);
        mediaPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 3,
                mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        views.setOnClickPendingIntent(R.id.status_bar_collapse, mediaPendingIntent);
        bigViews.setOnClickPendingIntent(R.id.status_bar_collapse, mediaPendingIntent);
        
        views.setImageViewResource(R.id.status_bar_play, R.drawable.apollo_holo_dark_pause);
        bigViews.setImageViewResource(R.id.status_bar_play, R.drawable.apollo_holo_dark_pause);

        views.setTextViewText(R.id.status_bar_track_name, getTrackName());
        bigViews.setTextViewText(R.id.status_bar_track_name, getTrackName());
        
        views.setTextViewText(R.id.status_bar_artist_name, getArtistName());
        bigViews.setTextViewText(R.id.status_bar_artist_name, getArtistName());
        
        bigViews.setTextViewText(R.id.status_bar_album_name, getAlbumName());
        
        status = new Notification.Builder(this).build();
        status.contentView = views;
        status.bigContentView = bigViews;
        status.flags = Notification.FLAG_ONGOING_EVENT;
        status.icon = R.drawable.stat_notify_music;
        status.contentIntent = PendingIntent
                .getActivity(this, 0, new Intent("com.andrew.apolloMod.PLAYBACK_VIEWER")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
        startForeground(PLAYBACKSERVICE_STATUS, status);
    }
    
    private void stop(boolean remove_status_icon) {
        if (mPlayer.isInitialized()) {
            mPlayer.stop();
        }
        mFileToPlay = null;
        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
            updateAlbumBitmap();
        }
        if (remove_status_icon) {
            gotoIdleState();
        } else {
            stopForeground(false);
        }
        if (remove_status_icon) {
            mIsSupposedToBePlaying = false;
        }
    }

    /**
     * Stops playback.
     */
    public void stop() {
        stop(true);
    }

    /**
     * Pauses playback (call play() to resume)
     */
    public void pause() {
        synchronized (this) {
            mMediaplayerHandler.removeMessages(FADEUP);
            if (mIsSupposedToBePlaying) {
                mPlayer.pause();
                gotoIdleState();
                mIsSupposedToBePlaying = false;
                notifyChange(PLAYSTATE_CHANGED);
                saveBookmarkIfNeeded();
            }
        }
    }

    /**
     * Returns whether something is currently playing
     * 
     * @return true if something is playing (or will be playing shortly, in case
     *         we're currently transitioning between tracks), false if not.
     */
    public boolean isPlaying() {
        return mIsSupposedToBePlaying;
    }

    /*
     * Desired behavior for prev/next/shuffle: - NEXT will move to the next
     * track in the list when not shuffling, and to a track randomly picked from
     * the not-yet-played tracks when shuffling. If all tracks have already been
     * played, pick from the full set, but avoid picking the previously played
     * track if possible. - when shuffling, PREV will go to the previously
     * played track. Hitting PREV again will go to the track played before that,
     * etc. When the start of the history has been reached, PREV is a no-op.
     * When not shuffling, PREV will go to the sequentially previous track (the
     * difference with the shuffle-case is mainly that when not shuffling, the
     * user can back up to tracks that are not in the history). Example: When
     * playing an album with 10 tracks from the start, and enabling shuffle
     * while playing track 5, the remaining tracks (6-10) will be shuffled, e.g.
     * the final play order might be 1-2-3-4-5-8-10-6-9-7. When hitting 'prev' 8
     * times while playing track 7 in this example, the user will go to tracks
     * 9-6-10-8-5-4-3-2. If the user then hits 'next', a random track will be
     * picked again. If at any time user disables shuffling the next/previous
     * track will be picked in sequential order again.
     */

    public void prev() {
        synchronized (this) {
            if (mShuffleMode == SHUFFLE_NORMAL) {
                // go to previously-played track and remove it from the history
                int histsize = mHistory.size();
                if (histsize == 0) {
                    // prev is a no-op
                    return;
                }
                Integer pos = mHistory.remove(histsize - 1);
                mPlayPos = pos.intValue();
            } else {
                if (mPlayPos > 0) {
                    mPlayPos--;
                } else {
                    mPlayPos = mPlayListLen - 1;
                }
            }
            saveBookmarkIfNeeded();
            stop(false);
            openCurrentAndNext();
            play();

            notifyChange(META_CHANGED);
        }
    }

    /**
     * Get the next position to play. Note that this may actually modify mPlayPos
     * if playback is in SHUFFLE_AUTO mode and the shuffle list window needed to
     * be adjusted. Either way, the return value is the next value that should be
     * assigned to mPlayPos;
     */
    private int getNextPosition(boolean force) {
        if (mRepeatMode == REPEAT_CURRENT) {
            if (mPlayPos < 0) return 0;
            return mPlayPos;
        } else if (mShuffleMode == SHUFFLE_NORMAL) {
            // Pick random next track from the not-yet-played ones
            // TODO: make it work right after adding/removing items in the queue.

            // Store the current file in the history, but keep the history at a
            // reasonable size
            if (mPlayPos >= 0) {
                mHistory.add(mPlayPos);
            }
            if (mHistory.size() > MAX_HISTORY_SIZE) {
                mHistory.removeElementAt(0);
            }

            int numTracks = mPlayListLen;
            int[] tracks = new int[numTracks];
            for (int i=0;i < numTracks; i++) {
                tracks[i] = i;
            }

            int numHistory = mHistory.size();
            int numUnplayed = numTracks;
            for (int i=0;i < numHistory; i++) {
                int idx = mHistory.get(i).intValue();
                if (idx < numTracks && tracks[idx] >= 0) {
                    numUnplayed--;
                    tracks[idx] = -1;
                }
            }

            // 'numUnplayed' now indicates how many tracks have not yet
            // been played, and 'tracks' contains the indices of those
            // tracks.
            if (numUnplayed <=0) {
                // everything's already been played
                if (mRepeatMode == REPEAT_ALL || force) {
                    //pick from full set
                    numUnplayed = numTracks;
                    for (int i=0;i < numTracks; i++) {
                        tracks[i] = i;
                    }
                } else {
                    // all done
                    return -1;
                }
            }
            int skip = mRand.nextInt(numUnplayed);
            int cnt = -1;
            while (true) {
                while (tracks[++cnt] < 0)
                    ;
                skip--;
                if (skip < 0) {
                    break;
                }
            }
            return cnt;
        } else if (mShuffleMode == SHUFFLE_AUTO) {
            doAutoShuffleUpdate();
            return mPlayPos + 1;
        } else {
            if (mPlayPos >= mPlayListLen - 1) {
                // we're at the end of the list
                if (mRepeatMode == REPEAT_NONE && !force) {
                    // all done
                    return -1;
                } else if (mRepeatMode == REPEAT_ALL || force) {
                    return 0;
                }
                return -1;
            } else {
                return mPlayPos + 1;
            }
        }
    }

    public void gotoNext(boolean force) {
        synchronized (this) {
            if (mPlayListLen <= 0) {
                Log.d(LOGTAG, "No play queue");
                return;
            }

            int pos = getNextPosition(force);
            if (pos < 0) {
                gotoIdleState();
                if (mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = false;
                    notifyChange(PLAYSTATE_CHANGED);
                }
                return;
            }
            mPlayPos = pos;
            saveBookmarkIfNeeded();
            stop(false);
            mPlayPos = pos;
            openCurrentAndNext();
            play();
            notifyChange(META_CHANGED);
        }
    }

    public void cycleRepeat() {
        if (mRepeatMode == REPEAT_NONE) {
            setRepeatMode(REPEAT_ALL);
        } else if (mRepeatMode == REPEAT_ALL) {
            setRepeatMode(REPEAT_CURRENT);
            if (mShuffleMode != SHUFFLE_NONE) {
                setShuffleMode(SHUFFLE_NONE);
            }
        } else {
            setRepeatMode(REPEAT_NONE);
        }
    }

    public void toggleShuffle() {
        if (mShuffleMode == SHUFFLE_NONE) {
            setShuffleMode(SHUFFLE_NORMAL);
            if (mRepeatMode == REPEAT_CURRENT) {
                setRepeatMode(REPEAT_ALL);
            }
        } else if (mShuffleMode == SHUFFLE_NORMAL || mShuffleMode == SHUFFLE_AUTO) {
            setShuffleMode(SHUFFLE_NONE);
        }
    }

    private void gotoIdleState() {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        stopForeground(false);
        if (status != null) {
            status.contentView.setImageViewResource(R.id.status_bar_play,
                    mIsSupposedToBePlaying ? R.drawable.apollo_holo_dark_play
                            : R.drawable.apollo_holo_dark_pause);
            status.bigContentView.setImageViewResource(R.id.status_bar_play,
                    mIsSupposedToBePlaying ? R.drawable.apollo_holo_dark_play
                            : R.drawable.apollo_holo_dark_pause);
            NotificationManager mManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            mManager.notify(PLAYBACKSERVICE_STATUS, status);
        }
    }

    private void saveBookmarkIfNeeded() {
        try {
            if (isPodcast()) {
                long pos = position();
                long bookmark = getBookmark();
                long duration = duration();
                if ((pos < bookmark && (pos + 10000) > bookmark)
                        || (pos > bookmark && (pos - 10000) < bookmark)) {
                    // The existing bookmark is close to the current
                    // position, so don't update it.
                    return;
                }
                if (pos < 15000 || (pos + 10000) > duration) {
                    // if we're near the start or end, clear the bookmark
                    pos = 0;
                }

                // write 'pos' to the bookmark field
                ContentValues values = new ContentValues();
                values.put(AudioColumns.BOOKMARK, pos);
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mCursor.getLong(IDCOLIDX));
                getContentResolver().update(uri, values, null, null);
            }
        } catch (SQLiteException ex) {
        }
    }

    // Make sure there are at least 5 items after the currently playing item
    // and no more than 10 items before.
    private void doAutoShuffleUpdate() {
        boolean notify = false;

        // remove old entries
        if (mPlayPos > 10) {
            removeTracks(0, mPlayPos - 9);
            notify = true;
        }
        // add new entries if needed
        int to_add = 7 - (mPlayListLen - (mPlayPos < 0 ? -1 : mPlayPos));
        for (int i = 0; i < to_add; i++) {
            // pick something at random from the list

            int lookback = mHistory.size();
            int idx = -1;
            while (true) {
                idx = mRand.nextInt(mAutoShuffleList.length);
                if (!wasRecentlyUsed(idx, lookback)) {
                    break;
                }
                lookback /= 2;
            }
            mHistory.add(idx);
            if (mHistory.size() > MAX_HISTORY_SIZE) {
                mHistory.remove(0);
            }
            ensurePlayListCapacity(mPlayListLen + 1);
            mPlayList[mPlayListLen++] = mAutoShuffleList[idx];
            notify = true;
        }
        if (notify) {
            notifyChange(QUEUE_CHANGED);
        }
    }

    // check that the specified idx is not in the history (but only look at at
    // most lookbacksize entries in the history)
    private boolean wasRecentlyUsed(int idx, int lookbacksize) {

        // early exit to prevent infinite loops in case idx == mPlayPos
        if (lookbacksize == 0) {
            return false;
        }

        int histsize = mHistory.size();
        if (histsize < lookbacksize) {
            lookbacksize = histsize;
        }
        int maxidx = histsize - 1;
        for (int i = 0; i < lookbacksize; i++) {
            long entry = mHistory.get(maxidx - i);
            if (entry == idx) {
                return true;
            }
        }
        return false;
    }

    // A simple variation of Random that makes sure that the
    // value it returns is not equal to the value it returned
    // previously, unless the interval is 1.
    private static class Shuffler {
        private int mPrevious;

        private final Random mRandom = new Random();

        public int nextInt(int interval) {
            int ret;
            do {
                ret = mRandom.nextInt(interval);
            } while (ret == mPrevious && interval > 1);
            mPrevious = ret;
            return ret;
        }
    };

    private boolean makeAutoShuffleList() {
        ContentResolver res = getContentResolver();
        Cursor c = null;
        try {
            c = res.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[] {
                BaseColumns._ID
            }, AudioColumns.IS_MUSIC + "=1", null, null);
            if (c == null || c.getCount() == 0) {
                return false;
            }
            int len = c.getCount();
            long[] list = new long[len];
            for (int i = 0; i < len; i++) {
                c.moveToNext();
                list[i] = c.getLong(0);
            }
            mAutoShuffleList = list;
            return true;
        } catch (RuntimeException ex) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return false;
    }

    /**
     * Removes the range of tracks specified from the play list. If a file
     * within the range is the file currently being played, playback will move
     * to the next file after the range.
     * 
     * @param first The first file to be removed
     * @param last The last file to be removed
     * @return the number of tracks deleted
     */
    public int removeTracks(int first, int last) {
        int numremoved = removeTracksInternal(first, last);
        if (numremoved > 0) {
            notifyChange(QUEUE_CHANGED);
        }
        return numremoved;
    }

    private int removeTracksInternal(int first, int last) {
        synchronized (this) {
            if (last < first)
                return 0;
            if (first < 0)
                first = 0;
            if (last >= mPlayListLen)
                last = mPlayListLen - 1;

            boolean gotonext = false;
            if (first <= mPlayPos && mPlayPos <= last) {
                mPlayPos = first;
                gotonext = true;
            } else if (mPlayPos > last) {
                mPlayPos -= (last - first + 1);
            }
            int num = mPlayListLen - last - 1;
            for (int i = 0; i < num; i++) {
                mPlayList[first + i] = mPlayList[last + 1 + i];
            }
            mPlayListLen -= last - first + 1;

            if (gotonext) {
                if (mPlayListLen == 0) {
                    stop(true);
                    mPlayPos = -1;
                    if (mCursor != null) {
                        mCursor.close();
                        mCursor = null;
                    }
                } else {
                    if (mPlayPos >= mPlayListLen) {
                        mPlayPos = 0;
                    }
                    boolean wasPlaying = mIsSupposedToBePlaying;
                    stop(false);
                    openCurrentAndNext();
                    if (wasPlaying) {
                        play();
                    }
                }
                updateAlbumBitmap();
                notifyChange(META_CHANGED);
            }
            return last - first + 1;
        }
    }

    private synchronized void updateAlbumBitmap() {
    	if (mAlbumBitmapTask != null) {
    		mAlbumBitmapTask.cancel(true);
    		mAlbumBitmapTask = null;
    	}
    	
    	if (mCursor == null) {
    		return;
    	}

        ImageInfo mInfo = new ImageInfo();
        mInfo.type = TYPE_ALBUM;
        mInfo.size = SIZE_THUMB;
        mInfo.source = SRC_FIRST_AVAILABLE;
        mInfo.data = new String[]{ String.valueOf(getAlbumId()), getArtistName(), getAlbumName() };

        String tag = ImageUtils.createShortTag( mInfo ) + SIZE_THUMB ;
        if (tag == mAlbumBitmapTag)
            return;

        mAlbumBitmapTag = tag;
        mAlbumBitmap = null;
        

    	Resources resources = getResources();
    	DisplayMetrics metrics = resources.getDisplayMetrics();
    	int thumbSize = (int) ( ( 153 * (metrics.densityDpi/160f) ) + 0.5f );

        mAlbumBitmapTask = new GetBitmapTask( thumbSize, mInfo,  this,  this );
        mAlbumBitmapTask.execute();
    }

    @Override
    public void bitmapReady(Bitmap bitmap, String tag) {
        synchronized (this) {
            if (tag.equals(mAlbumBitmapTag)) {
                mAlbumBitmap = bitmap;
            }
        }
        notifyChange(META_CHANGED);
        if (status != null)
        	updateNotification();

        mAlbumBitmapTask = null;
    }

    /**
     * Removes all instances of the track with the given id from the playlist.
     * 
     * @param id The id to be removed
     * @return how many instances of the track were removed
     */
    public int removeTrack(long id) {
        int numremoved = 0;
        synchronized (this) {
            for (int i = 0; i < mPlayListLen; i++) {
                if (mPlayList[i] == id) {
                    numremoved += removeTracksInternal(i, i);
                    i--;
                }
            }
        }
        if (numremoved > 0) {
            notifyChange(QUEUE_CHANGED);
        }
        return numremoved;
    }

    public void setShuffleMode(int shufflemode) {
        synchronized (this) {
            if (mShuffleMode == shufflemode && mPlayListLen > 0) {
                return;
            }
            mShuffleMode = shufflemode;
            notifyChange(SHUFFLEMODE_CHANGED);
            if (mShuffleMode == SHUFFLE_AUTO) {
                if (makeAutoShuffleList()) {
                    mPlayListLen = 0;
                    doAutoShuffleUpdate();
                    mPlayPos = 0;
                    openCurrentAndNext();
                    play();
                    notifyChange(META_CHANGED);
                    return;
                } else {
                    // failed to build a list of files to shuffle
                    mShuffleMode = SHUFFLE_NONE;
                }
            }
            saveQueue(false);
        }
    }

    public int getShuffleMode() {
        return mShuffleMode;
    }

    public void setRepeatMode(int repeatmode) {
        synchronized (this) {
            mRepeatMode = repeatmode;
            setNextTrack();
            notifyChange(REPEATMODE_CHANGED);
            saveQueue(false);
        }
    }

    public int getRepeatMode() {
        return mRepeatMode;
    }

    public int getMediaMountedCount() {
        return mMediaMountedCount;
    }

    /**
     * Returns the path of the currently playing file, or null if no file is
     * currently playing.
     */
    public String getPath() {
        return mFileToPlay;
    }

    /**
     * Returns the rowid of the currently playing file, or -1 if no file is
     * currently playing.
     */
    public long getAudioId() {
        synchronized (this) {
            if (mPlayPos >= 0 && mPlayer.isInitialized()) {
                return mPlayList[mPlayPos];
            }
        }
        return -1;
    }

    /**
     * Returns the position in the queue
     * 
     * @return the position in the queue
     */
    public int getQueuePosition() {
        synchronized (this) {
            return mPlayPos;
        }
    }

    /**
     * Starts playing the track at the given position in the queue.
     * 
     * @param pos The position in the queue of the track that will be played.
     */
    public void setQueuePosition(int pos) {
        synchronized (this) {
            stop(false);
            mPlayPos = pos;
            openCurrentAndNext();
            play();
            notifyChange(META_CHANGED);
            if (mShuffleMode == SHUFFLE_AUTO) {
                doAutoShuffleUpdate();
            }
        }
    }

    public String getArtistName() {
        synchronized (this) {
            if (mCursor == null) {
                return getString(R.string.unknown);
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST));
        }
    }

    public long getArtistId() {
        synchronized (this) {
            if (mCursor == null) {
                return -1;
            }
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(AudioColumns.ARTIST_ID));
        }
    }

    public String getAlbumName() {
        synchronized (this) {
            if (mCursor == null) {
                return getString(R.string.unknown);
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(AudioColumns.ALBUM));
        }
    }

    public long getAlbumId() {
        synchronized (this) {
            if (mCursor == null) {
                return -1;
            }
            return mCursor.getLong(mCursor.getColumnIndexOrThrow(AudioColumns.ALBUM_ID));
        }
    }

    public Bitmap getAlbumBitmap() {
        return mAlbumBitmap;
    }

    public String getTrackName() {
        synchronized (this) {
            if (mCursor == null) {
                return getString(R.string.unknown);
            }
            return mCursor.getString(mCursor.getColumnIndexOrThrow(MediaColumns.TITLE));
        }
    }

    private boolean isPodcast() {
        synchronized (this) {
            if (mCursor == null) {
                return false;
            }
            return (mCursor.getInt(PODCASTCOLIDX) > 0);
        }
    }

    private long getBookmark() {
        synchronized (this) {
            if (mCursor == null) {
                return 0;
            }
            return mCursor.getLong(BOOKMARKCOLIDX);
        }
    }

    /**
     * Returns the duration of the file in milliseconds. Currently this method
     * returns -1 for the duration of MIDI files.
     */
    public long duration() {
        if (mPlayer.isInitialized()) {
            return mPlayer.duration();
        }
        return -1;
    }

    /**
     * Returns the current playback position in milliseconds
     */
    public long position() {
        if (mPlayer.isInitialized()) {
            return mPlayer.position();
        }
        return -1;
    }

    /**
     * Seeks to the position specified.
     * 
     * @param pos The position to seek to, in milliseconds
     */
    public long seek(long pos) {
        if (mPlayer.isInitialized()) {
            if (pos < 0)
                pos = 0;
            if (pos > mPlayer.duration())
                pos = mPlayer.duration();
            return mPlayer.seek(pos);
        }
        return -1;
    }

    /**
     * Sets the audio session ID.
     * 
     * @param sessionId: the audio session ID.
     */
    public void setAudioSessionId(int sessionId) {
        synchronized (this) {
            mPlayer.setAudioSessionId(sessionId);
        }
    }

    /**
     * Returns the audio session ID.
     */
    public int getAudioSessionId() {
        synchronized (this) {
            return mPlayer.getAudioSessionId();
        }
    }

    public void toggleFavorite() {
        if (!isFavorite()) {
            addToFavorites();
        } else {
            removeFromFavorites();
        }
    }

    public boolean isFavorite() {
        if (getAudioId() >= 0)
            return isFavorite(getAudioId());
        return false;
    }

    public boolean isFavorite(long id) {
        return MusicUtils.isFavorite(this, id);
    }

    public void removeFromFavorites() {
        if (getAudioId() >= 0) {
            removeFromFavorites(getAudioId());
        }
    }

    public void removeFromFavorites(long id) {
        MusicUtils.removeFromFavorites(this, id);
        notifyChange(FAVORITE_CHANGED);
    }

    public void addToFavorites() {
        if (getAudioId() >= 0) {
            addToFavorites(getAudioId());
        }
    }

    public void addToFavorites(long id) {
        MusicUtils.addToFavorites(this, id);
        notifyChange(FAVORITE_CHANGED);
    }

    /**
     * Provides a unified interface for dealing with midi files and other media
     * files.
     */
    private class MultiPlayer {
        private MediaPlayer mCurrentMediaPlayer = new MediaPlayer();
        
        private MediaPlayer mNextMediaPlayer;

        private Handler mHandler;

        private boolean mIsInitialized = false;

        public MultiPlayer() {
            mCurrentMediaPlayer.setWakeMode(ApolloService.this, PowerManager.PARTIAL_WAKE_LOCK);
        }

        public void setDataSource(String path) {
            mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
            if (mIsInitialized) {
                setNextDataSource(null);
            }
        }

        private boolean setDataSourceImpl(MediaPlayer player, String path) {
            try {
                player.reset();
                player.setOnPreparedListener(null);
                if (path.startsWith("content://")) {
                    player.setDataSource(ApolloService.this, Uri.parse(path));
                } else {
                    player.setDataSource(path);
                }
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.prepare();
            } catch (IOException ex) {
                return false;
            } catch (IllegalArgumentException ex) {
                return false;
            }
            player.setOnCompletionListener(listener);
            player.setOnErrorListener(errorListener);
            Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
            i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            sendBroadcast(i);

            VisualizerUtils.initVisualizer( player );
            return true;
        }

        public void setNextDataSource(String path) {
            mCurrentMediaPlayer.setNextMediaPlayer(null);
            if (mNextMediaPlayer != null) {
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
            }
            if (path == null) {
                return;
            }
            mNextMediaPlayer = new MediaPlayer();
            mNextMediaPlayer.setWakeMode(ApolloService.this, PowerManager.PARTIAL_WAKE_LOCK);
            mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
            if (setDataSourceImpl(mNextMediaPlayer, path)) {
                mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
            } else {
                // failed to open next, we'll transition the old fashioned way,
                // which will skip over the faulty file
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
            }
        }

        public boolean isInitialized() {
            return mIsInitialized;
        }

        public void start() {
            mCurrentMediaPlayer.start();
        }

        public void stop() {
            mCurrentMediaPlayer.reset();
            mIsInitialized = false;
        }

        /**
         * You CANNOT use this player anymore after calling release()
         */
        public void release() {
            stop();
            mCurrentMediaPlayer.release();
            VisualizerUtils.releaseVisualizer();
        }

        public void pause() {
            mCurrentMediaPlayer.pause();
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
                    mCurrentMediaPlayer.release();
                    mCurrentMediaPlayer = mNextMediaPlayer;
                    mNextMediaPlayer = null;
                    mHandler.sendEmptyMessage(TRACK_WENT_TO_NEXT);
                } else {
                    // Acquire a temporary wakelock, since when we return from
                    // this callback the MediaPlayer will release its wakelock
                    // and allow the device to go to sleep.
                    // This temporary wakelock is released when the RELEASE_WAKELOCK
                    // message is processed, but just in case, put a timeout on it.
                    mWakeLock.acquire(30000);
                    mHandler.sendEmptyMessage(TRACK_ENDED);
                    mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
                }

            }
        };

        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        mIsInitialized = false;
                        mCurrentMediaPlayer.release();
                        // Creating a new MediaPlayer and settings its wakemode
                        // does not
                        // require the media service, so it's OK to do this now,
                        // while the
                        // service is still being restarted
                        mCurrentMediaPlayer = new MediaPlayer();
                        mCurrentMediaPlayer
                                .setWakeMode(ApolloService.this, PowerManager.PARTIAL_WAKE_LOCK);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(SERVER_DIED), 2000);
                        return true;
                    default:
                        Log.d("MultiPlayer", "Error: " + what + "," + extra);
                        break;
                }
                return false;
            }
        };

        public long duration() {
            return mCurrentMediaPlayer.getDuration();
        }

        public long position() {
            return mCurrentMediaPlayer.getCurrentPosition();
        }

        public long seek(long whereto) {
            mCurrentMediaPlayer.seekTo((int)whereto);
            return whereto;
        }

        public void setVolume(float vol) {
            mCurrentMediaPlayer.setVolume(vol, vol);
        }

        public void setAudioSessionId(int sessionId) {
            mCurrentMediaPlayer.setAudioSessionId(sessionId);
        }

        public int getAudioSessionId() {
            return mCurrentMediaPlayer.getAudioSessionId();
        }
    }

    /*
     * By making this a static class with a WeakReference to the Service, we
     * ensure that the Service can be GCd even when the system process still has
     * a remote reference to the stub.
     */
    static class ServiceStub extends IApolloService.Stub {
        WeakReference<ApolloService> mService;

        ServiceStub(ApolloService service) {
            mService = new WeakReference<ApolloService>(service);
        }

        @Override
        public void openFile(String path) {
            mService.get().open(path);
        }

        @Override
        public void open(long[] list, int position) {
            mService.get().open(list, position);
        }

        @Override
        public long getIdFromPath(String path) {
            return mService.get().getIdFromPath(path);
        }

        @Override
        public int getQueuePosition() {
            return mService.get().getQueuePosition();
        }

        @Override
        public void setQueuePosition(int index) {
            mService.get().setQueuePosition(index);
        }

        @Override
        public boolean isPlaying() {
            return mService.get().isPlaying();
        }

        @Override
        public void stop() {
            mService.get().stop();
        }

        @Override
        public void pause() {
            mService.get().pause();
        }

        @Override
        public void play() {
            mService.get().play();
        }

        @Override
        public void prev() {
            mService.get().prev();
        }

        @Override
        public void next() {
            mService.get().gotoNext(true);
        }

        @Override
        public String getTrackName() {
            return mService.get().getTrackName();
        }

        @Override
        public String getAlbumName() {
            return mService.get().getAlbumName();
        }

        @Override
        public Bitmap getAlbumBitmap() {
            return mService.get().getAlbumBitmap();
        }

        @Override
        public long getAlbumId() {
            return mService.get().getAlbumId();
        }

        @Override
        public String getArtistName() {
            return mService.get().getArtistName();
        }

        @Override
        public long getArtistId() {
            return mService.get().getArtistId();
        }

        @Override
        public void enqueue(long[] list, int action) {
            mService.get().enqueue(list, action);
        }

        @Override
        public long[] getQueue() {
            return mService.get().getQueue();
        }

        @Override
        public String getPath() {
            return mService.get().getPath();
        }

        @Override
        public long getAudioId() {
            return mService.get().getAudioId();
        }

        @Override
        public long position() {
            return mService.get().position();
        }

        @Override
        public long duration() {
            return mService.get().duration();
        }

        @Override
        public long seek(long pos) {
            return mService.get().seek(pos);
        }

        @Override
        public void setShuffleMode(int shufflemode) {
            mService.get().setShuffleMode(shufflemode);
        }

        @Override
        public int getShuffleMode() {
            return mService.get().getShuffleMode();
        }

        @Override
        public int removeTracks(int first, int last) {
            return mService.get().removeTracks(first, last);
        }

        @Override
        public int removeTrack(long id) {
            return mService.get().removeTrack(id);
        }

        @Override
        public void setRepeatMode(int repeatmode) {
            mService.get().setRepeatMode(repeatmode);
        }

        @Override
        public int getRepeatMode() {
            return mService.get().getRepeatMode();
        }

        @Override
        public int getMediaMountedCount() {
            return mService.get().getMediaMountedCount();
        }

        @Override
        public int getAudioSessionId() {
            return mService.get().getAudioSessionId();
        }

        @Override
        public void addToFavorites(long id) throws RemoteException {
            mService.get().addToFavorites(id);
        }

        @Override
        public void removeFromFavorites(long id) throws RemoteException {
            mService.get().removeFromFavorites(id);
        }

        @Override
        public boolean isFavorite(long id) throws RemoteException {
            return mService.get().isFavorite(id);
        }

        @Override
        public void toggleFavorite() throws RemoteException {
            mService.get().toggleFavorite();
        }
        
        public void notifyChange(String what){
        	mService.get().notifyChange(what);
        }

    }

    private final IBinder mBinder = new ServiceStub(this);

}
