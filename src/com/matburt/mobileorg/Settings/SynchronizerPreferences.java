package com.matburt.mobileorg.Settings;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SynchronizerPreferences extends Preference {
    
    public static HashMap<String,Intent> syncIntents = new HashMap<String,Intent>();
    public SynchronizerPreferences(Context context) {
        super(context);
    }
    
    public SynchronizerPreferences(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public SynchronizerPreferences(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    protected View onCreateView(ViewGroup parent){
        
        LinearLayout layout = new LinearLayout(getContext());
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
                              LinearLayout.LayoutParams.WRAP_CONTENT,
                              LinearLayout.LayoutParams.WRAP_CONTENT);
        params1.weight  = 1.0f;
        layout.setPadding(20, 10, 10, 10);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        TextView view = new TextView(getContext());
        view.setText("Configure Synchronizer Settings");
        view.setLayoutParams(params1);
        view.setTextAppearance(getContext(), android.R.style.TextAppearance_Large);
        this.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    SharedPreferences appSettings = PreferenceManager.getDefaultSharedPreferences(getContext());
                    String synchroMode = appSettings.getString("syncSource","");
                    if(syncIntents.containsKey(synchroMode))
                    {
                    
                        getContext().startActivity(syncIntents.get(synchroMode));
                    }
                    else {
                        //throw new ReportableError(R.string.error_synchronizer_type_unknown,
                        //                          synchroMode);
                    }
                    return true;
                }
            });

        layout.addView(view);
        layout.setId(android.R.id.widget_frame);
        return layout; 
    }    
}
