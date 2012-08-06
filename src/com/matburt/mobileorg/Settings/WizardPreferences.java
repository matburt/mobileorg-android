package com.matburt.mobileorg.Settings;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WizardPreferences extends Preference {
    public WizardPreferences(Context context) {
        super(context);
    }
    
    public WizardPreferences(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public WizardPreferences(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    protected View onCreateView(ViewGroup parent){
        
        LinearLayout layout = new LinearLayout(getContext());
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
                              LinearLayout.LayoutParams.WRAP_CONTENT,
                              LinearLayout.LayoutParams.WRAP_CONTENT);
        //params1.gravity = Gravity.LEFT;
        params1.weight  = 1.0f;
        layout.setPadding(20, 10, 10, 10);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        TextView view = new TextView(getContext());
        view.setText("Re-run Setup Wizard");
        // view.setTextSize(18);
        // view.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        view.setTextAppearance(getContext(), android.R.style.TextAppearance_Large);
        //view.setGravity(Gravity.LEFT);
        view.setLayoutParams(params1);

        this.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    getContext().startActivity(new Intent(getContext(), WizardActivity.class));
                    return true;
                }
            });

        layout.addView(view);
        layout.setId(android.R.id.widget_frame);
        return layout; 
    }    
    
}