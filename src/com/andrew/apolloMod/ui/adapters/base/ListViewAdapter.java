package com.andrew.apolloMod.ui.adapters.base;

import static com.andrew.apolloMod.Constants.SIZE_THUMB;
import static com.andrew.apolloMod.Constants.SRC_FIRST_AVAILABLE;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.cache.ImageInfo;
import com.andrew.apolloMod.cache.ImageProvider;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.views.ViewHolderList;

public abstract class ListViewAdapter extends SimpleCursorAdapter {
	
    private WeakReference<ViewHolderList> holderReference;
    
    private AnimationDrawable mPeakOneAnimation, mPeakTwoAnimation;
    
    protected Context mContext;
    
    private int left, top;    
    
    public String mListType = null,  mLineOneText = null, mLineTwoText = null;
    
    public String[] mImageData = null;
    
    public long mPlayingId = 0, mCurrentId = 0;
    
    public boolean showContextEnabled = true;
    
    private ImageProvider mImageProvider;

    /**
     * Used to quickly show our the ContextMenu
     */
    private final View.OnClickListener showContextMenu = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.showContextMenu();
        }
    };
    
    public ListViewAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mContext = context;
        // Helps center the text in the Playlist/Genre tab
        left = mContext.getResources().getDimensionPixelSize(
                R.dimen.listview_items_padding_left_top);
        top = mContext.getResources().getDimensionPixelSize(
                R.dimen.listview_items_padding_gp_top);
        
    	mImageProvider = ImageProvider.getInstance( (Activity) mContext );
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);

        Cursor mCursor = (Cursor) getItem(position);
        setupViewData(mCursor);

        final ViewHolderList viewholder;
        if ( view != null ) {
            viewholder = new ViewHolderList(view);
            holderReference = new WeakReference<ViewHolderList>(viewholder);
            view.setTag(holderReference.get());
        } else {
            viewholder = (ViewHolderList)convertView.getTag();
        }

        if( mLineOneText != null ){
            holderReference.get().mViewHolderLineOne.setText(mLineOneText);
        }
        else{
        	holderReference.get().mViewHolderLineOne.setVisibility(View.GONE);
        }

        if( mLineTwoText != null ){
            holderReference.get().mViewHolderLineTwo.setText(mLineTwoText);
        }
        else{
            holderReference.get().mViewHolderLineOne.setPadding(left, top, 0, 0);
            holderReference.get().mViewHolderLineTwo.setVisibility(View.GONE);
        }
        
        if( mImageData != null ){

            ImageInfo mInfo = new ImageInfo();
            mInfo.type = mListType;
            mInfo.size = SIZE_THUMB;
            mInfo.source = SRC_FIRST_AVAILABLE;
            mInfo.data = mImageData;
            
            mImageProvider.loadImage( viewholder.mViewHolderImage, mInfo ); 
        }
        else{
            holderReference.get().mViewHolderImage.setVisibility(View.GONE);
        }
        
        if( showContextEnabled ){
            holderReference.get().mQuickContext.setOnClickListener(showContextMenu);        	
        }
        else{
        	 holderReference.get().mQuickContext.setVisibility(View.GONE);
        }

        if ( ( mPlayingId !=0 && mCurrentId !=0 ) && mPlayingId ==  mCurrentId ) {
            holderReference.get().mPeakOne.setImageResource(R.anim.peak_meter_1);
            holderReference.get().mPeakTwo.setImageResource(R.anim.peak_meter_2);
            mPeakOneAnimation = (AnimationDrawable)holderReference.get().mPeakOne.getDrawable();
            mPeakTwoAnimation = (AnimationDrawable)holderReference.get().mPeakTwo.getDrawable();
            try {
                if ( MusicUtils.mService.isPlaying() ) {
                    mPeakOneAnimation.start();
                    mPeakTwoAnimation.start();
                } else {
                    mPeakOneAnimation.stop();
                    mPeakTwoAnimation.stop();
                }
            } catch ( RemoteException e ) {
                e.printStackTrace();
            }
        } else {
            holderReference.get().mPeakOne.setImageResource(0);
            holderReference.get().mPeakTwo.setImageResource(0);
        }
        return view;
    }
    
    public abstract void setupViewData( Cursor mCursor ); 
    
}
