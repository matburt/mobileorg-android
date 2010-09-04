package com.matburt.mobileorg;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;


public class SynchronizerPreference extends Preference {
    
    
    public SynchronizerPreference(Context context) {
        super(context);
    }
    
    public SynchronizerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public SynchronizerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
        protected View onCreateView(ViewGroup parent){
        
        LinearLayout layout = new LinearLayout(getContext());
        LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(
                              LinearLayout.LayoutParams.WRAP_CONTENT,
                              LinearLayout.LayoutParams.WRAP_CONTENT);
        params1.gravity = Gravity.LEFT;
        params1.weight  = 1.0f;
        LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
                            80,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
        params2.gravity = Gravity.RIGHT;
        LinearLayout.LayoutParams params3 = new LinearLayout.LayoutParams(
                            30,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
        params3.gravity = Gravity.CENTER;
        layout.setPadding(15, 5, 10, 5);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        TextView view = new TextView(getContext());
        view.setText("Configure Synchronizer Settings...");
        view.setTextSize(18);
        view.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        view.setGravity(Gravity.LEFT);
        view.setLayoutParams(params1);
        
        layout.addView(view);
        layout.setId(android.R.id.widget_frame);
        return layout; 
    }    
}
