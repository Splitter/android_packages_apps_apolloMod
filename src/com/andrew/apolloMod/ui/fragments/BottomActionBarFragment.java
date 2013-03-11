/**
 * 
 */

package com.andrew.apolloMod.ui.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.helpers.utils.ThemeUtils;
import com.andrew.apolloMod.service.ApolloService;
import com.andrew.apolloMod.ui.widgets.BottomActionBar;

/**
 * @author Andrew Neal
 */
public class BottomActionBarFragment extends Fragment {

	private ImageButton mPrev, mPlay, mNext;
    private BottomActionBar mBottomActionBar;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View root = inflater.inflate(R.layout.bottom_action_bar, container);
        mBottomActionBar = new BottomActionBar(getActivity());
        
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

        ThemeUtils.setImageButton(getActivity(), mPrev, "apollo_previous");
        ThemeUtils.setImageButton(getActivity(), mNext, "apollo_next");
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

    /**
     * Set the play and pause image
     */
    private void setPauseButtonImage() {
        try {
            if (MusicUtils.mService != null && MusicUtils.mService.isPlaying()) {
                mPlay.setImageResource(R.drawable.apollo_holo_light_pause);
                // Theme chooser
                ThemeUtils.setImageButton(getActivity(), mPlay, "apollo_pause");
            } else {
                mPlay.setImageResource(R.drawable.apollo_holo_light_play);
                // Theme chooser
                ThemeUtils.setImageButton(getActivity(), mPlay, "apollo_play");
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }
}
