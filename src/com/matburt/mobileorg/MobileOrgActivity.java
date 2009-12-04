package com.matburt.mobileorg;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MobileOrgActivity extends Activity
{
    private static final int OP_MENU_SETTINGS = 1;
    private static final int OP_MENU_SYNC = 2;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MobileOrgActivity.OP_MENU_SYNC, 0, "Sync");
        menu.add(0, MobileOrgActivity.OP_MENU_SETTINGS, 0, "Settings");
        return true;
    }

    /* Handles item selections */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MobileOrgActivity.OP_MENU_SYNC:
            return true;
        case MobileOrgActivity.OP_MENU_SETTINGS:
            return true;
        }
        return false;
    }

}
