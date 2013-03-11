
package com.andrew.apolloMod.preferences;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrew.apolloMod.R;

import static com.andrew.apolloMod.Constants.APOLLO;
import static com.andrew.apolloMod.Constants.THEME_DESCRIPTION;
import static com.andrew.apolloMod.Constants.THEME_PREVIEW;
import static com.andrew.apolloMod.Constants.THEME_TITLE;

public class ThemePreview extends Preference {
    private CharSequence themeName;

    private CharSequence themePackageName;

    private CharSequence themeDescription;

    private Drawable themePreview;

    public ThemePreview(Context context) {
        super(context);
    }

    public ThemePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThemePreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (themePackageName != null && themePackageName.toString().length() > 0) {
            TextView vThemeTitle = (TextView)view.findViewById(R.id.themeTitle);
            vThemeTitle.setText(themeName);
            TextView vThemeDescription = (TextView)view.findViewById(R.id.themeDescription);
            vThemeDescription.setMovementMethod(LinkMovementMethod.getInstance());
            vThemeDescription.setText(Html.fromHtml(themeDescription.toString()));
            ImageView vThemePreview = (ImageView)view.findViewById(R.id.themeIcon);
            if (themePreview != null)
                vThemePreview.setImageDrawable(themePreview);
            else
                vThemePreview.setImageResource(R.drawable.ic_launcher);
            vThemeTitle.setText(themeName);

            Button applyButton = (Button)view.findViewById(R.id.themeApply);
            applyButton.setEnabled(true);
        } else {
            Button applyButton = (Button)view.findViewById(R.id.themeApply);
            applyButton.setEnabled(false);
        }
    }

    /**
     * @param packageName
     */
    public void setTheme(CharSequence packageName) {
        themePackageName = packageName;
        themeName = null;
        themeDescription = null;
        if (themePreview != null)
            themePreview.setCallback(null);
        themePreview = null;
        if (!packageName.equals(APOLLO)) {
            Resources themeResources = null;
            try {
                themeResources = getContext().getPackageManager().getResourcesForApplication(
                        packageName.toString());
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            if (themeResources != null) {
                int themeNameId = themeResources.getIdentifier(THEME_TITLE, "string",
                        packageName.toString());
                if (themeNameId != 0) {
                    themeName = themeResources.getString(themeNameId);
                }
                int themeDescriptionId = themeResources.getIdentifier(THEME_DESCRIPTION, "string",
                        packageName.toString());
                if (themeDescriptionId != 0) {
                    themeDescription = themeResources.getString(themeDescriptionId);
                }
                int themePreviewId = themeResources.getIdentifier(THEME_PREVIEW, "drawable",
                        packageName.toString());
                if (themePreviewId != 0) {
                    themePreview = themeResources.getDrawable(themePreviewId);
                }
            }
        }
        if (themeName == null)
            themeName = getContext().getResources().getString(R.string.apollo_themes);
        if (themeDescription == null)
            themeDescription = getContext().getResources().getString(R.string.themes);
        notifyChanged();
    }

    public CharSequence getValue() {
        return themePackageName;
    }
}
