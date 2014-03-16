
package com.andrew.apolloMod.ui.fragments;

import static com.andrew.apolloMod.Constants.SIZE_NORMAL;
import static com.andrew.apolloMod.Constants.SRC_FIRST_AVAILABLE;
import static com.andrew.apolloMod.Constants.TYPE_ALBUM;
import java.lang.ref.WeakReference;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.cache.ImageInfo;
import com.andrew.apolloMod.cache.ImageProvider;
import com.andrew.apolloMod.helpers.utils.ApolloUtils;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.helpers.utils.VisualizerUtils;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.ui.adapters.AlbumArtPagerAdapter;
import com.andrew.apolloMod.ui.widgets.RepeatingImageButton;
import com.andrew.apolloMod.ui.widgets.VisualizerView;

public class AudioPlayerFragment extends Fragment {

    // Total and current time
    private TextView mTotalTime, mCurrentTime;

    // Album art
    private ViewPager mAlbumArtPager;

    private AlbumArtPagerAdapter mPagerAdapter;
    // Controls
    private ImageButton mRepeat, mPlay, mShuffle;

    private RepeatingImageButton mPrev, mNext;

    // Progress
    private SeekBar mProgress;

    // Where we are in the track
    private long mDuration, mLastSeekEventTime, mPosOverride = -1, mStartSeekPos = 0;

    private boolean mFromTouch, paused = false;

    // Handler
    private static final int REFRESH = 1, UPDATEINFO = 2;
    
    View root = null;

