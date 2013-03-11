/**
 * 
 */

package com.andrew.apolloMod.helpers.utils;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import static com.andrew.apolloMod.Constants.APOLLO;
import static com.andrew.apolloMod.Constants.APOLLO_PREFERENCES;
import static com.andrew.apolloMod.Constants.THEME_ITEM_FOREGROUND;
import static com.andrew.apolloMod.Constants.THEME_PACKAGE_NAME;

/**
 * @author Andrew Neal TODO - clean this up
 */
public class ThemeUtils {

    /**
     * @param context
     * @param default_theme
     * @return theme package name
     */
    public static String getThemePackageName(Context context, String default_theme) {
        SharedPreferences sp = context.getSharedPreferences(APOLLO_PREFERENCES, 0);
        return sp.getString(THEME_PACKAGE_NAME, default_theme);
    }

    /**
     * @param context
     * @param packageName
     */
    public static void setThemePackageName(Context context, String packageName) {
        SharedPreferences sp = context.getSharedPreferences(APOLLO_PREFERENCES, 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(THEME_PACKAGE_NAME, packageName);
        editor.commit();
    }

    /**
     * @param themeResources
     * @param themePackage
     * @param item_name
     * @param item
     * @param themeType
     */
	@SuppressWarnings("deprecation")
    public static void loadThemeResource(Resources themeResources, String themePackage,
            String item_name, View item, int themeType) {
        Drawable d = null;
        if (themeResources != null) {
            int resource_id = themeResources.getIdentifier(item_name, "drawable", themePackage);
            if (resource_id != 0) {
                try {
                    d = themeResources.getDrawable(resource_id);
                } catch (Resources.NotFoundException e) {
                    return;
                }
                if (themeType == THEME_ITEM_FOREGROUND && item instanceof ImageView) {
                    Drawable tmp = ((ImageView)item).getDrawable();
                    if (tmp != null) {
                        tmp.setCallback(null);
                        tmp = null;
                    }
                    ((ImageView)item).setImageDrawable(d);
                } else {
                    Drawable tmp = item.getBackground();
                    if (tmp != null) {
                        tmp.setCallback(null);
                        tmp = null;
                    }
                    item.setBackgroundDrawable(d);
                }
            }
        }
    }

    /**
     * @param mContext
     * @param view
     * @param resourceName
     * @param themeType
     */
    public static void initThemeChooser(Context mContext, View view, String resourceName,
            int themeType) {
        String themePackage = getThemePackageName(mContext, APOLLO);
        PackageManager pm = mContext.getPackageManager();
        Resources themeResources = null;
        if (!themePackage.equals(APOLLO)) {
            try {
                themeResources = pm.getResourcesForApplication(themePackage);
            } catch (NameNotFoundException e) {
                setThemePackageName(mContext, APOLLO);
            }
        }

        if (themeResources != null)
            loadThemeResource(themeResources, themePackage, resourceName, view, themeType);
    }

    /**
     * @param mContext
     * @param view
     * @param resourceName
     */
    public static void setTextColor(Context mContext, TextView view, String resourceName) {
        String themePackage = getThemePackageName(mContext, APOLLO);
        PackageManager pm = mContext.getPackageManager();
        Resources themeResources = null;
        if (!themePackage.equals(APOLLO)) {
            try {
                themeResources = pm.getResourcesForApplication(themePackage);
            } catch (NameNotFoundException e) {
                setThemePackageName(mContext, APOLLO);
            }
        }
        if (themeResources != null) {
            int resourceID = themeResources.getIdentifier(resourceName, "color", themePackage);
            if (resourceID != 0) {
                view.setTextColor(themeResources.getColor(resourceID));
            }
        }
    }

    /**
     * @param mContext
     * @param view
     * @param resourceName
     */
    public static void setBackgroundColor(Context mContext, View view, String resourceName) {
        String themePackage = getThemePackageName(mContext, APOLLO);
        PackageManager pm = mContext.getPackageManager();
        Resources themeResources = null;
        if (!themePackage.equals(APOLLO)) {
            try {
                themeResources = pm.getResourcesForApplication(themePackage);
            } catch (NameNotFoundException e) {
                setThemePackageName(mContext, APOLLO);
            }
        }
        if (themeResources != null) {
            int resourceID = themeResources.getIdentifier(resourceName, "color", themePackage);
            if (resourceID != 0) {
                view.setBackgroundColor(themeResources.getColor(resourceID));
            }
        }
    }

    /**
     * @param mContext
     * @param view
     * @param resourceName
     */
    public static void setImageButton(Context mContext, ImageButton view, String resourceName) {
        String themePackage = getThemePackageName(mContext, APOLLO);
        PackageManager pm = mContext.getPackageManager();
        Resources themeResources = null;
        if (!themePackage.equals(APOLLO)) {
            try {
                themeResources = pm.getResourcesForApplication(themePackage);
            } catch (NameNotFoundException e) {
                setThemePackageName(mContext, APOLLO);
            }
        }
        if (themeResources != null) {
            int resourceID = themeResources.getIdentifier(resourceName, "drawable", themePackage);
            if (resourceID != 0) {
                view.setImageDrawable(themeResources.getDrawable(resourceID));
            }
        }
    }

    /**
     * @param mContext
     * @param view
     * @param resourceName
     */
    public static void setMarginDrawable(Context mContext, ViewPager view, String resourceName) {
        String themePackage = getThemePackageName(mContext, APOLLO);
        PackageManager pm = mContext.getPackageManager();
        Resources themeResources = null;
        if (!themePackage.equals(APOLLO)) {
            try {
                themeResources = pm.getResourcesForApplication(themePackage);
            } catch (NameNotFoundException e) {
                setThemePackageName(mContext, APOLLO);
            }
        }
        if (themeResources != null) {
            int resourceID = themeResources.getIdentifier(resourceName, "drawable", themePackage);
            if (resourceID != 0) {
                view.setPageMarginDrawable(themeResources.getDrawable(resourceID));
            }
        }
    }

    /**
     * @param mContext
     * @param view
     * @param resourceName
     */
    public static void setActionBarBackground(Context mContext, ActionBar view, String resourceName) {
        String themePackage = getThemePackageName(mContext, APOLLO);
        PackageManager pm = mContext.getPackageManager();
        Resources themeResources = null;
        if (!themePackage.equals(APOLLO)) {
            try {
                themeResources = pm.getResourcesForApplication(themePackage);
            } catch (NameNotFoundException e) {
                setThemePackageName(mContext, APOLLO);
            }
        }
        if (themeResources != null) {
            int resourceID = themeResources.getIdentifier(resourceName, "drawable", themePackage);
            if (resourceID != 0) {
                view.setBackgroundDrawable(themeResources.getDrawable(resourceID));
            }
        }
    }

    /**
     * @param mContext
     * @param view
     * @param resourceName
     */
    public static void setActionBarItem(Context mContext, MenuItem view, String resourceName) {
        String themePackage = getThemePackageName(mContext, APOLLO);
        PackageManager pm = mContext.getPackageManager();
        Resources themeResources = null;
        if (!themePackage.equals(APOLLO)) {
            try {
                themeResources = pm.getResourcesForApplication(themePackage);
            } catch (NameNotFoundException e) {
                setThemePackageName(mContext, APOLLO);
            }
        }
        if (themeResources != null) {
            int resourceID = themeResources.getIdentifier(resourceName, "drawable", themePackage);
            if (resourceID != 0) {
                view.setIcon(themeResources.getDrawable(resourceID));
            }
        }
    }

    /**
     * @param mContext
     * @param view
     * @param resourceName
     */
    public static void setProgessDrawable(Context mContext, SeekBar view, String resourceName) {
        String themePackage = getThemePackageName(mContext, APOLLO);
        PackageManager pm = mContext.getPackageManager();
        Resources themeResources = null;
        if (!themePackage.equals(APOLLO)) {
            try {
                themeResources = pm.getResourcesForApplication(themePackage);
            } catch (NameNotFoundException e) {
                setThemePackageName(mContext, APOLLO);
            }
        }
        if (themeResources != null) {
            int resourceID = themeResources.getIdentifier(resourceName, "drawable", themePackage);
            if (resourceID != 0) {
                view.setProgressDrawable(themeResources.getDrawable(resourceID));
            }
        }
    }

    /**
     * @param mContext
     * @return which overflow icon to use
     */
    public static boolean overflowLight(Context mContext) {
        String themePackage = getThemePackageName(mContext, APOLLO);
        PackageManager pm = mContext.getPackageManager();
        Resources themeResources = null;
        if (!themePackage.equals(APOLLO)) {
            try {
                themeResources = pm.getResourcesForApplication(themePackage);
            } catch (NameNotFoundException e) {
                setThemePackageName(mContext, APOLLO);
            }
        }
        if (themeResources != null) {
            int resourceID = themeResources.getIdentifier("overflow.light", "bool", themePackage);
            if (resourceID != 0) {
                Boolean overflow = themeResources.getBoolean(resourceID);
                if (overflow)
                    return true;
            }
        }
        return false;
    }
}
