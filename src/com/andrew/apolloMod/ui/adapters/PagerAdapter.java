/**
 * 
 */

package com.andrew.apolloMod.ui.adapters;

import static com.andrew.apolloMod.Constants.TABS_ENABLED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.ui.activities.MusicLibrary;
import com.andrew.apolloMod.ui.fragments.RecentsRootFragment;
import com.andrew.apolloMod.ui.fragments.base.RefreshableFragment;

/**
 * @author Andrew Neal
 */
public class PagerAdapter extends FragmentPagerAdapter {

    private final ArrayList<Fragment> mFragments = new ArrayList<Fragment>();
    private Context mContext;

    public PagerAdapter(Context context, FragmentManager fragmentManager) {
        super(fragmentManager);
    	mContext = context;
    }

    public void addFragment(Fragment fragment) {
        mFragments.add(fragment);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }

    @Override
    public Fragment getItem(int position) {	
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        Set<String> defaults = new HashSet<String>(Arrays.asList(
        		mContext.getResources().getStringArray(R.array.tab_titles)
        	));
        Set<String> tabs_set = sp.getStringSet(TABS_ENABLED,defaults);

    	if (position == 0 
    			&& tabs_set.contains(mContext.getResources().getString(R.string.tab_recent)) 
    			&& MusicLibrary.class.isInstance(mContext) ){
    		return new RecentsRootFragment();
    	}
        return mFragments.get(position);
    }

    /**
     * This method update the fragments that extends the {@link RefreshableFragment} class
     */
    public void refresh() {
        for (int i = 0; i < mFragments.size(); i++) {
            if( mFragments.get(i) instanceof RefreshableFragment ) {
                ((RefreshableFragment)mFragments.get(i)).refresh();
            }
        }
    }

}
