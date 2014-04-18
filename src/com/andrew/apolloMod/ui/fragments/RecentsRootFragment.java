
package com.andrew.apolloMod.ui.fragments;

import static com.andrew.apolloMod.Constants.TABS_ENABLED;
import static com.andrew.apolloMod.Constants.VISUALIZATION_TYPE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.ui.fragments.list.RecentlyAddedFragment;
import com.andrew.apolloMod.ui.fragments.list.RecentlyPlayedFragment;

import static com.andrew.apolloMod.Constants.RECENTS_TYPE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class RecentsRootFragment extends Fragment {


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.recents_root_fragment, container, false);

		FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String type = sp.getString(RECENTS_TYPE, getResources().getString(R.string.recents_added));                    
 	    if( type.equals(getResources().getString(R.string.recents_added)) ){
 	    	Log.e("Recents","ADDED!!!!!");
 	    	transaction.replace(R.id.root_frame, new RecentlyAddedFragment());
 	    }
 	    else if( type.equals(getResources().getString(R.string.recents_played)) ){
 	    	Log.e("Recents","PLAYED!!!!!");
 	    	transaction.replace(R.id.root_frame, new RecentlyPlayedFragment());
 	    }
 	    else{
 	    	Log.e("Recents","SPLITTERRR!!!!!");
 	    	transaction.replace(R.id.root_frame, new RecentsSplitFragment());
 	    }     	

		//transaction.replace(R.id.root_frame, new RecentsSplitFragment());

		transaction.commit();

		return view;
	}

}
