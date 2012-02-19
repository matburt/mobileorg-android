package com.matburt.mobileorg.Plugin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.matburt.mobileorg.R;

public class SyncEditActivity extends Activity {

    private Button accept_button;
    private Button deny_button;

    private boolean canceled = true;
//    private static final String HELP_URL = "http://github.com/mobileorg/wiki";


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pluginsyncedit);

        this.accept_button = (Button) this.findViewById(R.id.plugin_sync_accept);
        this.accept_button.setOnClickListener(acceptListener);
        this.deny_button = (Button) this.findViewById(R.id.plugin_sync_deny);
        this.deny_button.setOnClickListener(denyListener);

        /*
         * A hack to prevent a private serializable classloader attack
         */
        BundleScrubber.scrub(getIntent());
        BundleScrubber.scrub(getIntent().getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE));

    }

    @Override
    public void finish()
    {
        if (this.canceled) {
            setResult(RESULT_CANCELED);
        }
        else {
            String message = getString(R.string.sync_plugin_message);
            final Intent resultIntent = new Intent();
            final Bundle resultBundle = new Bundle();
            resultBundle.putInt(PluginBundleManager.BUNDLE_EXTRA_INT_VERSION_CODE, 72);
            resultBundle.putString(PluginBundleManager.BUNDLE_EXTRA_STRING_MESSAGE, message);

            resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);

            /*
             * This is the blurb concisely describing what your setting's state is. This is simply used for display in the UI.
             */
            if (message.length() > getResources().getInteger(com.twofortyfouram.locale.platform.R.integer.twofortyfouram_locale_maximum_blurb_length))
                {
                    resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, message.substring(0, getResources().getInteger(com.twofortyfouram.locale.platform.R.integer.twofortyfouram_locale_maximum_blurb_length)));
                }
            else
                {
                    resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, message);
                }

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