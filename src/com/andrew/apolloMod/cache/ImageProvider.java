package com.andrew.apolloMod.cache;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.GetBitmapTask;
import com.andrew.apolloMod.helpers.utils.ImageUtils;

import static com.andrew.apolloMod.Constants.*;



import java.util.*;

public class ImageProvider implements GetBitmapTask.OnBitmapReadyListener{

    private ImageCache memCache;

    private Map<String, Set<ImageView>> pendingImagesMap = new HashMap<String, Set<ImageView>>();

    private Set<String> unavailable = new HashSet<String>();
    
    private Context mContext;
    
    private int thumbSize;
    
    private static  ImageProvider mInstance;
    
    protected ImageProvider( Activity activity ) {
    	mContext = activity;
    	memCache = ImageCache.getInstance( activity );
    	Resources resources = mContext.getResources();
    	DisplayMetrics metrics = resources.getDisplayMetrics();
    	thumbSize = (int) ( ( 153 * (metrics.densityDpi/160f) ) + 0.5f );
    }
    
    public final static ImageProvider getInstance( final Activity activity ) {    	
        if (mInstance == null) {
            mInstance = new ImageProvider( activity );
            mInstance.setImageCache(ImageCache.findOrCreateCache(activity));
        }
        return mInstance;        
    }
    
    public void setImageCache(final ImageCache cacheCallback) {
    	memCache = cacheCallback;
    }
    
    public void loadImage( ImageView imageView, ImageInfo imageInfo ){
    	String tag = ImageUtils.createShortTag(imageInfo) + imageInfo.size;
    	if( imageInfo.source.equals(SRC_FILE) || imageInfo.source.equals(SRC_LASTFM) || imageInfo.source.equals(SRC_GALLERY)){
    		clearFromMemoryCache( ImageUtils.createShortTag(imageInfo) );
    		asyncLoad( tag, imageView, new GetBitmapTask( thumbSize, imageInfo, this, imageView.getContext() ) );
		}
    	if(!setCachedBitmap(imageView, tag)){
            asyncLoad( tag, imageView, new GetBitmapTask( thumbSize, imageInfo, this, imageView.getContext() ) );
        }
    }

    private boolean setCachedBitmap(ImageView imageView, String tag) {
        if (unavailable.contains(tag)) {
            handleBitmapUnavailable(imageView, tag);
            return true;
        }
        Bitmap bitmap = memCache.get(tag);
        if (bitmap == null)
            return false;
        imageView.setTag(tag);
        imageView.setImageBitmap(bitmap);
        return true;
    }

    private void handleBitmapUnavailable(ImageView imageView, String tag) {
        imageView.setTag(tag);
        imageView.setImageDrawable(null);
    }

    private void setLoadedBitmap(ImageView imageView, Bitmap bitmap, String tag) {
        if (!tag.equals(imageView.getTag()))
            return;

        final TransitionDrawable transition = new TransitionDrawable(new Drawable[]{
                new ColorDrawable(android.R.color.transparent),
                new BitmapDrawable(imageView.getResources(), bitmap)
        });

        imageView.setImageDrawable(transition);
        final int duration = imageView.getResources().getInteger(R.integer.image_fade_in_duration);
        transition.startTransition(duration);
    }

    private void asyncLoad(String tag, ImageView imageView, AsyncTask<String, Integer, Bitmap> task) {
        Set<ImageView> pendingImages = pendingImagesMap.get(tag);
        if (pendingImages == null) {
            pendingImages = Collections.newSetFromMap(new WeakHashMap<ImageView, Boolean>()); // create weak set
            pendingImagesMap.put(tag, pendingImages);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        pendingImages.add(imageView);
        imageView.setTag(tag);
        imageView.setImageDrawable(null);
    }

    @Override
    public void bitmapReady(Bitmap bitmap, String tag) {
        if (bitmap == null) {
            unavailable.add(tag);
        }
        else
        {
            memCache.add(tag, bitmap);
        }
        Set<ImageView> pendingImages = pendingImagesMap.get(tag);
        if (pendingImages != null) {
            pendingImagesMap.remove(tag);
            for (ImageView imageView : pendingImages) {
                setLoadedBitmap(imageView, bitmap, tag);
            }
        }
    }
    
    public void clearFromMemoryCache(String tag){
        if (unavailable.contains(tag + SIZE_THUMB)) {
        	unavailable.remove(tag + SIZE_THUMB);
        }
        if (pendingImagesMap.get(tag + SIZE_THUMB)!=null){
        	pendingImagesMap.remove(tag + SIZE_THUMB);
        }
        if (memCache.get(tag + SIZE_THUMB)!=null){
        	memCache.remove(tag + SIZE_THUMB);
        }
        if (unavailable.contains(tag + SIZE_NORMAL)) {
        	unavailable.remove(tag + SIZE_NORMAL);
        }
        if (pendingImagesMap.get(tag + SIZE_NORMAL)!=null){
        	pendingImagesMap.remove(tag + SIZE_NORMAL);
        }
        if (memCache.get(tag + SIZE_NORMAL)!=null){
        	memCache.remove(tag + SIZE_NORMAL);
        }
    }
    
    public void clearAllCaches(){
    	try{
    		ImageUtils.deleteDiskCache(mContext);
    		memCache.clearMemCache();
    	}
    	catch(Exception e){}
    }
}
