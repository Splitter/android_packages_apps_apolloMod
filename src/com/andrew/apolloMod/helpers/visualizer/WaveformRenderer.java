/**
 * Copyright 2011, Felix Palmer
 *
 * Licensed under the MIT license:
 * http://creativecommons.org/licenses/MIT/
 */
package com.andrew.apolloMod.helpers.visualizer;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.MusicUtils;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class WaveformRenderer extends Renderer
{
  private float amplitude = 0;
  private Context mContext = null;
  private Paint mBrightPaint;
  private Paint paintBrightBlur;
  private Paint mPaint;
  private Paint paintBlur;

  /**
   * Renders the audio data onto a line. The line flashes on prominent beats
   * @param canvas
   * @param paint - Paint to draw lines with
   * @param paint - Paint to draw flash with
   * @param cycleColor - If true the color will change on each frame
   */
  public WaveformRenderer( Context context )
  {
    super();
    mContext = context;

    mBrightPaint = new Paint();
    mBrightPaint.setStrokeWidth(6f);
    mBrightPaint.setAntiAlias(true);
    mBrightPaint.setColor(Color.argb(188, 255, 255, 255));

    paintBrightBlur = new Paint();
    paintBrightBlur.set(mBrightPaint);
    paintBrightBlur.setColor(Color.argb(165, 0, 0, 0));
    paintBrightBlur.setStrokeWidth(6f);
    paintBrightBlur.setMaskFilter(new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL));
    
    mPaint = new Paint();
    mPaint.setStrokeWidth(2f);
    mPaint.setAntiAlias(true);
    mPaint.setColor(mContext.getResources().getColor(R.color.holo_blue_dark));
    
	paintBlur = new Paint();
	paintBlur.set(mPaint);
	paintBlur.setColor(Color.argb(165, 0, 0, 0));
	paintBlur.setStrokeWidth(2f);
	paintBlur.setMaskFilter(new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL));

  }

  @Override
  public void onRender(Canvas canvas, AudioData data, Rect rect)
  {
	  
    // Calculate points for line
    for (int i = 0; i < data.bytes.length - 1; i++) {
      mPoints[i * 4] = rect.width() * i / (data.bytes.length - 1);
      mPoints[i * 4 + 1] =  rect.height() / 2
          + ((byte) (data.bytes[i] + 128)) * (rect.height() / 3) / 128;
      mPoints[i * 4 + 2] = rect.width() * (i + 1) / (data.bytes.length - 1);
      mPoints[i * 4 + 3] = rect.height() / 2
          + ((byte) (data.bytes[i + 1] + 128)) * (rect.height() / 3) / 128;
    }

    // Calc amplitude for this waveform
    float accumulator = 0;
    for (int i = 0; i < data.bytes.length - 1; i++) {
      accumulator += Math.abs(data.bytes[i]);
    }
    if(MusicUtils.isPlaying()){
	    float amp = accumulator/(128 * data.bytes.length);
	    if(amp > amplitude)
	    {
	      // Amplitude is bigger than normal, make a prominent line
	    	amplitude = amp;    
	    	
	    	canvas.drawLines(mPoints, paintBrightBlur);
	    	canvas.drawLines(mPoints, mBrightPaint);
	    }
	    else
	    {
	      // Amplitude is nothing special, reduce the amplitude
	    	amplitude *= 0.99;

	    	canvas.drawLines(mPoints, paintBlur);
	    	canvas.drawLines(mPoints, mPaint);
	    }
    }
  }

  @Override
  public void onRender(Canvas canvas, FFTData data, Rect rect)
  {
    // Do nothing, we only display audio data
  }

}
