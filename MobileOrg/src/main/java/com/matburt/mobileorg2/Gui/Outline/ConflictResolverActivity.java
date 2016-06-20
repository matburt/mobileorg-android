package com.matburt.mobileorg2.Gui.Outline;

import android.content.Intent;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import com.matburt.mobileorg2.OrgData.OrgContract;
import com.matburt.mobileorg2.OrgData.OrgFile;
import com.matburt.mobileorg2.OrgData.OrgProviderUtils;
import com.matburt.mobileorg2.OrgNodeListActivity;
import com.matburt.mobileorg2.R;
import com.matburt.mobileorg2.Synchronizers.JGitWrapper;
import com.matburt.mobileorg2.Synchronizers.SynchronizerManager;
import com.matburt.mobileorg2.util.OrgFileNotFoundException;
import com.matburt.mobileorg2.util.OrgUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ConflictResolverActivity extends AppCompatActivity {

    EditText editText;
    String filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conflict_resolver);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();



        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            Long nodeId = getIntent().getLongExtra(OrgContract.NODE_ID, -1);

            editText = (EditText)findViewById(R.id.conflict_resolver_text);
            try {
                OrgFile file = new OrgFile(nodeId, getContentResolver());
                if (actionBar != null) {
                    actionBar.setTitle(file.name);
                }

                String dir = SynchronizerManager.getInstance(null,null,null).getSyncher().getAbsoluteFilesDir(this);
                this.filename = dir+"/"+file.filename;
                editText.setText(OrgUtils.readAll(this.filename));

            } catch (OrgFileNotFoundException e) {
                e.printStackTrace();
            }
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
            case R.id.edit_menu_cancel:
                NavUtils.navigateUpTo(this, new Intent(this, OrgNodeListActivity.class));
                return true;
            case R.id.edit_menu_ok:
                if(this.filename!=null && !this.filename.equals("")){
                    OrgUtils.writeToFile(this.filename, editText.getText().toString());
                    new JGitWrapper.MergeTask(this).execute();
                }
                NavUtils.navigateUpTo(this, new Intent(this, OrgNodeListActivity.class));
                return true;
        }
        return false;
    }


}
