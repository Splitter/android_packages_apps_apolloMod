
package com.andrew.apolloMod.ui.adapters;

import java.util.ArrayList;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import com.andrew.apolloMod.helpers.RefreshableFragment;

public class AlbumArtPagerAdapter extends FragmentStatePagerAdapter {

    private final ArrayList<Fragment> mFragments = new ArrayList<Fragment>();

    public AlbumArtPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
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
        return mFragments.get(position);
    }

    public void removeItem(int position){
    	mFragments.remove(position);
        notifyDataSetChanged();
    }
    
    public void addFragmentTo(Fragment fragment, int position ){
    	mFragments.add(position, fragment);
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemPosition(Object object){
        return PagerAdapter.POSITION_NONE;
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
