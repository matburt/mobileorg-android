package com.matburt.mobileorg;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * An activity representing a single OrgNode detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link OrgNodeListActivity}.
 */
public class EditNodeActivity extends AppCompatActivity {
    EditNodeFragment fragment;
    public static String EDIT_NODE_FRAGMENT = "edit_node_fragment_tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_node);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_1);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");


        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.

            fragment = new EditNodeFragment();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.edit_node_container, fragment, EDIT_NODE_FRAGMENT)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_node_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, new Intent(this, OrgNodeListActivity.class));
            return true;

            case R.id.edit_menu_ok:

                boolean shouldFinish = ((EditNodeFragment)(getSupportFragmentManager().findFragmentByTag(EDIT_NODE_FRAGMENT))).onOKPressed();
                if(shouldFinish) finish();
//                Intent intent = new Intent(this, OrgNodeListActivity.class);
//                startActivity(intent);
                return true;
            case R.id.edit_menu_cancel:
                ((EditNodeFragment)(getSupportFragmentManager().findFragmentByTag(EDIT_NODE_FRAGMENT))).onCancelPressed();
                finish();
//                intent = new Intent(this, OrgNodeListActivity.class);
//                startActivity(intent);
                return true;
        }
//        return super.onOptionsItemSelected(item);
        return false;
    }
}
