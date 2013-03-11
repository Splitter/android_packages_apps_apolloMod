/**
 * Copyright 2011, Felix Palmer
 *
 * Licensed under the MIT license:
 * http://creativecommons.org/licenses/MIT/
 */
package com.andrew.apolloMod.helpers.visualizer;

import com.andrew.apolloMod.R;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;

public class SolidBarGraphRenderer extends Renderer
{
  private Context mContext = null;
  
  private int[] mData = null;

  public SolidBarGraphRenderer(Context context)
  {
    super();
    mContext = context;
    mData = new int[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0};
  }

  @Override
  public void onRender(Canvas canvas, AudioData data, Rect rect)
  {
    // Do nothing, we only display FFT data
  }

  /**
   * Renders a 14 line bar graph/ histogram of the FFT data
   */
  @Override
  public void onRender(Canvas canvas, FFTData data, Rect rect)
  {
	  //space between lines of graph  
	  float space = 4f;

	  Resources resources = mContext.getResources();
	  DisplayMetrics metrics = resources.getDisplayMetrics();
	  //margin from left/right edges
	  int margin = (int) ( ( 16 * (metrics.densityDpi/160f) ) + 0.5f );
  
	  //Calculate width of each bar
	  float bar_width = ( ( rect.width() - ((13 * space) + (margin * 2)) ) / 14 );
	  //calculate length between the start of each bar
	  float next_start = bar_width + space;

	  //Stroke of each bar
	  Paint paint = new Paint();
	  paint.setStrokeWidth(bar_width);
	  paint.setAntiAlias(true);
	  paint.setColor(mContext.getResources().getColor(R.color.holo_blue_dark));
  
	  //Stroke for bars drop shadow
	  Paint paintBlur = new Paint();
	  paintBlur.set(paint);
	  paintBlur.setColor(Color.argb(165, 0, 0, 0));
	  paintBlur.setStrokeWidth(bar_width);
	  paintBlur.setMaskFilter(new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL));
	  
	  for (int i = 0; i < 14; i++) {
			//set x start/stop of bar
			mFFTPoints[i * 4] = margin + (i * next_start)+bar_width/2;
			mFFTPoints[i * 4 + 2] = margin + (i * next_start)+bar_width/2;
		
			//calculate height of bar based on sampling 4 data points
			byte rfk = data.bytes[ (10 * i)];
			byte ifk = data.bytes[ (10 * i + 1)];
			float magnitude = (rfk * rfk + ifk * ifk);
			int dbValue = (int) (10 * Math.log10(magnitude));
			rfk = data.bytes[ (10 * i + 2)];
			ifk = data.bytes[ (10 * i + 3)];
			magnitude = (rfk * rfk + ifk * ifk);
			dbValue = (int) ( (10 * Math.log10(magnitude)) + dbValue) / 2;
		
			//Average with previous bars value(reduce spikes / smoother transitions)
			dbValue =( mData[i] +  ((dbValue < 0) ? 0 : dbValue) ) / 2;
			mData[i] = dbValue;
		
			//only jump height on multiples of 5
			if(dbValue >= 5)
				dbValue = (int) Math.floor(dbValue/5) * 5;
		
			//set y start/stop of bar
			mFFTPoints[i * 4 + 1] = rect.height();
			mFFTPoints[i * 4 + 3] = rect.height() - (dbValue * 8);
	  }
	  //draw drop shadow lines first
	  canvas.drawLines(mFFTPoints, paintBlur);
	  //draw solid bar graph on top
	  canvas.drawLines(mFFTPoints, paint);
  }
}
