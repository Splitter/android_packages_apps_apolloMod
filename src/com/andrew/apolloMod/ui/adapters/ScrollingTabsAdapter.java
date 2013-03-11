
package com.andrew.apolloMod.ui.adapters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.ThemeUtils;

import static com.andrew.apolloMod.Constants.TABS_ENABLED;

public class ScrollingTabsAdapter implements TabAdapter {

    private final Activity activity;

    public ScrollingTabsAdapter(Activity act) {
        activity = act;
    }

    @Override
    public View getView(int position) {
        LayoutInflater inflater = activity.getLayoutInflater();
        final Button tab = (Button)inflater.inflate(R.layout.tabs, null);

        //Get default values for tab visibility preferences
        final String[] mTitles = activity.getResources().getStringArray(R.array.tab_titles);

        //Get tab visibility preferences
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        Set<String> defaults = new HashSet<String>(Arrays.asList(mTitles));
        Set<String> tabs_set = sp.getStringSet(TABS_ENABLED,defaults);
        //if its empty fill reset it to full defaults
    		//stops app from crashing when no tabs are shown
    		//TODO:rewrite activity to not crash when no tabs are chosen to show
        		//or display error when no option is chosen
        if(tabs_set.size()==0)
        	tabs_set = defaults;
        
        //MultiSelectListPreference fails to preserve order of options chosen
        //Re-order based on order of default options array
        //This ensures titles are attached to correct tabs/pages
        String[] tabs_new = new String[tabs_set.size()];
        int cnt = 0;
        for(int i = 0 ; i< mTitles.length ; i++){
        	if(tabs_set.contains(mTitles[i])){
        		tabs_new[cnt]=mTitles[i];
        		cnt++;
        	}        	
        }        
        //Set the tab text
        if (position < tabs_new.length)
            tab.setText(tabs_new[position].toUpperCase());

        // Theme chooser
        ThemeUtils.setTextColor(activity, tab, "tab_text_color");
        return tab;
    }
}
