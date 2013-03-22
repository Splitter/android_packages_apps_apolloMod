package com.andrew.apolloMod.helpers.utils;


import java.lang.ref.WeakReference;

import com.andrew.apolloMod.ui.widgets.VisualizerView;

import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;

public class VisualizerUtils {
		
	  private static Visualizer mVisualizer = null;
	  private static WeakReference<VisualizerView> mView = null;

	  public static void updateVisualizerView(WeakReference<VisualizerView> visualizerView){
		  mView = visualizerView;
	  }
	  
	  public static void updateVisualizerFFT(byte[] bytes) {
		  if(mView==null)
			  return;
		  VisualizerView view = mView.get();
	      if (view == null) {
	      	return;
	      }
		  view.updateVisualizerFFT(bytes);
	  }

	  public static void updateVisualizer(byte[] bytes) {
		  if(mView==null)
			  return;
		  VisualizerView view = mView.get();
	      if (view == null) {
	      	return;
	      }
		  view.updateVisualizer(bytes);
	  }
	  
	  public static void releaseVisualizer(){
		  if(mVisualizer != null)
			  mVisualizer.release();
	  }
	  
	  public static void initVisualizer( MediaPlayer player ){
		  VisualizerUtils.releaseVisualizer();
		  try{
			  mVisualizer =  new Visualizer(player.getAudioSessionId());
		  }
		  catch(Exception e){
			  mVisualizer = null;
			  return;
		  }

		  mVisualizer.setEnabled(false);		  

		  mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);

		  Visualizer.OnDataCaptureListener captureListener = new Visualizer.OnDataCaptureListener()
		  {
			  @Override
			  public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
			      int samplingRate)
			  {
				  VisualizerUtils.updateVisualizer(bytes);
			  }
			
			  @Override
			  public void onFftDataCapture(Visualizer visualizer, byte[] bytes,
			      int samplingRate)
			  {
				  VisualizerUtils.updateVisualizerFFT(bytes);
			  }
		  };
		  mVisualizer.setDataCaptureListener(captureListener,20000 , true, true);	

		  mVisualizer.setEnabled(true);		  
	  }
}
