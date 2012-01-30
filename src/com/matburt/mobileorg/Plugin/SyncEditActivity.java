package com.matburt.mobileorg.Plugin;

import android.app.Activity;
import android.widget.Button;
import android.os.Bundle;
import android.view.View;
import android.content.Intent;

import com.matburt.mobileorg.R;

public class SyncEditActivity extends Activity {

    private Button accept_button;
    private Button deny_button;

    private boolean canceled = true;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pluginsyncedit);

        this.accept_button = (Button) this.findViewById(R.id.plugin_sync_accept);
        this.accept_button.setOnClickListener(acceptListener);
        this.deny_button = (Button) this.findViewById(R.id.plugin_sync_deny);
        this.deny_button.setOnClickListener(denyListener);
    }

    @Override
    public void finish()
    {
        if (this.canceled) {
            setResult(RESULT_CANCELED);
        }
        else {
            final Intent resultIntent = new Intent();
            setResult(RESULT_OK, resultIntent); 
        } 
        super.finish();
    }

    private View.OnClickListener acceptListener = new View.OnClickListener() {
            public void onClick(View v) {
                canceled = false;
                finish();
            }
    };


    private View.OnClickListener denyListener = new View.OnClickListener() {
            public void onClick(View v) {
                canceled = true;
                finish();
            }
    };
}