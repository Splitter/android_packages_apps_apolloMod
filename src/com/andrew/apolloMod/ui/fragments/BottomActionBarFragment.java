
package com.andrew.apolloMod.ui.fragments;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.ui.fragments.list.NowPlayingFragment;
import com.andrew.apolloMod.ui.widgets.BottomActionBar;

public class BottomActionBarFragment extends Fragment {

	private ImageButton mPrev, mPlay, mNext, mQueue;
    private BottomActionBar mBottomActionBar;
    private RelativeLayout albumArt, listQueue;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View root = inflater.inflate(R.layout.bottom_action_bar, container);
        mBottomActionBar = new BottomActionBar(getActivity());
        
        
        mQueue = (ImageButton)root.findViewById(R.id.bottom_action_bar_switch_queue);
        
        
        
        mPrev = (ImageButton)root.findViewById(R.id.bottom_action_bar_previous);
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

        mPlay = (ImageButton)root.findViewById(R.id.bottom_action_bar_play);
        mPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                doPauseResume();
            }
        });

        mNext = (ImageButton)root.findViewById(R.id.bottom_action_bar_next);
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
        return root;
    }

    /**
     * Update the list as needed
     */
    private final BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mBottomActionBar != null) {
                mBottomActionBar.updateBottomActionBar(getActivity());
            }
            setPauseButtonImage();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ApolloService.PLAYSTATE_CHANGED);
        filter.addAction(ApolloService.META_CHANGED);
        getActivity().registerReceiver(mMediaStatusReceiver, filter);
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mMediaStatusReceiver);
        super.onStop();
    }

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
            setPauseButtonImage();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    public void onCollapsed(){
    	mPrev.setVisibility(View.VISIBLE);
    	mNext.setVisibility(View.VISIBLE);
    	mPlay.setVisibility(View.VISIBLE);
    	
    	mQueue.setImageResource(R.drawable.btn_switch_queue);
    	mQueue.setVisibility(View.GONE);
        
        listQueue.setVisibility(View.GONE);
        albumArt.setVisibility(View.VISIBLE);

        fade(listQueue, 0f);
        // Fade in the album art
        fade(albumArt, 1f);
    }
    
    public void onExpanded(){
    	mPrev.setVisibility(View.GONE);
    	mNext.setVisibility(View.GONE);
    	mPlay.setVisibility(View.GONE);
    	mQueue.setVisibility(View.VISIBLE);
    }
    
    /**
     * @param v The view to animate
     * @param alpha The alpha to apply
     */
    private void fade(final View v, final float alpha) {
        final ObjectAnimator fade = ObjectAnimator.ofFloat(v, "alpha", alpha);
        fade.setInterpolator(AnimationUtils.loadInterpolator(getActivity(),
                android.R.anim.accelerate_decelerate_interpolator));
        fade.setDuration(400);
        fade.start();
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

	public void setUpQueueSwitch(Activity activity) {
		// TODO Auto-generated method stub
		albumArt = (RelativeLayout) activity.findViewById(R.id.audio_player_album_art_wrapper);
        listQueue = (RelativeLayout) activity.findViewById(R.id.audio_player_queue_wrapper);
        mQueue.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	if(albumArt.getVisibility()==View.VISIBLE){
            		listQueue.removeAllViews();
            		getFragmentManager().beginTransaction().add(R.id.audio_player_queue_wrapper, new NowPlayingFragment(), "NowPlayingTag").commit();
            		mQueue.setImageResource(R.drawable.btn_switch_queue_active);
                    albumArt.setVisibility(View.GONE);
                    listQueue.setVisibility(View.VISIBLE);
                    // Fade out the pager container
                    fade(albumArt, 0f);
                    // Fade in the album art
                    fade(listQueue, 1f);
            	}
            	else{
                    listQueue.setVisibility(View.GONE);
                    albumArt.setVisibility(View.VISIBLE);
            		mQueue.setImageResource(R.drawable.btn_switch_queue);
                    // Fade out the pager container
                    fade(listQueue, 0f);
                    // Fade in the album art
                    fade(albumArt, 1f);
            	}
            	
            }
        });
		
	}
}
