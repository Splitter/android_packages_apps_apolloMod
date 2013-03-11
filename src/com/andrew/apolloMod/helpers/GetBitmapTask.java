package com.andrew.apolloMod.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.cache.ImageInfo;
import com.andrew.apolloMod.helpers.utils.ImageUtils;

import static com.andrew.apolloMod.Constants.*;

import java.io.File;
import java.lang.ref.WeakReference;

public class GetBitmapTask extends AsyncTask<String, Integer, Bitmap> {

    
    private WeakReference<OnBitmapReadyListener> mListenerReference;

    private WeakReference<Context> mContextReference;

    private ImageInfo mImageInfo;
    
    private int mThumbSize;

    public GetBitmapTask( int thumbSize, ImageInfo imageInfo, OnBitmapReadyListener listener, Context context ) {
        mListenerReference = new WeakReference<OnBitmapReadyListener>(listener);
        mContextReference = new WeakReference<Context>(context);
        mImageInfo = imageInfo;
    	mThumbSize = thumbSize;
    }

    @Override
    protected Bitmap doInBackground(String... ignored) {
        Context context = mContextReference.get();
        if (context == null) {
            return null;
        }
        //Get bitmap from proper source
        File nFile = null;
        
        if( mImageInfo.source.equals(SRC_FILE)  && !isCancelled()){
        	nFile = ImageUtils.getImageFromMediaStore( context, mImageInfo );
        }
        else if ( mImageInfo.source.equals(SRC_LASTFM)  && !isCancelled()){
        	nFile = ImageUtils.getImageFromWeb( context, mImageInfo );
        }
        else if ( mImageInfo.source.equals(SRC_GALLERY)  && !isCancelled()){
        	nFile = ImageUtils.getImageFromGallery( context, mImageInfo );
        }        	
        else if ( mImageInfo.source.equals(SRC_FIRST_AVAILABLE)  && !isCancelled()){
        	Bitmap bitmap = null;
        	if( mImageInfo.size.equals( SIZE_NORMAL ) ){
        		bitmap = ImageUtils.getNormalImageFromDisk( context, mImageInfo );
        	}
        	else if( mImageInfo.size.equals( SIZE_THUMB ) ){
        		bitmap = ImageUtils.getThumbImageFromDisk( context, mImageInfo, mThumbSize );
        	}
        	//if we have a bitmap here then its already properly sized
        	if( bitmap != null ){
        		return bitmap;
        	}
        	
        	if( mImageInfo.type.equals( TYPE_ALBUM ) ){
        		nFile = ImageUtils.getImageFromMediaStore( context, mImageInfo );
        	}
        	if( nFile == null && ( mImageInfo.type.equals( TYPE_ALBUM ) || mImageInfo.type.equals( TYPE_ARTIST ) ) )
        		nFile = ImageUtils.getImageFromWeb( context, mImageInfo );
        }
        if( nFile != null ){        	
        	// if requested size is normal return it
        	if( mImageInfo.size.equals( SIZE_NORMAL ) )
        		return BitmapFactory.decodeFile(nFile.getAbsolutePath());
        	//if it makes it here we want a thumbnail image
        	return ImageUtils.getThumbImageFromDisk( context, nFile, mThumbSize );
        }
        return null;
    }
    
    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        OnBitmapReadyListener listener = mListenerReference.get();
        if(bitmap == null && !isCancelled()){
        	if(mImageInfo.size.equals(SIZE_THUMB))
        		bitmap = BitmapFactory.decodeResource(mContextReference.get().getResources(),
        													R.drawable.no_art_small);
        	else if(mImageInfo.size.equals(SIZE_NORMAL))
        		bitmap = BitmapFactory.decodeResource(mContextReference.get().getResources(),
        													R.drawable.no_art_normal);
        }
        if (bitmap != null && !isCancelled()) {
            if (listener != null) {
                	listener.bitmapReady(bitmap,  ImageUtils.createShortTag(mImageInfo) + mImageInfo.size );
            }
        }
    }

    public static interface OnBitmapReadyListener {
        public void bitmapReady(Bitmap bitmap, String tag);
    }
}
