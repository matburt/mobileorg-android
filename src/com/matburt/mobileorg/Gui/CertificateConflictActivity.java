package com.matburt.mobileorg.Gui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.os.Build;
import android.os.Bundle;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.matburt.mobileorg.R;

public class CertificateConflictActivity extends Activity {

    private TextView hash_details;
    private TextView cert_descr;
    private Button accept_button;
    private Button deny_button;

    @SuppressLint("NewApi")
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.certconflict);

        this.hash_details = (TextView) this.findViewById(R.id.cert_hash_details);
        this.cert_descr = (TextView) this.findViewById(R.id.cert_new_descr);
        this.accept_button = (Button) this.findViewById(R.id.cert_conflict_accept);
        this.accept_button.setOnClickListener(acceptListener);
        this.deny_button = (Button) this.findViewById(R.id.cert_conflict_deny);
        this.deny_button.setOnClickListener(denyListener);

        SharedPreferences appSettings = 
            PreferenceManager.getDefaultSharedPreferences(this);
        String webCertHash = Integer.toString(appSettings.getInt("webCertHash", 0));
        String conflictHash = Integer.toString(appSettings.getInt("webConflictHash", 0));
        String conflictDetails = appSettings.getString("webConflictHashDesc", "");
        this.hash_details.setText("Previous Hash: " + webCertHash + " does not match the current one: " + conflictHash);
        this.cert_descr.setText("The New Certificate Looks like this:\n" + conflictDetails);
    	// Disable transitions if configured
		if (Build.VERSION.SDK_INT >= 5 && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("viewAnimateTransitions", true)) {
			overridePendingTransition(0, 0);
		}
	}

    private void accept() {
        SharedPreferences appSettings = 
            PreferenceManager.getDefaultSharedPreferences(this);
        Editor edit = appSettings.edit();
        edit.putInt("webCertHash", appSettings.getInt("webConflictHash", 0));
        edit.putString("webCertDesscr", appSettings.getString("webConflictHashDesc", ""));
        edit.commit();
    }

    private View.OnClickListener acceptListener = new View.OnClickListener() {
            public void onClick(View v) {
                accept();
                finish();
            }
    };


    private View.OnClickListener denyListener = new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
    };
}