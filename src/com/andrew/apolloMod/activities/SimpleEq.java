/**
 * 
 */

package com.andrew.apolloMod.activities;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.ApolloUtils;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.preferences.SharedPreferencesCompat;
import com.andrew.apolloMod.views.VerticalSeekBar;

import static com.andrew.apolloMod.Constants.*;

/**
 * @author Andrew Neal
 * @Note This displays specific track or album listings
 */
public class SimpleEq extends FragmentActivity
			implements 	SeekBar.OnSeekBarChangeListener,
		      			CompoundButton.OnCheckedChangeListener {
	
	SeekBar bBoost;
	CheckBox bBoostEnable;

	CheckBox eQEnable;
	VerticalSeekBar SeekBars[] = new VerticalSeekBar[6];
	
    TextView SeekBarLabels[] = new TextView[6];
    
    SharedPreferences mPreferences;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        // Landscape mode on phone isn't ready
        if (!ApolloUtils.isTablet(this))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        ApolloUtils.showUpTitleOnly(getActionBar());

        // Layout
        setContentView(R.layout.simple_eq);
        bBoost =  (SeekBar)findViewById(R.id.simple_eq_bassboost);
        bBoost.setOnSeekBarChangeListener(this);
        bBoostEnable = (CheckBox)findViewById(R.id.simple_eq_bass);
        bBoostEnable.setOnCheckedChangeListener(this);

        eQEnable = (CheckBox)findViewById(R.id.simple_eq_equalizer);
        eQEnable.setOnCheckedChangeListener(this);
        SeekBars[0]  = (VerticalSeekBar)findViewById(R.id.simple_eq_band0_seek);
        SeekBars[0].setOnSeekBarChangeListener(this);
        SeekBarLabels[0] = (TextView)findViewById(R.id.simple_eq_band0);
        SeekBars[1]  = (VerticalSeekBar)findViewById(R.id.simple_eq_band1_seek);
        SeekBars[1].setOnSeekBarChangeListener(this);
        SeekBarLabels[1] = (TextView)findViewById(R.id.simple_eq_band1);
        SeekBars[2]  = (VerticalSeekBar)findViewById(R.id.simple_eq_band2_seek);
        SeekBars[2].setOnSeekBarChangeListener(this);
        SeekBarLabels[2] = (TextView)findViewById(R.id.simple_eq_band2);
        SeekBars[3]  = (VerticalSeekBar)findViewById(R.id.simple_eq_band3_seek);
        SeekBars[3].setOnSeekBarChangeListener(this);
        SeekBarLabels[3] = (TextView)findViewById(R.id.simple_eq_band3);
        SeekBars[4]  = (VerticalSeekBar)findViewById(R.id.simple_eq_band4_seek);
        SeekBars[4].setOnSeekBarChangeListener(this);
        SeekBarLabels[4] = (TextView)findViewById(R.id.simple_eq_band4);
        SeekBars[5]  = (VerticalSeekBar)findViewById(R.id.simple_eq_band5_seek);
        SeekBars[5].setOnSeekBarChangeListener(this);
        SeekBarLabels[5] = (TextView)findViewById(R.id.simple_eq_band5);
        
        mPreferences = getSharedPreferences(APOLLO_PREFERENCES, MODE_WORLD_READABLE
                | MODE_WORLD_WRITEABLE);
        
        initEqualizerValues();
        
    }
    
    public void initEqualizerValues(){
    	bBoost.setProgress(mPreferences.getInt("simple_eq_bboost",0));
    	bBoostEnable.setChecked(mPreferences.getBoolean("simple_eq_boost_enable", false));
    	eQEnable.setChecked(mPreferences.getBoolean("simple_eq_equalizer_enable", false));
        int[] freqs = MusicUtils.getEqualizerFrequencies();
		for( int i = 0; i <= 5 ; i++ ){
			SeekBars[i].setProgress(mPreferences.getInt("simple_eq_seekbars"+String.valueOf(i),100));
			String freq = "";
			if( i <= ( freqs.length-1 ) ){
			    if (freqs[i] < 1000000)
			    	freq= "" + (freqs[i] / 1000) + "Hz";
			    else
			    	freq= "" + (freqs[i] / 1000000) + "kHz";	
			}
			else{
				freq = "0Hz";
			}
		    SeekBarLabels[i].setText(freq);		  
		}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {		
        Editor ed = mPreferences.edit();
		if (seekBar == bBoost)
        {
            ed.putInt("simple_eq_bboost", progress);
        }
		else{
			for( int i = 0; i <= 5 ; i++ ){
				if(SeekBars[i]==seekBar){
		            ed.putInt("simple_eq_seekbars"+String.valueOf(i), progress);				
				}
			}
		}
        SharedPreferencesCompat.apply(ed);
        MusicUtils.updateEqualizerSettings(getApplicationContext());
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCheckedChanged(CompoundButton bView, boolean isChecked) {
        Editor ed = mPreferences.edit();
		if(bView ==  bBoostEnable){
            ed.putBoolean("simple_eq_boost_enable", isChecked);		
		}
		else if(bView == eQEnable){
            ed.putBoolean("simple_eq_equalizer_enable", isChecked);
		}
        SharedPreferencesCompat.apply(ed);
		MusicUtils.updateEqualizerSettings(getApplicationContext());
	}
}
