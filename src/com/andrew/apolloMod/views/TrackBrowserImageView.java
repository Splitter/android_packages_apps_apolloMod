package com.andrew.apolloMod.views;


import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.widget.ImageView;

/***
 * 
 * Scale and crop src image from top left corner
 * Based on code from SO user Matt
 * http://stackoverflow.com/questions/6330084/imageview-scaling-top-crop
 */

public class TrackBrowserImageView extends ImageView {

    public TrackBrowserImageView(Context context) {
        super(context);
        setup();
    }

    public TrackBrowserImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public TrackBrowserImageView(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        setup();
    }

    private void setup() {
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b)
    {
    	if(getDrawable()!=null){
    		float width = r - l; float height = b - t;
            Matrix matrix = getImageMatrix(); 
            float scaleFactor, scaleFactorWidth, scaleFactorHeight;
            scaleFactorWidth = (float)width/(float)getDrawable().getIntrinsicWidth();
            scaleFactorHeight = (float)height/(float)getDrawable().getIntrinsicHeight();    

            if(scaleFactorHeight > scaleFactorWidth) {
                scaleFactor = scaleFactorHeight;
            } else {
                scaleFactor = scaleFactorWidth;
            }

            matrix.setScale(scaleFactor, scaleFactor, 0, 0);
            setImageMatrix(matrix);    	
        }    	

        return super.setFrame(l, t, r, b);
    }

}