    // Notify if repeat or shuffle changes
    private Toast mToast;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.audio_player, container, false);
        
        mTotalTime = (TextView)root.findViewById(R.id.audio_player_total_time);
        mCurrentTime = (TextView)root.findViewById(R.id.audio_player_current_time);

        mRepeat = (ImageButton)root.findViewById(R.id.audio_player_repeat);
        mPrev = (RepeatingImageButton)root.findViewById(R.id.audio_player_prev);
        mPlay = (ImageButton)root.findViewById(R.id.audio_player_play);
        mNext = (RepeatingImageButton)root.findViewById(R.id.audio_player_next);
        mShuffle = (ImageButton)root.findViewById(R.id.audio_player_shuffle);

        mRepeat.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                cycleRepeat();
            }
        });

        mPrev.setRepeatListener(mRewListener, 260);
        mPrev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MusicUtils.mService == null)
                    return;
                try {
                    if (MusicUtils.mService.position() < 2000) {
                        MusicUtils.mService.prev();
                    } else {
                        MusicUtils.mService.seek(0);
                        MusicUtils.mService.play();
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        mPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doPauseResume();
            }
        });
        
        mNext.setRepeatListener(mFfwdListener, 260);
        mNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MusicUtils.mService == null)
                    return;
                try {
                    MusicUtils.mService.next();
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        mShuffle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleShuffle();
            }
        });
        
        mProgress = (SeekBar)root.findViewById(android.R.id.progress);
        if (mProgress instanceof SeekBar) {
            SeekBar seeker = mProgress;
            seeker.setOnSeekBarChangeListener(mSeekListener);
        }
        mProgress.setMax(1000);
        
        mAlbumArtPager = (ViewPager)root.findViewById(R.id.now_playing_viewpager);
        mPagerAdapter = new AlbumArtPagerAdapter(getChildFragmentManager());
        mPagerAdapter.addFragment(new AlbumArtFragment());
        mPagerAdapter.addFragment(new AlbumArtFragment());
        mPagerAdapter.addFragment(new AlbumArtFragment());
        mAlbumArtPager.setAdapter(mPagerAdapter);
        mAlbumArtPager.setOffscreenPageLimit(3);
        mAlbumArtPager.setCurrentItem(1);
        mAlbumArtPager.setOnPageChangeListener(new AlbumArtPageListener());
        
        FrameLayout mColorstripBottom = (FrameLayout)root.findViewById(R.id.colorstrip_bottom);
        mColorstripBottom.setBackgroundColor(getResources().getColor(R.color.holo_blue_dark));
        return root;
    }

    private class AlbumArtPageListener extends SimpleOnPageChangeListener{
        public void onPageScrollStateChanged(int state) {   
        	if(state == ViewPager.SCROLL_STATE_IDLE){
        		int cur = mAlbumArtPager.getCurrentItem();
	        	if(cur == 0){
	        		mPagerAdapter.addFragmentTo(new AlbumArtFragment(), 0);
	        		mPagerAdapter.removeItem(3);
	                mAlbumArtPager.setAdapter(mPagerAdapter);
	                mAlbumArtPager.setCurrentItem(1);
	                if (MusicUtils.mService == null)
	                    return;
	                try {
	                    MusicUtils.mService.prev();
	                } catch (RemoteException ex) {
	                    ex.printStackTrace();
	                }
	    		}
	        	else if ( cur == 2 ){
	        		mPagerAdapter.addFragmentTo(new AlbumArtFragment(), 3);
	        		mPagerAdapter.removeItem(0);
	                mAlbumArtPager.setAdapter(mPagerAdapter);
	                mAlbumArtPager.setCurrentItem(1);
	                if (MusicUtils.mService == null)
	                    return;
	                try {
	                    MusicUtils.mService.next();
	                } catch (RemoteException ex) {
	                    ex.printStackTrace();
	                }
	    		}
        	}
	    }
	}
    /**
     * Update everything as the meta or playstate changes
     */
    private final BroadcastReceiver mStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ApolloService.META_CHANGED))
                mHandler.sendMessage(mHandler.obtainMessage(UPDATEINFO));
            setPauseButtonImage();
            setShuffleButtonImage();
            setRepeatButtonImage();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter();
        f.addAction(ApolloService.PLAYSTATE_CHANGED);
        f.addAction(ApolloService.META_CHANGED);
        getActivity().registerReceiver(mStatusListener, new IntentFilter(f));

        long next = refreshNow();
        queueNextRefresh(next);
        
        WeakReference<VisualizerView> mView = new WeakReference<VisualizerView>((VisualizerView)root.findViewById(R.id.visualizerView));
        VisualizerUtils.updateVisualizerView(mView);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        paused = true;
        mHandler.removeMessages(REFRESH);
        getActivity().unregisterReceiver(mStatusListener);
    }

    /**
     * Cycle repeat states
     */
    private void cycleRepeat() {
        if (MusicUtils.mService == null) {
            return;
        }
        try {
            int mode = MusicUtils.mService.getRepeatMode();
            if (mode == ApolloService.REPEAT_NONE) {
                MusicUtils.mService.setRepeatMode(ApolloService.REPEAT_ALL);
                ApolloUtils.showToast(R.string.repeat_all, mToast, getActivity());
            } else if (mode == ApolloService.REPEAT_ALL) {
                MusicUtils.mService.setRepeatMode(ApolloService.REPEAT_CURRENT);
                if (MusicUtils.mService.getShuffleMode() != ApolloService.SHUFFLE_NONE) {
                    MusicUtils.mService.setShuffleMode(ApolloService.SHUFFLE_NONE);
                    setShuffleButtonImage();
                }
                ApolloUtils.showToast(R.string.repeat_one, mToast, getActivity());
            } else {
                MusicUtils.mService.setRepeatMode(ApolloService.REPEAT_NONE);
                ApolloUtils.showToast(R.string.repeat_off, mToast, getActivity());
            }
            setRepeatButtonImage();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Scan backwards
     */
    private final RepeatingImageButton.RepeatListener mRewListener = new RepeatingImageButton.RepeatListener() {
        @Override
        public void onRepeat(View v, long howlong, int repcnt) {
            scanBackward(repcnt, howlong);
        }
    };

    /**
     * Play and pause music
     */
    private void doPauseResume() {
        try {
            if (MusicUtils.mService != null) {
                if (MusicUtils.mService.isPlaying()) {
                    MusicUtils.mService.pause();
                } else {
                    MusicUtils.mService.play();
                }
            }
            refreshNow();
            setPauseButtonImage();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Scan forwards
     */
    private final RepeatingImageButton.RepeatListener mFfwdListener = new RepeatingImageButton.RepeatListener() {
        @Override
        public void onRepeat(View v, long howlong, int repcnt) {
            scanForward(repcnt, howlong);
        }
    };

    /**
     * Set the shuffle mode
     */
    private void toggleShuffle() {
        if (MusicUtils.mService == null) {
            return;
        }
        try {
            int shuffle = MusicUtils.mService.getShuffleMode();
            if (shuffle == ApolloService.SHUFFLE_NONE) {
                MusicUtils.mService.setShuffleMode(ApolloService.SHUFFLE_NORMAL);
                if (MusicUtils.mService.getRepeatMode() == ApolloService.REPEAT_CURRENT) {
                    MusicUtils.mService.setRepeatMode(ApolloService.REPEAT_ALL);
                    setRepeatButtonImage();
                }
                ApolloUtils.showToast(R.string.shuffle_on, mToast, getActivity());
            } else if (shuffle == ApolloService.SHUFFLE_NORMAL
                    || shuffle == ApolloService.SHUFFLE_AUTO) {
                MusicUtils.mService.setShuffleMode(ApolloService.SHUFFLE_NONE);
                ApolloUtils.showToast(R.string.shuffle_off, mToast, getActivity());
            }
            setShuffleButtonImage();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    private void scanBackward(int repcnt, long delta) {
        if (MusicUtils.mService == null)
            return;
        try {
            if (repcnt == 0) {
                mStartSeekPos = MusicUtils.mService.position();
                mLastSeekEventTime = 0;
            } else {
                if (delta < 5000) {
                    // seek at 10x speed for the first 5 seconds
                    delta = delta * 10;
                } else {
                    // seek at 40x after that
                    delta = 50000 + (delta - 5000) * 40;
                }
                long newpos = mStartSeekPos - delta;
                if (newpos < 0) {
                    // move to previous track
                    MusicUtils.mService.prev();
                    long duration = MusicUtils.mService.duration();
                    mStartSeekPos += duration;
                    newpos += duration;
                }
                if (((delta - mLastSeekEventTime) > 250) || repcnt < 0) {
                    MusicUtils.mService.seek(newpos);
                    mLastSeekEventTime = delta;
                }
                if (repcnt >= 0) {
                    mPosOverride = newpos;
                } else {
                    mPosOverride = -1;
                }
                refreshNow();
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    private void scanForward(int repcnt, long delta) {
        if (MusicUtils.mService == null)
            return;
        try {
            if (repcnt == 0) {
                mStartSeekPos = MusicUtils.mService.position();
                mLastSeekEventTime = 0;
            } else {
                if (delta < 5000) {
                    // seek at 10x speed for the first 5 seconds
                    delta = delta * 10;
                } else {
                    // seek at 40x after that
                    delta = 50000 + (delta - 5000) * 40;
                }
                long newpos = mStartSeekPos + delta;
                long duration = MusicUtils.mService.duration();
                if (newpos >= duration) {
                    // move to next track
                    MusicUtils.mService.next();
                    mStartSeekPos -= duration; // is OK to go negative
                    newpos -= duration;
                }
                if (((delta - mLastSeekEventTime) > 250) || repcnt < 0) {
                    MusicUtils.mService.seek(newpos);
                    mLastSeekEventTime = delta;
                }
                if (repcnt >= 0) {
                    mPosOverride = newpos;
                } else {
                    mPosOverride = -1;
                }
                refreshNow();
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Set the repeat images
     */
    private void setRepeatButtonImage() {
        if (MusicUtils.mService == null)
            return;
        try {
            switch (MusicUtils.mService.getRepeatMode()) {
                case ApolloService.REPEAT_ALL:
                    mRepeat.setImageResource(R.drawable.apollo_holo_light_repeat_all);
                    break;
                case ApolloService.REPEAT_CURRENT:
                    mRepeat.setImageResource(R.drawable.apollo_holo_light_repeat_one);
                    break;
                default:
                    mRepeat.setImageResource(R.drawable.apollo_holo_light_repeat_normal);
                    break;
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Set the shuffle images
     */
    private void setShuffleButtonImage() {
        if (MusicUtils.mService == null)
            return;
        try {
            switch (MusicUtils.mService.getShuffleMode()) {
                case ApolloService.SHUFFLE_NONE:
                    mShuffle.setImageResource(R.drawable.apollo_holo_light_shuffle_normal);
                    break;
                case ApolloService.SHUFFLE_AUTO:
                    mShuffle.setImageResource(R.drawable.apollo_holo_light_shuffle_on);
                    break;
                default:
                    mShuffle.setImageResource(R.drawable.apollo_holo_light_shuffle_on);
                    break;
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Set the play and pause image
     */
    private void setPauseButtonImage() {
        try {
            if (MusicUtils.mService != null && MusicUtils.mService.isPlaying()) {
                mPlay.setImageResource(R.drawable.apollo_holo_light_pause);
            } else {
                mPlay.setImageResource(R.drawable.apollo_holo_light_play);
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param delay
     */
    private void queueNextRefresh(long delay) {
        if (!paused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    /**
     * We need to refresh the time via a Handler
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    break;
                case UPDATEINFO:
                    updateMusicInfo();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Drag to a specfic duration
     */
    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            mLastSeekEventTime = 0;
            mFromTouch = true;
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser || (MusicUtils.mService == null))
                return;
            long now = SystemClock.elapsedRealtime();
            if ((now - mLastSeekEventTime) > 250) {
                mLastSeekEventTime = now;
                mPosOverride = mDuration * progress / 1000;
                try {
                    MusicUtils.mService.seek(mPosOverride);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }

                if (!mFromTouch) {
                    refreshNow();
                    mPosOverride = -1;
                }
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            mPosOverride = -1;
            mFromTouch = false;
        }
    };

    /**
     * @return current time
     */
    private long refreshNow() {
        if (MusicUtils.mService == null)
            return 500;
        try {
            long pos = mPosOverride < 0 ? MusicUtils.mService.position() : mPosOverride;
            long remaining = 1000 - (pos % 1000);
            if ((pos >= 0) && (mDuration > 0)) {
                mCurrentTime.setText(MusicUtils.makeTimeString(getActivity(), pos / 1000));

                if (MusicUtils.mService.isPlaying()) {
                    mCurrentTime.setVisibility(View.VISIBLE);
                    mCurrentTime.setTextColor(getResources().getColor(R.color.transparent_black));
                } else {
                    // blink the counter
                    int col = mCurrentTime.getCurrentTextColor();
                    mCurrentTime.setTextColor(col == getResources().getColor(
                            R.color.transparent_black) ? getResources().getColor(
                            R.color.holo_blue_dark) : getResources().getColor(
                            R.color.transparent_black));
                    remaining = 500;
                }

                mProgress.setProgress((int)(1000 * pos / mDuration));
            } else {
                mCurrentTime.setText("--:--");
                mProgress.setProgress(1000);
            }
            return remaining;
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        return 500;
    }

    /**
     * Update what's playing
     */
    private void updateMusicInfo() {
        if (MusicUtils.mService == null) {
            return;
        }

        String artistName = MusicUtils.getArtistName();
        String albumName = MusicUtils.getAlbumName();
        String albumId = String.valueOf(MusicUtils.getCurrentAlbumId());
        mDuration = MusicUtils.getDuration();
        mTotalTime.setText(MusicUtils.makeTimeString(getActivity(), mDuration / 1000));

        ImageInfo mInfo = new ImageInfo();
        mInfo.type = TYPE_ALBUM;
        mInfo.size = SIZE_NORMAL;
        mInfo.source = SRC_FIRST_AVAILABLE;
        mInfo.data = new String[]{ albumId , artistName, albumName };

        AlbumArtFragment cur =(AlbumArtFragment) mPagerAdapter.getItem(mAlbumArtPager.getCurrentItem());
        ImageProvider.getInstance( getActivity() ).loadImage( cur.albumArt, mInfo );
    }

}
