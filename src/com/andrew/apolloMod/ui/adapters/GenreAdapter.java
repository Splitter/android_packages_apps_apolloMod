
package com.andrew.apolloMod.ui.adapters;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

import com.andrew.apolloMod.R;
import com.andrew.apolloMod.helpers.utils.MusicUtils;
import com.andrew.apolloMod.ui.fragments.list.GenresFragment;
import com.andrew.apolloMod.views.ViewHolderList;

/**
 * @author Andrew Neal
 */
public class GenreAdapter extends SimpleCursorAdapter {

    private WeakReference<ViewHolderList> holderReference;

    private final int left;
    
    private Context mContext;

    public GenreAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        // Helps center the text in the Genres tab
        mContext = context;
        left = mContext.getResources().getDimensionPixelSize(
                R.dimen.listview_items_padding_left_top);
    }

    /**
     * Used to quickly our the ContextMenu
     */
    private final View.OnClickListener showContextMenu = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            v.showContextMenu();
        }
    };

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);

        Cursor mCursor = (Cursor) getItem(position);
        // ViewHolderList
        final ViewHolderList viewholder;

        if (view != null) {

            viewholder = new ViewHolderList(view);
            holderReference = new WeakReference<ViewHolderList>(viewholder);
            view.setTag(holderReference.get());

        } else {
            viewholder = (ViewHolderList)convertView.getTag();
        }

        // Genre name
        String genreName = mCursor.getString(GenresFragment.mGenreNameIndex);
        holderReference.get().mViewHolderLineOne.setText(MusicUtils.parseGenreName(mContext,
                genreName));

        holderReference.get().mViewHolderLineOne.setPadding(left, 40, 0, 0);

        holderReference.get().mViewHolderImage.setVisibility(View.GONE);
        holderReference.get().mViewHolderLineTwo.setVisibility(View.GONE);

        holderReference.get().mQuickContext.setOnClickListener(showContextMenu);
        return view;
    }
}
