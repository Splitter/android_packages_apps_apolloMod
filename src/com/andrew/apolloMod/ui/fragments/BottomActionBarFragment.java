
package com.andrew.apolloMod.ui.fragments;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.ui.fragments.list.NowPlayingFragment;
import com.andrew.apolloMod.ui.widgets.BottomActionBar;

public class BottomActionBarFragment extends Fragment {

	private ImageButton mPrev, mPlay, mNext, mQueue, mFavs;
	private ImageView mAlbumImage;
    private BottomActionBar mBottomActionBar;
    private RelativeLayout albumArt, listQueue;
    private TextView mSongName, mSongNameOnly, mArtistName;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View root = inflater.inflate(R.layout.bottom_action_bar, container);
        mBottomActionBar = new BottomActionBar(getActivity());
        mAlbumImage = (ImageView)root.findViewById(R.id.bottom_action_bar_album_art);

        mSongName = (TextView)root.findViewById(R.id.bottom_action_bar_track_name);
        mSongNameOnly = (TextView)root.findViewById(R.id.bottom_action_bar_track_name_only);
        mArtistName = (TextView)root.findViewById(R.id.bottom_action_bar_artist_name);

        mQueue = (ImageButton)root.findViewById(R.id.bottom_action_bar_switch_queue);

        mFavs = (ImageButton)root.findViewById(R.id.bottom_action_bar_favorites);
        mFavs.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	MusicUtils.toggleFavorite();
            	if(MusicUtils.isFavorite(getActivity(), MusicUtils.getCurrentAudioId())){
            		mFavs.setImageResource(R.drawable.apollo_holo_light_favorite_selected);
            	}
            	else{
                	Theme theme = getActivity().getTheme();
            		TypedValue typedvalueattr = new TypedValue();
            		theme.resolveAttribute(R.attr.AudioFavoritesButton, typedvalueattr, true); 
            		mFavs.setImageResource(typedvalueattr.resourceId);
            	}
            }
        });
        
        
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
    	Theme theme = getActivity().getTheme();
		TypedValue typedvalueattr = new TypedValue();
		theme.resolveAttribute(R.attr.AudioQueueButton, typedvalueattr, true); 
    	mQueue.setImageResource( typedvalueattr.resourceId );
    	mQueue.setVisibility(View.GONE);
    	mFavs.setVisibility(View.GONE);        
        listQueue.setVisibility(View.GONE);
        
    	mPrev.setVisibility(View.VISIBLE);
    	mNext.setVisibility(View.VISIBLE);
    	mPlay.setVisibility(View.VISIBLE);
    	mAlbumImage.setVisibility(View.VISIBLE);
        albumArt.setVisibility(View.VISIBLE);
        
        mSongName.setVisibility(View.VISIBLE);
        mArtistName.setVisibility(View.VISIBLE);
        mSongNameOnly.setVisibility(View.GONE);

        fade(listQueue, 0f);
        fade(albumArt, 1f);
    }
    
    public void onExpanded(){
    	mPrev.setVisibility(View.GONE);
    	mNext.setVisibility(View.GONE);
    	mPlay.setVisibility(View.GONE);
    	
    	mAlbumImage.setVisibility(View.GONE);
    	mQueue.setVisibility(View.VISIBLE);
    	mFavs.setVisibility(View.VISIBLE);
    	
        mSongName.setVisibility(View.GONE);
        mArtistName.setVisibility(View.GONE);
        mSongNameOnly.setVisibility(View.VISIBLE);
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
        	Theme theme = getActivity().getTheme();
			TypedValue typedvalueattr = new TypedValue();
            if (MusicUtils.mService != null && MusicUtils.mService.isPlaying()) {
    			theme.resolveAttribute(R.attr.AudioPauseButton, typedvalueattr, true); 
            } else {
    			theme.resolveAttribute(R.attr.AudioPlayButton, typedvalueattr, true); 
            }
            mPlay.setImageResource(typedvalueattr.resourceId);
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
                	Theme theme = getActivity().getTheme();
            		TypedValue typedvalueattr = new TypedValue();
            		theme.resolveAttribute(R.attr.AudioQueueButton, typedvalueattr, true); 
            		mQueue.setImageResource(typedvalueattr.resourceId);
                    // Fade out the pager container
                    fade(listQueue, 0f);
                    // Fade in the album art
                    fade(albumArt, 1f);
            	}
            	
            }
        });
		
	}
}
